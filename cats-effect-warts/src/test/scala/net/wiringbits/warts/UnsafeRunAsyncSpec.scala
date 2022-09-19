package net.wiringbits.warts

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeRunAsyncSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeRunSync from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeRunAsync) {
      new CustomIO("works").unsafeRunAsync(_ => ())
    }
    assertEmpty(result)
  }

  test("IO#unsafeRunAsync is disabled") {
    val result = WartTestTraverser(UnsafeRunAsync) {
      IO.pure(0).unsafeRunAsync(_ => ())
    }
    val errorMessage = "[wartremover:UnsafeRunAsync] " + UnsafeRunAsync.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeRunAsync obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeRunAsync) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunAsync"))
      class A {
        def f = IO.pure(0).unsafeRunAsync(_ => ())
      }
    }
    assertEmpty(result)
  }
}
