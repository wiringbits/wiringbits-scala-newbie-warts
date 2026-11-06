package net.wiringbits.warts

import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class NamedArgumentsSpec extends AnyFunSuite with CustomAssertions {
  private val errorMessage = "[wartremover:NamedArguments] " + NamedArguments.message

  test("two same typed arguments passed positionally are flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def merge(survivorId: String, loserId: String): Unit = ()
      merge("a", "b")
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("three same typed arguments passed positionally are flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def merge(survivorId: String, loserId: String, userId: String): Unit = ()
      merge("a", "b", "c")
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("same typed parameters that are not adjacent are flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def f(a: String, flag: Boolean, b: String): Unit = ()
      f("a", true, "b")
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("a constructor with same typed parameters passed positionally is flagged") {
    val result = WartTestTraverser(NamedArguments) {
      class Point(x: Int, y: Int)
      new Point(1, 2)
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("naming only the same typed arguments is enough even if distinct typed ones stay positional") {
    val result = WartTestTraverser(NamedArguments) {
      def f(a: String, flag: Boolean, b: String): Unit = ()
      f(a = "a", flag = true, b = "b")
    }
    assertEmpty(result)
  }

  test("same typed arguments passed by name are not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def merge(survivorId: String, loserId: String): Unit = ()
      merge(survivorId = "a", loserId = "b")
    }
    assertEmpty(result)
  }

  test("same typed arguments named but reordered are not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def merge(survivorId: String, loserId: String): Unit = ()
      merge(loserId = "b", survivorId = "a")
    }
    assertEmpty(result)
  }

  test("every parameter having a distinct type is not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def f(a: String, b: Int, c: Boolean): Unit = ()
      f("a", 1, true)
    }
    assertEmpty(result)
  }

  test("fewer than two parameters of any one type is not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def f(a: String, b: Int): Unit = ()
      f("a", 1)
    }
    assertEmpty(result)
  }

  test("a single argument call is not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def f(a: String): Unit = ()
      f("a")
    }
    assertEmpty(result)
  }

  test("standard library combinators such as map are not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      List(1, 2, 3).map(i => i + 1)
    }
    assertEmpty(result)
  }

  test("a vararg call is not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def f(xs: String*): Unit = ()
      f("a", "b", "c")
    }
    assertEmpty(result)
  }

  test("obeys SuppressWarnings") {
    val result = WartTestTraverser(NamedArguments) {
      @SuppressWarnings(Array("net.wiringbits.warts.NamedArguments"))
      class A {
        def merge(survivorId: String, loserId: String): Unit = ()
        def f = merge("a", "b")
      }
    }
    assertEmpty(result)
  }
}
