package net.wiringbits.warts

import net.wiringbits.warts.layering.{LayeringConfig, LayeringRules}
import org.wartremover.{WartTraverser, WartUniverse}

import java.nio.file.Paths
import scala.collection.mutable

/** Enforces package layering: calls go downward through the layers, never sideways in layers that forbid it.
  *
  * WartRemover cannot pass configuration to a custom wart, so the layer model is read from `.wartremover-layering.conf`
  * in the compiler's working directory (the build root), see [[LayeringConfig]]. The file is loaded once, a missing or
  * invalid file is reported as a compiler error.
  */
object Layering extends LayeringTraverser {
  override protected lazy val config: Either[String, LayeringConfig] =
    LayeringConfig.load(Paths.get(LayeringConfig.FileName).toAbsolutePath)
}

/** Reads `(sourcePath, fromPkg, fromClass, toPkg, toClass)` for every term reference and delegates the decision to
  * [[LayeringRules]].
  */
abstract class LayeringTraverser extends WartTraverser {
  protected def config: Either[String, LayeringConfig]

  def apply(u: WartUniverse): u.Traverser = {
    new u.Traverser(this) {
      import q.reflect.*

      private var configErrorReported = false
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

      private def check(layering: LayeringConfig, ref: Ref, owner: Symbol): Unit = {
        val sym = ref.symbol
        for {
          block <- layering.selectBlock(ref.pos.sourceFile.path)
          from <- topLevel(owner)
          to <- topLevel(sym)
          if !isBaseType(from, to)
          fromPkg = from.maybeOwner.fullName
          toPkg = to.maybeOwner.fullName
          toClass = className(to)
          violation <- LayeringRules.verdict(block, fromPkg, className(from), toPkg, toClass)
          // A selection like Foo.bar starts where its qualifier Foo starts, report the expression once.
          if reported.add((ref.pos.sourceFile.path, ref.pos.start, s"$toPkg.$toClass"))
        } error(ref.pos, violation.message)
      }

      override def traverseTree(tree: Tree)(owner: Symbol): Unit = {
        config match {
          case Left(message) =>
            if (!configErrorReported) {
              configErrorReported = true
              error(tree.pos, message)
            }
          case Right(layering) =>
            tree match {
              case _ if hasWartAnnotation(tree) => ()
              case _: Import | _: Export => ()
              case ref: Ref =>
                check(layering, ref, owner)
                super.traverseTree(tree)(owner)
              case _ =>
                super.traverseTree(tree)(owner)
            }
        }
      }
    }
  }
}
