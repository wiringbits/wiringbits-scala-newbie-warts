package net.wiringbits.warts.layering

final case class LayeringError(message: String)

object LayeringRules {

  /** Decides whether a reference from `fromClass` (a top level class in `fromPkg`) to `toClass` (a top level class in
    * `toPkg`) breaks the layering of the block. Calls go downward through the layers, never sideways in a layer that
    * forbids sibling calls.
    */
  def verdict(
      block: Block,
      fromPkg: String,
      fromClass: String,
      toPkg: String,
      toClass: String
  ): Option[LayeringError] = {
    if (fromPkg == toPkg && fromClass == toClass) None
    else if (isPackageLevel(fromClass) || isPackageLevel(toClass)) None
    else if (block.leaves.exists(inPackage(toPkg, _))) None
    else
      for {
        (fromLayer, fromRank) <- layerOf(block, fromPkg)
        (toLayer, toRank) <- layerOf(block, toPkg)
        error <-
          if (toRank < fromRank)
            Some(
              LayeringError(
                s"$fromClass must not call $toClass from the higher layer ${toLayer.name}; calls go downward from ${fromLayer.name}."
              )
            )
          else if (toRank == fromRank && block.forbidSiblingCalls(toLayer.name))
            Some(LayeringError(s"$fromClass must not call sibling $toClass; shared logic belongs one layer down."))
          else None
      } yield error
  }

  /** The layer owning the package, the longest matching package prefix wins. */
  def layerOf(block: Block, pkg: String): Option[(Layer, Int)] =
    block.layers.zipWithIndex
      .flatMap { case (layer, rank) =>
        layer.packages.filter(inPackage(pkg, _)).map(prefix => (prefix.length, layer, rank))
      }
      .maxByOption(_._1)
      .map { case (_, layer, rank) => (layer, rank) }

  // Package objects (named `package`) and Scala 3 top level definitions (wrapped in `<file>$package`) hold package wide
  // helpers shared by the layer, they are not peer classes of it.
  private def isPackageLevel(className: String): Boolean =
    className == "package" || className.endsWith("$package")

  private def inPackage(pkg: String, prefix: String): Boolean =
    pkg == prefix || pkg.startsWith(prefix + ".")
}
