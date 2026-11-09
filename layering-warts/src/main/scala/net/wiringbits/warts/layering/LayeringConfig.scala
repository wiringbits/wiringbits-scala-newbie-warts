package net.wiringbits.warts.layering

import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

/** A layer owns its packages and may only reference the layers listed in `calls`, itself included. */
final case class Layer(name: String, packages: List[String], calls: Set[String])

/** The layer model of a module.
  *
  * @param roots
  *   the packages the module owns, references to code outside every root are not checked
  * @param allowSameFileSiblings
  *   a class may reference another class of its own layer defined in the same source file
  */
final case class LayeringConfig(
    roots: List[String],
    layers: List[Layer],
    allowSameFileSiblings: Boolean
)

object LayeringConfig {
  val FileName: String = ".wartremover-layering.conf"

  val ExpectedFormat: String =
    """Expected HOCON format:
      |roots = ["com.example"]
      |layers = [
      |  { name = controllers, packages = ["com.example.controllers"], calls = [actions, shared] }
      |  { name = actions, packages = ["com.example.actions"], calls = [shared] }
      |  { name = shared, packages = ["com.example.models"], calls = [shared] }
      |]
      |allowSameFileSiblings = true""".stripMargin

  /** The config of the module owning the source file: the nearest [[FileName]] in its directory or a parent one. */
  def forSource(sourcePath: Path): Either[String, LayeringConfig] = {
    val dir = Option(sourcePath.toAbsolutePath.getParent)
    locate(sourcePath) match {
      case Some(path) => load(path)
      case None =>
        Left(
          s"The Layering wart is enabled but no $FileName was found in ${dir.getOrElse(sourcePath)} or its parents.\n$ExpectedFormat"
        )
    }
  }

  def locate(sourcePath: Path): Option[Path] =
    Iterator
      .iterate(sourcePath.toAbsolutePath.getParent)(_.getParent)
      .takeWhile(_ != null)
      .map(_.resolve(FileName))
      .find(Files.isRegularFile(_))

  def load(path: Path): Either[String, LayeringConfig] = {
    if (!Files.isRegularFile(path))
      Left(s"The Layering wart is enabled but its config file $path does not exist.\n$ExpectedFormat")
    else
      try {
        parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).left.map(e => s"$path: $e")
      } catch {
        case NonFatal(e) => Left(s"The Layering wart could not read its config file $path: ${e.getMessage}")
      }
  }

  def parse(text: String): Either[String, LayeringConfig] =
    try {
      validate(ConfigFactory.parseString(text).resolve()).left.map(e => s"Invalid Layering config: $e\n$ExpectedFormat")
    } catch {
      case e: ConfigException => Left(s"Invalid Layering config: ${e.getMessage}\n$ExpectedFormat")
    }

  private def validate(c: Config): Either[String, LayeringConfig] = {
    val roots = c.getStringList("roots").asScala.toList
    val layers = c.getConfigList("layers").asScala.toList.map { l =>
      Layer(
        name = l.getString("name"),
        packages = l.getStringList("packages").asScala.toList,
        calls = l.getStringList("calls").asScala.toSet
      )
    }
    val allowSameFileSiblings = !c.hasPath("allowSameFileSiblings") || c.getBoolean("allowSameFileSiblings")
    val names = layers.map(_.name)
    val unknownCalls = layers.flatMap(l => (l.calls -- names).map(callee => s"${l.name} -> $callee"))
    val outsideRoots = layers.flatMap(_.packages).filterNot(pkg => roots.exists(LayeringRules.inPackage(pkg, _)))

    if (roots.isEmpty) Left("`roots` is empty")
    else if (layers.isEmpty) Left("`layers` is empty")
    else if (names.distinct.size != names.size)
      Left(s"duplicated layer names: ${names.diff(names.distinct).mkString(", ")}")
    else if (unknownCalls.nonEmpty) Left(s"calls to unknown layers: ${unknownCalls.mkString(", ")}")
    else if (outsideRoots.nonEmpty) Left(s"layer packages outside every root: ${outsideRoots.mkString(", ")}")
    else Right(LayeringConfig(roots = roots, layers = layers, allowSameFileSiblings = allowSameFileSiblings))
  }
}
