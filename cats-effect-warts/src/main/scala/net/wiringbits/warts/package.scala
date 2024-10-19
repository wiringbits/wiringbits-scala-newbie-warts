package net.wiringbits.warts

import org.wartremover.{WartTraverser, WartUniverse}

sealed abstract class UnsafeWartTraverser(unsafeMethodName: String, wartClassName: String) extends WartTraverser {
  private[warts] val message =
    s"""$unsafeMethodName is a pretty dangerous method for applications.
       |Avoid it unless you know what you are doing, in which case, you can
       |suppress the warning by annotating your code with:
       |@SuppressWarnings(Array("net.wiringbits.warts.$wartClassName"))
       |""".stripMargin

  def apply(u: WartUniverse): u.Traverser = {
    new u.Traverser(this) {
      import q.reflect.*

      val targetMethodName: String = unsafeMethodName
      val targetSymbol = TypeRepr.of[cats.effect.IO[Any]].typeSymbol
      val targetMethod = targetSymbol.methodMember(targetMethodName).headOption.getOrElse {
        throw new IllegalArgumentException(s"Method $unsafeMethodName not found in cats.effect.IO")
      }

      override def traverseTree(tree: Tree)(owner: Symbol): Unit = {
        tree match {
          case _ if hasWartAnnotation(tree) => ()
          case Apply(Apply(method, _), _) if method.symbol == targetMethod =>
            error(tree.pos, message)
            super.traverseTree(tree)(owner)
          case _ =>
            super.traverseTree(tree)(owner)
        }
      }
    }
  }
}

object UnsafeRunAndForget
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeRunAndForget",
      wartClassName = "UnsafeRunAndForget"
    )

object UnsafeRunAsync
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeRunAsync",
      wartClassName = "UnsafeRunAsync"
    )

object UnsafeRunAsyncOutcome
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeRunAsyncOutcome",
      wartClassName = "UnsafeRunAsyncOutcome"
    )

object UnsafeRunCancelable
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeRunCancelable",
      wartClassName = "UnsafeRunCancelable"
    )

object UnsafeRunSync
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeRunSync",
      wartClassName = "UnsafeRunSync"
    )

object UnsafeRunTimed
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeRunTimed",
      wartClassName = "UnsafeRunTimed"
    )

object UnsafeToFuture
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeToFuture",
      wartClassName = "UnsafeToFuture"
    )

object UnsafeToFutureCancelable
    extends UnsafeWartTraverser(
      unsafeMethodName = "unsafeToFutureCancelable",
      wartClassName = "UnsafeToFutureCancelable"
    )
