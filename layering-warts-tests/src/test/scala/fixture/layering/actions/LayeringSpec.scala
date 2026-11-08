package fixture.layering.actions

import fixture.layering.*
import fixture.layering.controllers.HomeController
import fixture.layering.models.User
import fixture.layering.repositories.UserRepository
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

// The spec lives in the actions layer, so the code under test calls from fixture.layering.actions.LayeringSpec.
class LayeringSpec extends AnyFunSuite with BaseAction {
  private val prefix = "[wartremover:LayeringFixture] "

  test("a call to a sibling in a layer forbidding it is flagged") {
    val result = WartTestTraverser(LayeringFixture) {
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(
      result.errors == List(
        prefix + "LayeringSpec must not call sibling OtherAction; shared logic belongs one layer down."
      )
    )
    assert(result.warnings.isEmpty)
  }

  test("a call to a higher layer is flagged") {
    val result = WartTestTraverser(LayeringFixture) {
      object Probe {
        def f: Int = HomeController.index()
      }
    }
    assert(
      result.errors == List(
        prefix + "LayeringSpec must not call HomeController from the higher layer controllers; calls go downward from actions."
      )
    )
  }

  test("downward calls, leaves, base types and own members are not flagged") {
    val result = WartTestTraverser(LayeringFixture) {
      object Probe {
        def f: Int = UserRepository.find()
        def g: User = User(1)
        def h: Int = shared()
        def i: List[Int] = List(1, 2)
      }
    }
    assert(result.errors.isEmpty)
    assert(result.warnings.isEmpty)
  }

  test("package objects and top level definitions are not flagged") {
    val result = WartTestTraverser(LayeringFixture) {
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
    val result = WartTestTraverser(LayeringFixture) {
      @SuppressWarnings(Array("fixture.layering.LayeringFixture"))
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(result.errors.isEmpty)
  }

  test("a source file matching no module is not checked") {
    val result = WartTestTraverser(UnmatchedLayeringFixture) {
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(result.errors.isEmpty)
  }

  test("an invalid config is reported as a compiler error") {
    val result = WartTestTraverser(InvalidLayeringFixture) {
      object Probe {
        def f: Int = 1
      }
    }
    assert(result.errors.size == 1)
    assert(result.errors.head.startsWith("[wartremover:InvalidLayeringFixture] Invalid Layering config"))
  }

  test("a missing config is reported as a compiler error naming the expected path") {
    val result = WartTestTraverser(MissingLayeringFixture) {
      object Probe {
        def f: Int = 1
      }
    }
    assert(result.errors.size == 1)
    assert(result.errors.head.contains("/no-such-dir/.wartremover-layering.conf does not exist"))
  }
}
