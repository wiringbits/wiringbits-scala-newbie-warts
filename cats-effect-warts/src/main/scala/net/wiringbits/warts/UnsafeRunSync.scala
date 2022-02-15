package net.wiringbits.warts

import org.wartremover.{WartTraverser, WartUniverse}

object UnsafeRunSync extends WartTraverser {
  private val message =
    """unsafeRunSync is a pretty dangerous method for applications.
      |Avoid it unless you know what you are doing, in which case, you can
      |supress the warning by annotating your code with:
      |@SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunSync"))
      |""".stripMargin

  def apply(u: WartUniverse): u.Traverser = {
    import u.universe._

    val targetMethodName: TermName = TermName("unsafeRunSync")
    val targetSymbol = typeOf[cats.effect.IO[Any]]
    val targetMethod = targetSymbol.member(targetMethodName)
    require(targetMethod != NoSymbol)

    new Traverser {
      override def traverse(tree: Tree): Unit = {
        tree match {
          // Ignore trees marked by SuppressWarnings
          case t if hasWartAnnotation(u)(t) => ()
          case Apply(Apply(method, _), _) if method.symbol == targetMethod =>
            error(u)(tree.pos, message)
            super.traverse(tree)

          case _ => super.traverse(tree)
        }
      }
    }
  }
}
