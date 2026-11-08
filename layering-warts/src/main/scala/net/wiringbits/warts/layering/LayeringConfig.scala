package net.wiringbits.warts.layering

import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

/** A layer, its position in [[Block.layers]] is its rank: the first layer is the highest one. */
final case class Layer(name: String, packages: List[String])

/** The layer model applied to the source files whose path contains `matchPath`. */
final case class Block(
    matchPath: String,
    layers: List[Layer],
    forbidSiblingCalls: Set[String],
    leaves: List[String]
)

final case class LayeringConfig(modules: List[Block]) {

  /** The block whose `match` is the longest substring of the path, a path matching no block is not checked. */
  def selectBlock(sourcePath: String): Option[Block] = {
    val normalized = sourcePath.replace('\\', '/')
    modules.filter(block => normalized.contains(block.matchPath)).maxByOption(_.matchPath.length)
  }
}

object LayeringConfig {
  val FileName: String = ".wartremover-layering.conf"

  val ExpectedFormat: String =
    """Expected HOCON format:
      |modules = [
      |  {
      |    match = "/server/"
      |    layers = [
      |      { name = controllers, packages = ["controllers"] }
      |      { name = actions, packages = ["com.example.actions"] }
      |    ]
      |    forbidSiblingCalls = [controllers, actions]
      |    leaves = ["com.example.models"]
      |  }
      |]""".stripMargin

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
      val config = ConfigFactory.parseString(text).resolve()
      val blocks = config.getConfigList("modules").asScala.toList.map(parseBlock)
      blocks.collectFirst { case Left(e) => e } match {
        case Some(e) => Left(s"Invalid Layering config: $e\n$ExpectedFormat")
        case None => Right(LayeringConfig(blocks.collect { case Right(b) => b }))
      }
    } catch {
      case e: ConfigException => Left(s"Invalid Layering config: ${e.getMessage}\n$ExpectedFormat")
    }

  private def parseBlock(c: Config): Either[String, Block] = {
    def strings(path: String): List[String] =
      if (c.hasPath(path)) c.getStringList(path).asScala.toList else Nil

    val matchPath = c.getString("match")
    val layers = c.getConfigList("layers").asScala.toList.map { l =>
      Layer(name = l.getString("name"), packages = l.getStringList("packages").asScala.toList)
    }
    val forbidSiblingCalls = strings("forbidSiblingCalls").toSet
    val unknownLayers = forbidSiblingCalls -- layers.map(_.name)

    if (matchPath.isEmpty) Left("a module has an empty `match`")
    else if (layers.isEmpty) Left(s"the module matching '$matchPath' has no layers")
    else if (unknownLayers.nonEmpty)
      Left(s"the module matching '$matchPath' forbids sibling calls in unknown layers: ${unknownLayers.mkString(", ")}")
    else
      Right(
        Block(
          matchPath = matchPath,
          layers = layers,
          forbidSiblingCalls = forbidSiblingCalls,
          leaves = strings("leaves")
        )
      )
  }
}
