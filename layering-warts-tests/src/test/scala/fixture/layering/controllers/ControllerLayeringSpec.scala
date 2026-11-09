package fixture.layering.controllers

import fixture.layering.repositories.UserRepository
import fixture.layering.repositories.daos.UserDao
import net.wiringbits.warts.Layering
import org.scalatest.funsuite.AnyFunSuite
import org.wartremover.test.WartTestTraverser

class ControllerLayeringSpec extends AnyFunSuite {
  test("skipping a layer is allowed when the edge is listed") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = UserRepository.find()
      }
    }
    assert(result.errors.isEmpty)
  }

  test("skipping a layer is flagged when the edge is not listed") {
    val result = WartTestTraverser(Layering) {
      object Probe {
        def f: Int = UserDao.find()
      }
    }
    assert(
      result.errors == List(
        "[wartremover:Layering] ControllerLayeringSpec in controllers must not call UserDao in daos; controllers calls actions, repositories, shared."
      )
    )
  }
}
