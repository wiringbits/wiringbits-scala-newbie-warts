package fixture.layering.repositories.daos

import net.wiringbits.warts.Layering
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class DaoLayeringSpec extends AnyFunSuite {
  test("a sibling call is allowed when the layer calls itself") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = UserDao.find()
      }
    }
    assert(result.errors.isEmpty)
  }
}
