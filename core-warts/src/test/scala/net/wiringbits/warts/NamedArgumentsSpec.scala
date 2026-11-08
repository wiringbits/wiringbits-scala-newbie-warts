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
      def f(flag: Boolean, a: String, b: String): Unit = ()
      f(true, a = "a", b = "b")
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

  test("declaring a case class with same typed fields is not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      case class Example(a: String, b: String)
      case class Ids(x: java.util.UUID, y: java.util.UUID, amount: BigDecimal)
    }
    assertEmpty(result)
  }

  test("a case class apply with same typed arguments passed positionally is flagged") {
    val result = WartTestTraverser(NamedArguments) {
      case class Example(a: String, b: String)
      Example("x", "y")
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("a case class constructor with same typed arguments passed positionally is flagged") {
    val result = WartTestTraverser(NamedArguments) {
      case class Example(a: String, b: String)
      new Example("x", "y")
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("a case class apply with same typed arguments passed by name is not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      case class Example(a: String, b: String)
      Example(a = "x", b = "y")
    }
    assertEmpty(result)
  }

  test("same typed arguments passed positionally inside a lambda are flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def merge(survivorId: String, loserId: String): Unit = ()
      List("a").foreach(x => merge(x, "b"))
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("same typed arguments passed positionally in a pattern definition are flagged") {
    val result = WartTestTraverser(NamedArguments) {
      def pair(a: String, b: String): (String, String) = (a, b)
      val (x, y) = pair("a", "b")
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("same typed arguments passed positionally in a default value are flagged") {
    val result = WartTestTraverser(NamedArguments) {
      class Point(x: Int, y: Int)
      def f(p: Point = new Point(1, 2)): Unit = ()
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("same typed parameters supplied only through default values are not flagged") {
    val result = WartTestTraverser(NamedArguments) {
      final case class Widget(child: String, margin: Int = 0, padding: Int = 0)
      def transfer(amount: Int, fromAccount: Int, toAccount: Int = 0): Unit = ()
      Widget(child = "hello")
      Widget(child = "hello", margin = 4)
      transfer(amount = 10, fromAccount = 1)
    }
    assertEmpty(result)
  }

  test("same typed parameters supplied through default values are not flagged when named arguments are reordered") {
    val result = WartTestTraverser(NamedArguments) {
      final case class Widget(child: String, margin: Int = 0, padding: Int = 0)
      def transfer(amount: Int, fromAccount: Int, toAccount: Int = 0): Unit = ()
      def n(): Int = 1
      Widget(margin = 4, child = "hello")
      Widget(margin = n(), child = "hello")
      transfer(fromAccount = n(), amount = n())
    }
    assertEmpty(result)
  }

  test("same typed arguments passed positionally to a method with default values are flagged") {
    val result = WartTestTraverser(NamedArguments) {
      final case class Widget(child: String, margin: Int = 0, padding: Int = 0)
      Widget("hello", 4, 8)
    }
    assertErrors(result)(errorMessage, 1)
  }

  test("same typed arguments passed positionally without default values are flagged unless named") {
    val result = WartTestTraverser(NamedArguments) {
      def transfer(amount: Int, fromAccount: Int, toAccount: Int): Unit = ()
      transfer(10, 1, 2)
      transfer(amount = 10, fromAccount = 1, toAccount = 2)
    }
    assertErrors(result)(errorMessage, 1)
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
