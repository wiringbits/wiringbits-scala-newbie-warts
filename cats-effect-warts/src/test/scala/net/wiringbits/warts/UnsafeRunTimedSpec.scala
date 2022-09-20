package net.wiringbits.warts

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class UnsafeRunTimedSpec extends AnyFunSuite with CustomAssertions {
  private val oneMinuteTimeout = 1 minute

  test("unsafeRunTimed from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeRunTimed) {
      new CustomIO("works").unsafeRunTimed(oneMinuteTimeout)
    }
    assertEmpty(result)
  }

  test("IO#unsafeRunTimed is disabled") {
    val result = WartTestTraverser(UnsafeRunTimed) {
      IO.pure(0).unsafeRunTimed(oneMinuteTimeout)
    }
    val errorMessage = "[wartremover:UnsafeRunTimed] " + UnsafeRunTimed.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeRunTimed obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeRunTimed) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunTimed"))
      class A {
        def f = IO.pure(0).unsafeRunTimed(oneMinuteTimeout)
      }
    }
    assertEmpty(result)
  }
}
