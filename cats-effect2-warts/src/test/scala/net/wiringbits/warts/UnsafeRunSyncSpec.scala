package net.wiringbits.warts

import cats.effect.IO
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnsafeRunSyncSpec extends AnyFunSuite with CustomAssertions {
  test("unsafeRunSync from non IO classes is allowed") {
    val result = WartTestTraverser(UnsafeRunSync) {
      new CustomIO("works").unsafeRunSync()
    }
    assertEmpty(result)
  }

  test("IO#unsafeRunSync is disabled") {
    val result = WartTestTraverser(UnsafeRunSync) {
      IO.pure(0).unsafeRunSync()
    }
    val errorMessage = "[wartremover:UnsafeRunSync] " + UnsafeRunSync.message
    assertErrors(result)(errorMessage, 1)
  }

  test("IO#unsafeRunSync obeys SuppressWarnings") {
    val result = WartTestTraverser(UnsafeRunSync) {
      @SuppressWarnings(Array("net.wiringbits.warts.UnsafeRunSync"))
      class A {
        def f = IO.pure(0).unsafeRunSync()
      }
    }
    assertEmpty(result)
  }
}
