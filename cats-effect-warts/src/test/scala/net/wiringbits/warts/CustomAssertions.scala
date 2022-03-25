package net.wiringbits.warts

import org.scalatest.Assertions
import org.wartremover.test.WartTestTraverser

// Based on https://github.com/wartremover/wartremover/blob/08bd815cccadba6eaca63117a47b7194919a407f/core/src/test/scala/org/wartremover/test/ResultAssertions.scala
trait CustomAssertions extends Assertions {
  def assertEmpty(result: WartTestTraverser.Result) = {
    assertResult(List.empty, "result.errors")(result.errors)
    assertResult(List.empty, "result.warnings")(result.warnings)
  }

  def assertErrors(result: WartTestTraverser.Result)(message: String, times: Int) = {
    assertResult(List.fill(times)(message), "result.errors")(result.errors.map(skipTraverserPrefix))
    assertResult(List.empty, "result.warnings")(result.warnings.map(skipTraverserPrefix))
  }

  private val messageFormat = """^\[wartremover:\S+\] (.+)$""".r

  private def skipTraverserPrefix(msg: String) = msg match {
    case messageFormat(rest) => rest
    case s => s
  }
}
