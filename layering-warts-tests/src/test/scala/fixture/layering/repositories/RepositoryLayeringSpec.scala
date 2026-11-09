package fixture.layering.repositories

import fixture.layering.actions.OtherAction
import fixture.layering.repositories.daos.UserDao
import fixture.layering.repositories.models.UserRow
import net.wiringbits.warts.Layering
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class RepositoryLayeringSpec extends AnyFunSuite {
  test("a call to a higher layer is flagged") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = OtherAction.run()
      }
    }
    assert(
      result.errors == List(
        "[wartremover:Layering] RepositoryLayeringSpec in repositories must not call OtherAction in actions; repositories calls daos, shared."
      )
    )
  }

  test("the longest package prefix classifies the callee") {
    // repositories.models belongs to shared and repositories.daos to daos, not to repositories
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: UserRow = UserRow(1)
        def g: Int = UserDao.find()
      }
    }
    assert(result.errors.isEmpty)
  }
}
