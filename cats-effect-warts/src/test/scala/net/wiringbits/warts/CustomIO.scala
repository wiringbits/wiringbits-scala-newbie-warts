package net.wiringbits.warts
import cats.Id
import cats.effect.{Outcome, unsafe}

import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

final class CustomIO[T](x: T) {
  def unsafeRunSync()(implicit runtime: unsafe.IORuntime): T = x
  def unsafeRunAndForget()(implicit runtime: unsafe.IORuntime): Unit = ()
  def unsafeRunAsync(cb: Either[Throwable, T] => Unit)(implicit runtime: unsafe.IORuntime): Unit = ()
  def unsafeRunAsyncOutcome(cb: Outcome[Id, Throwable, T @uncheckedVariance] => Unit)(implicit
      runtime: unsafe.IORuntime
  ): Unit = ()

  def unsafeRunCancelable()(implicit runtime: unsafe.IORuntime): () => Future[Unit] =
    () => Future(())(ExecutionContext.global)
  def unsafeRunTimed(limit: FiniteDuration)(implicit runtime: unsafe.IORuntime): Option[T] = Some(x)
  def unsafeToFuture()(implicit runtime: unsafe.IORuntime): Future[T] = Future(x)(ExecutionContext.global)
  def unsafeToFutureCancelable()(implicit runtime: unsafe.IORuntime): (Future[T], () => Future[Unit]) = (
    Future(x)(ExecutionContext.global),
    () => Future(())(ExecutionContext.global)
  )
}
