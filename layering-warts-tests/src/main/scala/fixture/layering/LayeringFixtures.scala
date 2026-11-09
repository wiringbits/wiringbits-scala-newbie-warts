package fixture.layering

import net.wiringbits.warts.LayeringTraverser
import net.wiringbits.warts.layering.LayeringConfig

import java.nio.file.{Path, Paths}

// Layering warts with a tweaked config, they must be compiled before the specs that run them.

object NoSameFileSiblingsFixture extends LayeringTraverser {
  override protected def configFor(sourcePath: Path): Either[String, LayeringConfig] =
    LayeringConfig.forSource(sourcePath).map(_.copy(allowSameFileSiblings = false))
}

object InvalidLayeringFixture extends LayeringTraverser {
  override protected def configFor(sourcePath: Path): Either[String, LayeringConfig] =
    LayeringConfig.parse("roots = [ ")
}

object MissingLayeringFixture extends LayeringTraverser {
  override protected def configFor(sourcePath: Path): Either[String, LayeringConfig] =
    LayeringConfig.forSource(Paths.get("/no-such-dir/Foo.scala"))
}
