package fixture.layering.unlayered

import fixture.layering.actions.OtherAction
import net.wiringbits.warts.Layering
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class UnlayeredLayeringSpec extends AnyFunSuite {
  test("a caller under the roots belonging to no layer is flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(
      result.errors == List(
        "[wartremover:Layering] UnlayeredLayeringSpec must not call OtherAction, the caller package fixture.layering.unlayered belongs to no layer; add it to a layer."
      )
    )
  }

  test("a caller belonging to no layer may use code outside the roots") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: List[Int] = List(1)
      }
    }
    assert(result.errors.isEmpty)
  }
}
