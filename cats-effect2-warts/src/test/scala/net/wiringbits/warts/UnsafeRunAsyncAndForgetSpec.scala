package net.wiringbits.warts

import cats.effect.IO
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeRunAsyncAndForgetSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeRunAsyncAndForget from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeRunAsyncAndForget) {
      new CustomIO("works").unsafeRunAsyncAndForget()
    }
    assertEmpty(result)
  }

  test("IO#unsafeRunAsyncAndForget is disabled") {
    val result = WartTestTraverser(UnsafeRunAsyncAndForget) {
      IO.pure(0).unsafeRunAsyncAndForget()
    }
    val errorMessage = "[wartremover:UnsafeRunAsyncAndForget] " + UnsafeRunAsyncAndForget.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeRunAsyncAndForget obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeRunAsyncAndForget) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunAsyncAndForget"))
      class A {
        def f = IO.pure(0).unsafeRunAsyncAndForget()
      }
    }
    assertEmpty(result)
  }
}
