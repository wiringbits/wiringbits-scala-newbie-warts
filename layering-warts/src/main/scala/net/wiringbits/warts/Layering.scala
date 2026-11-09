package net.wiringbits.warts

import net.wiringbits.warts.layering.{LayeringConfig, LayeringRules}
import org.wartremover.{WartTraverser, WartUniverse}

import java.nio.file.{Path, Paths}
import scala.collection.mutable

/** Enforces package layering: a layer only references the layers it declares in `calls`.
  *
  * WartRemover cannot pass configuration to a custom wart, so each module declares its layer model in a
  * `.wartremover-layering.conf` at its base directory, the nearest one to the compiled source file applies, see
  * [[LayeringConfig]]. A missing or invalid file is reported as a compiler error.
  */
object Layering extends LayeringTraverser {
  override protected def configFor(sourcePath: Path): Either[String, LayeringConfig] =
    LayeringConfig.forSource(sourcePath)
}

/** Reads `(fromPkg, fromClass, toPkg, toClass, sameFile)` for every term reference and delegates the decision to
  * [[LayeringRules]].
  */
abstract class LayeringTraverser extends WartTraverser {

  /** The layer model governing the source file. */
  protected def configFor(sourcePath: Path): Either[String, LayeringConfig]

  def apply(u: WartUniverse): u.Traverser = {
    new u.Traverser(this) {
      import q.reflect.*

      private val configs = mutable.Map.empty[String, Either[String, LayeringConfig]]
      private val configErrorsReported = mutable.Set.empty[String]
      private val reported = mutable.Set.empty[(String, Int, String)]

      // The top level class or object enclosing the symbol, None for packages and symbols without owner.
      private def topLevel(sym: Symbol): Option[Symbol] = {
        @annotation.tailrec
        def loop(s: Symbol): Option[Symbol] =
          if (s.isNoSymbol || s.isPackageDef) None
          else {
            val owner = s.maybeOwner
            if (owner.isNoSymbol) None
            else if (owner.isPackageDef) Some(s)
            else loop(owner)
          }

        loop(sym)
      }

      // A class and its companion object share the name, the module class carries a trailing $.
      private def className(sym: Symbol): String = sym.name.stripSuffix("$")

      private def classSymbol(sym: Symbol): Symbol =
        if (sym.isClassDef) sym else sym.moduleClass

      private def isBaseType(from: Symbol, to: Symbol): Boolean = {
        val fromClass = classSymbol(from)
        val toClass = classSymbol(to)
        !fromClass.isNoSymbol && !toClass.isNoSymbol && fromClass.typeRef.baseClasses.contains(toClass)
      }

      private def check(ref: Ref, owner: Symbol): Unit = {
        val sym = ref.symbol
        for {
          from <- topLevel(owner)
          to <- topLevel(sym)
          if !isBaseType(from, to)
          // The enclosing class lives in the compiled file, unlike the reference which may come from an inlined body.
          fromFile <- from.pos.map(_.sourceFile)
          config <- configOf(fromFile, ref.pos)
          fromPkg = from.maybeOwner.fullName
          toPkg = to.maybeOwner.fullName
          toClass = className(to)
          sameFile = to.pos.exists(_.sourceFile.path == fromFile.path)
          violation <- LayeringRules.verdict(config, fromPkg, className(from), toPkg, toClass, sameFile)
          // A selection like Foo.bar starts where its qualifier Foo starts, report the expression once.
          if reported.add((ref.pos.sourceFile.path, ref.pos.start, s"$toPkg.$toClass"))
        } error(ref.pos, violation.message)
      }

      // Reports a missing or invalid config once per source file.
      private def configOf(file: SourceFile, pos: Position): Option[LayeringConfig] =
        configs.getOrElseUpdate(file.path, configFor(file.getJPath.getOrElse(Paths.get(file.path)))) match {
          case Right(config) => Some(config)
          case Left(message) =>
            if (configErrorsReported.add(file.path)) error(pos, message)
            None
        }

      override def traverseTree(tree: Tree)(owner: Symbol): Unit = {
        tree match {
          case _ if hasWartAnnotation(tree) => ()
          case _: Import | _: Export => ()
          case ref: Ref =>
            check(ref, owner)
            super.traverseTree(tree)(owner)
          case _ =>
            super.traverseTree(tree)(owner)
        }
      }
    }
  }
}
