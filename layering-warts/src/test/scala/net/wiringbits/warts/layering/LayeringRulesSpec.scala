package net.wiringbits.warts.layering

import org.scalatest.funsuite.AnyFunSuite

class LayeringRulesSpec extends AnyFunSuite {
  private val config = LayeringConfig
    .parse(
      """roots = ["net.wiringbits", "controllers"]
        |layers = [
        |  { name = routing,      packages = ["net.wiringbits.routing"],           calls = [controllers] }
        |  { name = controllers,  packages = ["controllers"],                      calls = [actions, repositories, shared] }
        |  { name = actions,      packages = ["net.wiringbits.actions"],           calls = [repositories, shared] }
        |  { name = repositories, packages = ["net.wiringbits.repositories"],      calls = [daos, shared] }
        |  { name = daos,         packages = ["net.wiringbits.repositories.daos"], calls = [daos, shared] }
        |  { name = shared,       packages = ["net.wiringbits.models", "net.wiringbits.repositories.models"], calls = [shared] }
        |]
        |""".stripMargin
    )
    .fold(e => fail(e), identity)

  private val ok = None
  private def error(message: String) = Some(LayeringError(message))

  private val actions = "net.wiringbits.actions"
  private val repositories = "net.wiringbits.repositories"
  private val daos = "net.wiringbits.repositories.daos"
  private val models = "net.wiringbits.models"

  private val cases = List(
    // (description, fromPkg, fromClass, toPkg, toClass, sameFile, expected)
    ("listed edge", "controllers", "UsersController", actions, "CreateUserAction", false, ok),
    (
      "unlisted edge",
      repositories,
      "UsersRepository",
      actions,
      "CreateUserAction",
      false,
      error(
        "UsersRepository in repositories must not call CreateUserAction in actions; repositories calls daos, shared."
      )
    ),
    ("listed skip", "controllers", "UsersController", repositories, "UsersRepository", false, ok),
    (
      "unlisted skip",
      "controllers",
      "UsersController",
      daos,
      "UsersDAO",
      false,
      error(
        "UsersController in controllers must not call UsersDAO in daos; controllers calls actions, repositories, shared."
      )
    ),
    (
      "sibling without self edge",
      actions,
      "CreateUserAction",
      actions,
      "SendEmailAction",
      false,
      error(
        "CreateUserAction must not call sibling SendEmailAction, actions does not call itself; shared logic belongs in a layer both can call."
      )
    ),
    ("sibling without self edge in the same file", actions, "CreateUserAction", actions, "Request", true, ok),
    ("sibling with self edge", daos, "UsersDAO", daos, "TokensDAO", false, ok),
    (
      "shared calling a controller",
      models,
      "User",
      "controllers",
      "UsersController",
      false,
      error("User in shared must not call UsersController in controllers; shared calls shared.")
    ),
    (
      "an unlisted edge is flagged even in the same file",
      models,
      "User",
      "controllers",
      "UsersController",
      true,
      error("User in shared must not call UsersController in controllers; shared calls shared.")
    ),
    (
      "callee under the roots in no layer",
      actions,
      "CreateUserAction",
      "net.wiringbits.config",
      "AppConfig",
      false,
      error(
        "CreateUserAction must not call AppConfig, its package net.wiringbits.config belongs to no layer; add it to a layer."
      )
    ),
    (
      "callee in the root package itself",
      actions,
      "CreateUserAction",
      "net.wiringbits",
      "Module",
      false,
      error("CreateUserAction must not call Module, its package net.wiringbits belongs to no layer; add it to a layer.")
    ),
    (
      "caller under the roots in no layer",
      "net.wiringbits.tasks",
      "JobsTask",
      actions,
      "CreateUserAction",
      false,
      error(
        "JobsTask must not call CreateUserAction, the caller package net.wiringbits.tasks belongs to no layer; add it to a layer."
      )
    ),
    (
      "caller under the roots in no layer calling outside the roots",
      "net.wiringbits.tasks",
      "JobsTask",
      "cats.effect",
      "IO",
      false,
      ok
    ),
    ("caller outside the roots", "views.html", "index", "controllers", "UsersController", false, ok),
    ("stdlib", actions, "CreateUserAction", "scala.collection.immutable", "List", false, ok),
    ("third party", actions, "CreateUserAction", "cats.effect", "IO", false, ok),
    ("root prefix is matched by segments", actions, "CreateUserAction", "net.wiringbitsx", "Foo", false, ok),
    (
      "longest prefix: repositories.models is shared",
      repositories,
      "UsersRepository",
      "net.wiringbits.repositories.models",
      "UserRow",
      false,
      ok
    ),
    ("longest prefix: repositories.daos is daos", repositories, "UsersRepository", daos, "UsersDAO", false, ok),
    (
      "a subpackage belongs to its layer",
      repositories,
      "UsersRepository",
      "net.wiringbits.repositories.support",
      "Helper",
      false,
      error(
        "UsersRepository must not call sibling Helper, repositories does not call itself; shared logic belongs in a layer both can call."
      )
    ),
    ("a class and its own companion", actions, "CreateUserAction", actions, "CreateUserAction", false, ok),
    ("controller to its package object", "controllers", "UsersController", "controllers", "package", false, ok),
    ("package object to a controller", models, "package", "controllers", "UsersController", false, ok),
    ("top level definition to a controller", models, "helpers$package", "controllers", "UsersController", false, ok),
    (
      "controller to a top level definition",
      "controllers",
      "UsersController",
      "controllers",
      "helpers$package",
      false,
      ok
    )
  )

  cases.foreach { case (description, fromPkg, fromClass, toPkg, toClass, sameFile, expected) =>
    test(description) {
      assert(LayeringRules.verdict(config, fromPkg, fromClass, toPkg, toClass, sameFile) == expected)
    }
  }

  test("a same file sibling is flagged when allowSameFileSiblings is false") {
    val strict = config.copy(allowSameFileSiblings = false)
    assert(LayeringRules.verdict(strict, actions, "CreateUserAction", actions, "Request", sameFile = true).isDefined)
  }
}
