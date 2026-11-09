package net.wiringbits.warts.layering

final case class LayeringError(message: String)

object LayeringRules {

  /** Decides whether a reference from `fromClass` (a top level class in `fromPkg`) to `toClass` (a top level class in
    * `toPkg`) breaks the layering: a layer may only reference the layers listed in its `calls`, and every package under
    * the roots must belong to a layer, as caller or callee.
    *
    * @param sameFile
    *   both classes are defined in the same source file
    */
  def verdict(
      config: LayeringConfig,
      fromPkg: String,
      fromClass: String,
      toPkg: String,
      toClass: String,
      sameFile: Boolean
  ): Option[LayeringError] = {
    if (fromPkg == toPkg && fromClass == toClass) None
    else if (isPackageLevel(fromClass) || isPackageLevel(toClass)) None
    else if (!config.roots.exists(inPackage(toPkg, _))) None
    else
      layerOf(config, toPkg) match {
        case None =>
          Some(
            LayeringError(
              s"$fromClass must not call $toClass, its package $toPkg belongs to no layer; add it to a layer."
            )
          )
        case Some(toLayer) =>
          layerOf(config, fromPkg) match {
            case None if config.roots.exists(inPackage(fromPkg, _)) =>
              Some(
                LayeringError(
                  s"$fromClass must not call $toClass, the caller package $fromPkg belongs to no layer; add it to a layer."
                )
              )
            case None => None
            case Some(fromLayer) =>
              if (fromLayer.calls(toLayer.name)) None
              else if (fromLayer == toLayer && config.allowSameFileSiblings && sameFile) None
              else if (fromLayer == toLayer)
                Some(
                  LayeringError(
                    s"$fromClass must not call sibling $toClass, ${fromLayer.name} does not call itself; shared logic belongs in a layer both can call."
                  )
                )
              else
                Some(
                  LayeringError(
                    s"$fromClass in ${fromLayer.name} must not call $toClass in ${toLayer.name}; ${fromLayer.name} calls ${describe(fromLayer.calls)}."
                  )
                )
          }
      }
  }

  /** The layer owning the package, the longest matching package prefix wins. */
  def layerOf(config: LayeringConfig, pkg: String): Option[Layer] =
    config.layers
      .flatMap(layer => layer.packages.filter(inPackage(pkg, _)).map(prefix => (prefix.length, layer)))
      .maxByOption(_._1)
      .map(_._2)

  def inPackage(pkg: String, prefix: String): Boolean =
    pkg == prefix || pkg.startsWith(prefix + ".")

  private def describe(calls: Set[String]): String =
    if (calls.isEmpty) "no layer" else calls.toList.sorted.mkString(", ")

  // Package objects (named `package`) and Scala 3 top level definitions (wrapped in `<file>$package`) hold package wide
  // helpers shared by the layer, they are not peer classes of it.
  private def isPackageLevel(className: String): Boolean =
    className == "package" || className.endsWith("$package")
}
