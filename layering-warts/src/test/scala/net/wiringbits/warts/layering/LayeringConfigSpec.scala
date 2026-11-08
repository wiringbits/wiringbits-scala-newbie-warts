package net.wiringbits.warts.layering

import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class LayeringConfigSpec extends AnyFunSuite {
  private val valid =
    """modules = [
      |  {
      |    match = "/server/"
      |    layers = [ { name = controllers, packages = ["controllers"] } ]
      |  }
      |]
      |""".stripMargin

  test("optional settings default to empty") {
    val block = LayeringConfig.parse(valid).fold(e => fail(e), identity).modules.head
    assert(block.forbidSiblingCalls.isEmpty)
    assert(block.leaves.isEmpty)
  }

  test("a missing file fails naming the expected path and format") {
    val dir = Files.createTempDirectory("layering")
    val path = dir.resolve(LayeringConfig.FileName)
    val result = LayeringConfig.load(path)
    assert(result.left.exists(_.contains(path.toString)))
    assert(result.left.exists(_.contains("does not exist")))
    assert(result.left.exists(_.contains(LayeringConfig.ExpectedFormat)))
  }

  test("an existing file is loaded") {
    val path = Files.createTempDirectory("layering").resolve(LayeringConfig.FileName)
    Files.write(path, valid.getBytes(StandardCharsets.UTF_8))
    assert(LayeringConfig.load(path).map(_.modules.map(_.matchPath)) == Right(List("/server/")))
  }

  test("an unparseable file fails naming the path and the expected format") {
    val path = Files.createTempDirectory("layering").resolve(LayeringConfig.FileName)
    Files.write(path, "modules = [ { match = ".getBytes(StandardCharsets.UTF_8))
    val result = LayeringConfig.load(path)
    assert(result.left.exists(_.contains(path.toString)))
    assert(result.left.exists(_.contains("Invalid Layering config")))
    assert(result.left.exists(_.contains(LayeringConfig.ExpectedFormat)))
  }

  private val invalidCases = List(
    "missing modules" -> "foo = 1",
    "missing match" -> """modules = [ { layers = [ { name = a, packages = ["a"] } ] } ]""",
    "missing layers" -> """modules = [ { match = "/server/" } ]""",
    "empty layers" -> """modules = [ { match = "/server/", layers = [] } ]""",
    "empty match" -> """modules = [ { match = "", layers = [ { name = a, packages = ["a"] } ] } ]""",
    "layer without packages" -> """modules = [ { match = "/server/", layers = [ { name = a } ] } ]""",
    "sibling ban on an unknown layer" ->
      """modules = [ { match = "/server/", layers = [ { name = a, packages = ["a"] } ], forbidSiblingCalls = [b] } ]""",
    "wrong type" -> """modules = [ { match = "/server/", layers = "controllers" } ]"""
  )

  invalidCases.foreach { case (description, text) =>
    test(s"invalid config is rejected: $description") {
      val result = LayeringConfig.parse(text)
      assert(result.left.exists(_.startsWith("Invalid Layering config")), result)
      assert(result.left.exists(_.contains(LayeringConfig.ExpectedFormat)))
    }
  }
}
