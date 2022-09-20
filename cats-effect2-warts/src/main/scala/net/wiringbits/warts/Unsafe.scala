package net.wiringbits.warts

import org.wartremover.{WartTraverser, WartUniverse}

object Unsafe extends WartTraverser {
  val safeTraversers: List[UnsafeWartTraverser] = List(
    UnsafeRunAsyncAndForget,
    UnsafeRunAsync,
    UnsafeRunCancelable,
    UnsafeRunSync,
    UnsafeRunTimed,
    UnsafeToFuture
  )

  def apply(u: WartUniverse): u.Traverser =
    WartTraverser.sumList(u)(safeTraversers)
}
