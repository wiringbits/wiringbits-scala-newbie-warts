package net.wiringbits.warts
import org.wartremover.{WartTraverser, WartUniverse}

object Unsafe extends WartTraverser {
  val safeTraversers: List[UnsafeWartTraverser] = List(
    UnsafeRunAndForget,
    UnsafeRunAsync,
    UnsafeRunAsyncOutcome,
    UnsafeRunCancelable,
    UnsafeRunSync,
    UnsafeRunTimed,
    UnsafeToFuture,
    UnsafeToFutureCancelable
  )

  def apply(u: WartUniverse): u.Traverser =
    WartTraverser.sumList(u)(safeTraversers)
}
