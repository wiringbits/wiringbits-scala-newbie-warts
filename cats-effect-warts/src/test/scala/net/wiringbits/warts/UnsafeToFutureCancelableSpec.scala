package net.wiringbits.warts

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeToFutureCancelableSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeToFutureCancelable from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeToFutureCancelable) {
      new CustomIO("works").unsafeToFutureCancelable()
    }
    assertEmpty(result)
  }

  test("IO#unsafeToFutureCancelable is disabled") {
    val result = WartTestTraverser(UnsafeToFutureCancelable) {
      IO.pure(0).unsafeToFutureCancelable()
    }
    val errorMessage = "[wartremover:UnsafeToFutureCancelable] " + UnsafeToFutureCancelable.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeToFutureCancelable obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeToFutureCancelable) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeToFutureCancelable"))
      class A {
        def f = IO.pure(0).unsafeToFutureCancelable()
      }
    }
    assertEmpty(result)
  }
}
