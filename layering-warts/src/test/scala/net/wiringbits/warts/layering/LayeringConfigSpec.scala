package net.wiringbits.warts.layering

import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

class LayeringConfigSpec extends AnyFunSuite {
  private val valid =
    """roots = ["com.example"]
      |layers = [ { name = controllers, packages = ["com.example.controllers"], calls = [] } ]
      |""".stripMargin

  private def write(path: Path, text: String): Path = {
    Files.createDirectories(path.getParent)
    Files.write(path, text.getBytes(StandardCharsets.UTF_8))
  }

  test("allowSameFileSiblings defaults to true") {
    assert(LayeringConfig.parse(valid).map(_.allowSameFileSiblings) == Right(true))
    assert(LayeringConfig.parse(valid + "allowSameFileSiblings = false").map(_.allowSameFileSiblings) == Right(false))
  }

  test("the nearest config to the source file applies") {
    val repo = Files.createTempDirectory("layering")
    write(repo.resolve(LayeringConfig.FileName), valid)
    val server =
      write(repo.resolve("server").resolve(LayeringConfig.FileName), valid.replace("com.example", "net.server"))
    val source = repo.resolve("server/app/controllers/HomeController.scala")

    assert(LayeringConfig.locate(source).contains(server))
    assert(LayeringConfig.forSource(source).map(_.roots) == Right(List("net.server")))
    assert(LayeringConfig.forSource(repo.resolve("web/src/Foo.scala")).map(_.roots) == Right(List("com.example")))
  }

  test("a source without config fails naming where it was looked up and the expected format") {
    val dir = Files.createTempDirectory("layering").resolve("server/app")
    val result = LayeringConfig.forSource(dir.resolve("Foo.scala"))
    assert(result.left.exists(_.contains(s"no ${LayeringConfig.FileName} was found in $dir or its parents")))
    assert(result.left.exists(_.contains(LayeringConfig.ExpectedFormat)))
  }

  test("an unparseable file fails naming the path and the expected format") {
    val path = write(Files.createTempDirectory("layering").resolve(LayeringConfig.FileName), "roots = [ ")
    val result = LayeringConfig.load(path)
    assert(result.left.exists(_.contains(path.toString)))
    assert(result.left.exists(_.contains("Invalid Layering config")))
    assert(result.left.exists(_.contains(LayeringConfig.ExpectedFormat)))
  }

  private val layerA = """{ name = a, packages = ["x.a"], calls = [] }"""

  private val invalidCases = List(
    "missing roots" -> s"layers = [ $layerA ]",
    "empty roots" -> s"roots = [], layers = [ $layerA ]",
    "missing layers" -> """roots = ["x"]""",
    "empty layers" -> """roots = ["x"], layers = []""",
    "layer without packages" -> """roots = ["x"], layers = [ { name = a, calls = [] } ]""",
    "layer without calls" -> """roots = ["x"], layers = [ { name = a, packages = ["x.a"] } ]""",
    "call to an unknown layer" -> """roots = ["x"], layers = [ { name = a, packages = ["x.a"], calls = [b] } ]""",
    "duplicated layer names" -> s"""roots = ["x"], layers = [ $layerA, $layerA ]""",
    "layer package outside the roots" -> """roots = ["x"], layers = [ { name = a, packages = ["y.a"], calls = [] } ]""",
    "legacy modules format" -> """modules = [ { match = "/server/", layers = [] } ]""",
    "wrong type" -> """roots = ["x"], layers = "a""""
  )

  invalidCases.foreach { case (description, text) =>
    test(s"invalid config is rejected: $description") {
      val result = LayeringConfig.parse(text)
      assert(result.left.exists(_.startsWith("Invalid Layering config")), result)
      assert(result.left.exists(_.contains(LayeringConfig.ExpectedFormat)))
    }
  }
}
