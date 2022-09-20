package net.wiringbits.warts

import cats.effect.IO
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeToFutureSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeToFuture from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeToFuture) {
      new CustomIO("works").unsafeToFuture()
    }
    assertEmpty(result)
  }

  test("IO#unsafeToFuture is disabled") {
    val result = WartTestTraverser(UnsafeToFuture) {
      IO.pure(0).unsafeToFuture()
    }
    val errorMessage = "[wartremover:UnsafeToFuture] " + UnsafeToFuture.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeToFuture obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeToFuture) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeToFuture"))
      class A {
        def f = IO.pure(0).unsafeToFuture()
      }
    }
    assertEmpty(result)
  }
}
