package net.wiringbits.warts

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeRunCancelableSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeRunCancelable from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeRunCancelable) {
      new CustomIO("works").unsafeRunCancelable()
    }
    assertEmpty(result)
  }

  test("IO#unsafeRunCancelable is disabled") {
    val result = WartTestTraverser(UnsafeRunCancelable) {
      IO.pure(0).unsafeRunCancelable()
    }
    val errorMessage = "[wartremover:UnsafeRunCancelable] " + UnsafeRunCancelable.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeRunCancelable obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeRunCancelable) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunCancelable"))
      class A {
        def f = IO.pure(0).unsafeRunCancelable()
      }
    }
    assertEmpty(result)
  }
}
