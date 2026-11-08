package fixture.layering

import net.wiringbits.warts.LayeringTraverser
import net.wiringbits.warts.layering.LayeringConfig

import java.nio.file.Paths

/** Layering warts with an inline config, they must be compiled before the specs that run them. */
object LayeringFixture extends LayeringTraverser {
  override protected lazy val config: Either[String, LayeringConfig] = LayeringConfig.parse(
    """modules = [
      |  {
      |    match = "/layering-warts-tests/src/test/"
      |    layers = [
      |      { name = controllers,  packages = ["fixture.layering.controllers"] }
      |      { name = actions,      packages = ["fixture.layering.actions"] }
      |      { name = repositories, packages = ["fixture.layering.repositories"] }
      |    ]
      |    forbidSiblingCalls = [controllers, actions, repositories]
      |    leaves = ["fixture.layering.models"]
      |  }
      |]
      |""".stripMargin
  )
}

object UnmatchedLayeringFixture extends LayeringTraverser {
  override protected lazy val config: Either[String, LayeringConfig] = LayeringConfig.parse(
    """modules = [
      |  {
      |    match = "/no-such-module/"
      |    layers = [ { name = actions, packages = ["fixture.layering.actions"] } ]
      |    forbidSiblingCalls = [actions]
      |  }
      |]
      |""".stripMargin
  )
}

object InvalidLayeringFixture extends LayeringTraverser {
  override protected lazy val config: Either[String, LayeringConfig] = LayeringConfig.parse("modules = [ { match = ")
}

object MissingLayeringFixture extends LayeringTraverser {
  override protected lazy val config: Either[String, LayeringConfig] =
    LayeringConfig.load(Paths.get("/no-such-dir", LayeringConfig.FileName))
}
