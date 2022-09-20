package net.wiringbits.warts

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeRunAsyncOutcomeSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeRunSync from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeRunAsyncOutcome) {
      new CustomIO("works").unsafeRunAsyncOutcome(_ => ())
    }
    assertEmpty(result)
  }

  test("IO#unsafeRunSync is disabled") {
    val result = WartTestTraverser(UnsafeRunAsyncOutcome) {
      IO.pure(0).unsafeRunAsyncOutcome(_ => ())
    }
    val errorMessage = "[wartremover:UnsafeRunAsyncOutcome] " + UnsafeRunAsyncOutcome.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeRunSync obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeRunAsyncOutcome) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunAsyncOutcome"))
      class A {
        def f = IO.pure(0).unsafeRunAsyncOutcome(_ => ())
      }
    }
    assertEmpty(result)
  }
}
