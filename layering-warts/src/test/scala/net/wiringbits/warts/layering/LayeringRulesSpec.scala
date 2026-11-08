package net.wiringbits.warts.layering

import org.scalatest.funsuite.AnyFunSuite

class LayeringRulesSpec extends AnyFunSuite {
  private val config = LayeringConfig
    .parse(
      """modules = [
        |  {
        |    match = "/server/"
        |    layers = [
        |      { name = controllers, packages = ["controllers"] }
        |      { name = actions,      packages = ["net.wiringbits.actions"] }
        |      { name = repositories, packages = ["net.wiringbits.repositories"] }
        |      { name = daos,         packages = ["net.wiringbits.repositories.daos"] }
        |    ]
        |    forbidSiblingCalls = [controllers, actions, repositories]
        |    leaves = ["net.wiringbits.models", "net.wiringbits.util"]
        |  }
        |  {
        |    match = "/web/"
        |    layers = [
        |      { name = pages,   packages = ["net.wiringbits.components.pages"] }
        |      { name = widgets, packages = ["net.wiringbits.components.widgets"] }
        |    ]
        |    forbidSiblingCalls = [pages]
        |    leaves = ["net.wiringbits.core"]
        |  }
        |  {
        |    match = "/web/admin/"
        |    layers = [
        |      { name = screens, packages = ["net.wiringbits.admin"] }
        |    ]
        |  }
        |]
        |""".stripMargin
    )
    .fold(e => fail(e), identity)

  private val serverPath = "/repo/server/app/controllers/UsersController.scala"
  private val server = config.selectBlock(serverPath).getOrElse(fail("server block not selected"))

  private val ok = None
  private def error(message: String) = Some(LayeringError(message))

  private val serverCases = List(
    // (description, fromPkg, fromClass, toPkg, toClass, expected)
    (
      "controller to sibling controller",
      "controllers",
      "UsersController",
      "controllers",
      "AdminController",
      error("UsersController must not call sibling AdminController; shared logic belongs one layer down.")
    ),
    ("controller to action", "controllers", "UsersController", "net.wiringbits.actions", "CreateUserAction", ok),
    (
      "action to sibling action",
      "net.wiringbits.actions",
      "CreateUserAction",
      "net.wiringbits.actions",
      "SendEmailAction",
      error("CreateUserAction must not call sibling SendEmailAction; shared logic belongs one layer down.")
    ),
    (
      "action to repository",
      "net.wiringbits.actions",
      "CreateUserAction",
      "net.wiringbits.repositories",
      "UsersRepository",
      ok
    ),
    (
      "repository to sibling repository",
      "net.wiringbits.repositories",
      "UsersRepository",
      "net.wiringbits.repositories",
      "TokensRepository",
      error("UsersRepository must not call sibling TokensRepository; shared logic belongs one layer down.")
    ),
    (
      "repository to dao",
      "net.wiringbits.repositories",
      "UsersRepository",
      "net.wiringbits.repositories.daos",
      "UsersDAO",
      ok
    ),
    (
      "repository to action, upward",
      "net.wiringbits.repositories",
      "UsersRepository",
      "net.wiringbits.actions",
      "CreateUserAction",
      error(
        "UsersRepository must not call CreateUserAction from the higher layer actions; calls go downward from repositories."
      )
    ),
    (
      "dao to repository, upward",
      "net.wiringbits.repositories.daos",
      "UsersDAO",
      "net.wiringbits.repositories",
      "UsersRepository",
      error("UsersDAO must not call UsersRepository from the higher layer repositories; calls go downward from daos.")
    ),
    (
      "dao to sibling dao, daos allow it and are not read as repositories",
      "net.wiringbits.repositories.daos",
      "UsersDAO",
      "net.wiringbits.repositories.daos",
      "TokensDAO",
      ok
    ),
    ("controller to leaf model", "controllers", "UsersController", "net.wiringbits.models", "User", ok),
    (
      "dao to leaf util in a subpackage",
      "net.wiringbits.repositories.daos",
      "UsersDAO",
      "net.wiringbits.util.time",
      "Clock",
      ok
    ),
    (
      "a class and its own companion",
      "net.wiringbits.actions",
      "CreateUserAction",
      "net.wiringbits.actions",
      "CreateUserAction",
      ok
    ),
    ("stdlib from a layer", "net.wiringbits.actions", "CreateUserAction", "scala.collection.immutable", "List", ok),
    ("third party from a layer", "net.wiringbits.actions", "CreateUserAction", "cats.effect", "IO", ok),
    ("unlayered code calling a layer upward", "net.wiringbits.models", "User", "controllers", "UsersController", ok),
    ("package prefix is matched by segments", "controllersExtra", "Foo", "controllersExtra", "Bar", ok),
    ("controller to its package object", "controllers", "UsersController", "controllers", "package", ok),
    (
      "action to the controllers package object",
      "net.wiringbits.actions",
      "CreateUserAction",
      "controllers",
      "package",
      ok
    ),
    ("package object to a controller", "controllers", "package", "controllers", "UsersController", ok),
    (
      "package object to a higher layer",
      "net.wiringbits.repositories",
      "package",
      "controllers",
      "UsersController",
      ok
    ),
    ("controller to a top level definition", "controllers", "UsersController", "controllers", "helpers$package", ok),
    (
      "top level definition to a higher layer",
      "net.wiringbits.actions",
      "helpers$package",
      "controllers",
      "UsersController",
      ok
    )
  )

  serverCases.foreach { case (description, fromPkg, fromClass, toPkg, toClass, expected) =>
    test(s"server: $description") {
      assert(LayeringRules.verdict(server, fromPkg, fromClass, toPkg, toClass) == expected)
    }
  }

  test("a /server/ path selects the server block and a /web/ path the web block") {
    assert(config.selectBlock("/repo/server/app/Foo.scala").map(_.matchPath).contains("/server/"))
    assert(config.selectBlock("/repo/web/src/main/scala/Foo.scala").map(_.matchPath).contains("/web/"))
  }

  test("windows paths are matched with forward slashes") {
    assert(config.selectBlock("C:\\repo\\server\\app\\Foo.scala").map(_.matchPath).contains("/server/"))
  }

  test("a path matching no block is unchecked") {
    assert(config.selectBlock("/repo/lib/src/main/scala/Foo.scala").isEmpty)
  }

  test("the longest match wins when blocks overlap") {
    assert(config.selectBlock("/repo/web/admin/Foo.scala").map(_.matchPath).contains("/web/admin/"))
  }

  test("each block applies its own layer model") {
    val web = config.selectBlock("/repo/web/src/Foo.scala").getOrElse(fail("web block not selected"))
    val pages = "net.wiringbits.components.pages"
    val widgets = "net.wiringbits.components.widgets"
    assert(
      LayeringRules.verdict(web, pages, "HomePage", pages, "AboutPage") ==
        error("HomePage must not call sibling AboutPage; shared logic belongs one layer down.")
    )
    assert(LayeringRules.verdict(web, pages, "HomePage", widgets, "Button") == ok)
    assert(LayeringRules.verdict(web, widgets, "Button", widgets, "Icon") == ok)
    // the server layers mean nothing to the web block
    assert(LayeringRules.verdict(web, "net.wiringbits.repositories", "A", "net.wiringbits.repositories", "B") == ok)
  }
}
