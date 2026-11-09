package fixture.layering.actions

import fixture.layering.*
import fixture.layering.controllers.{AdminController, HomeController}
import fixture.layering.models.User
import fixture.layering.repositories.UserRepository
import fixture.layering.unlayered.Stray
import net.wiringbits.warts.Layering
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

// The specs live in the layer they exercise, the module config is layering-warts-tests/.wartremover-layering.conf.
class LayeringSpec extends AnyFunSuite with BaseAction {
  private val prefix = "[wartremover:Layering] "

  test("the module config is found from the compiled source and a sibling call without a self edge is flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(
      result.errors == List(
        prefix + "LayeringSpec must not call sibling OtherAction, actions does not call itself; shared logic belongs in a layer both can call."
      )
    )
    assert(result.warnings.isEmpty)
  }

  test("a call to a layer missing from calls is flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = HomeController.index()
      }
    }
    assert(
      result.errors == List(
        prefix + "LayeringSpec in actions must not call HomeController in controllers; actions calls repositories, shared."
      )
    )
  }

  test("listed layers, base types, own members and code outside the roots are not flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = UserRepository.find()
        def g: User = User(1)
        def h: Int = shared()
        def i: List[Int] = List(1, 2)
        def j: String = java.util.UUID.randomUUID().toString
      }
    }
    assert(result.errors.isEmpty)
    assert(result.warnings.isEmpty)
  }

  test("a sibling defined in the same file is allowed") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = SameFileAction.run()
      }
    }
    assert(result.errors.isEmpty)
  }

  test("a sibling defined in the same file is flagged when allowSameFileSiblings is false") {
    val result = WartTestTraverser(NoSameFileSiblingsFixture) {
      object Probe {
        def f: Int = SameFileAction.run()
      }
    }
    assert(
      result.errors == List(
        "[wartremover:NoSameFileSiblingsFixture] LayeringSpec must not call sibling SameFileAction, actions does not call itself; shared logic belongs in a layer both can call."
      )
    )
  }

  test("a package under the roots belonging to no layer is flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = Stray.run()
      }
    }
    assert(
      result.errors == List(
        prefix + "LayeringSpec must not call Stray, its package fixture.layering.unlayered belongs to no layer; add it to a layer."
      )
    )
  }

  test("type only references are not flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f(c: AdminController): Option[AdminController] = Option(c)
      }
    }
    assert(result.errors.isEmpty)
  }

  test("package objects and top level definitions are not flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = actionsHelper()
        def g: Int = fixture.layering.controllers.controllersHelper()
        def h: Int = topLevelHelper()
      }
    }
    assert(result.errors.isEmpty)
    assert(result.warnings.isEmpty)
  }

  test("obeys SuppressWarnings") {
    val result = WartTestTraverser(Layering) {
      @SuppressWarnings(Array("net.wiringbits.warts.Layering"))
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(result.errors.isEmpty)
  }

  test("an invalid config is reported as a compiler error") {
    val result = WartTestTraverser(InvalidLayeringFixture) {
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(result.errors.size == 1)
    assert(result.errors.head.startsWith("[wartremover:InvalidLayeringFixture] Invalid Layering config"))
  }

  test("a missing config is reported as a compiler error naming where it was looked up") {
    val result = WartTestTraverser(MissingLayeringFixture) {
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(result.errors.size == 1)
    assert(result.errors.head.contains("no .wartremover-layering.conf was found in /no-such-dir or its parents"))
  }
}

object SameFileAction {
  def run(): Int = 1
}
