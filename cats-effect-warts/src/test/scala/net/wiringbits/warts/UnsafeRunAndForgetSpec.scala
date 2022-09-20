package net.wiringbits.warts

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeRunAndForgetSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeRunAndForget from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeRunAndForget) {
      new CustomIO("works").unsafeRunAndForget()
    }
    assertEmpty(result)
  }

  test("IO#unsafeRunAndForget is disabled") {
    val result = WartTestTraverser(UnsafeRunAndForget) {
      IO.pure(0).unsafeRunAndForget()
    }
    val errorMessage = "[wartremover:UnsafeRunAndForget] " + UnsafeRunAndForget.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeRunAndForget obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeRunAndForget) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunAndForget"))
      class A {
        def f = IO.pure(0).unsafeRunAndForget()
      }
    }
    assertEmpty(result)
  }
}
