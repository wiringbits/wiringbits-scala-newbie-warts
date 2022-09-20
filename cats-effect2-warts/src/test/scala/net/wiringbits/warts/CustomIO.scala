package net.wiringbits.warts
import cats.effect.{CancelToken, IO}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class CustomIO[T](x: T) {
  def unsafeRunSync(): T = x
  def unsafeRunAsyncAndForget(): Unit = ()
  def unsafeRunAsync(cb: Either[Throwable, T] => Unit): Unit = ()
  def unsafeRunCancelable(cb: Either[Throwable, T] => Unit): CancelToken[IO] = IO.unit
  def unsafeRunTimed(limit: Duration): Option[T] = Some(x)
  def unsafeToFuture(): Future[T] = Future(x)
}
