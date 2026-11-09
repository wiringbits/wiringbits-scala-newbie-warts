package fixture.layering.models

import fixture.layering.controllers.HomeController
import net.wiringbits.warts.Layering
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class ModelLayeringSpec extends AnyFunSuite {
  test("the bottom layer calling a controller is flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = HomeController.index()
      }
    }
    assert(
      result.errors == List(
        "[wartremover:Layering] ModelLayeringSpec in shared must not call HomeController in controllers; shared calls shared."
      )
    )
  }

  test("the bottom layer calling itself is allowed") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Account = Account(User(1).id)
      }
    }
    assert(result.errors.isEmpty)
  }
}
