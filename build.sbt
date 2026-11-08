ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization := "net.wiringbits"
ThisBuild / scalaVersion := "3.3.9"

inThisBuild(
  List(
    description := "A collection of WartRemover warts for Scala newbies used by Wiringbits",
    homepage := Some(
      url("https://github.com/wiringbits/wiringbits-scala-newbie-warts")
    ),
    licenses := List(
      "MIT" -> url("https://www.opensource.org/licenses/mit-license.html")
    ),
    developers := List(
      Developer(
        "AlexITC",
        "Alexis Hernandez",
        "alexis22229@gmail.com",
        url("https://github.com/AlexITC")
      )
    )
  )
)

lazy val commonSettings = Def.settings(
  scalaVersion := "3.3.9",
  organization := "net.wiringbits",
  libraryDependencies ++= Seq(
    "org.wartremover" % "wartremover" % wartremover.Wart.PluginVersion cross CrossVersion.full,
    "org.scalatest" %% "scalatest" % "3.2.20" % "test"
  )
)

lazy val catsEffectWarts = project
  .in(file("cats-effect-warts"))
  .settings(
    commonSettings,
    name := "cats-effect-warts",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.7.1"
    )
  )

lazy val coreWarts = project
  .in(file("core-warts"))
  .settings(
    commonSettings,
    name := "core-warts"
  )

lazy val layeringWarts = project
  .in(file("layering-warts"))
  .settings(
    commonSettings,
    name := "layering-warts",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.10"
    )
  )

// WartTestTraverser loads the wart under test by class name while the spec compiles, so wart fixtures carrying an
// inline config must be compiled before the specs that use them: they live in this module's main sources.
lazy val layeringWartsTests = project
  .in(file("layering-warts-tests"))
  .dependsOn(layeringWarts)
  .settings(
    commonSettings,
    name := "layering-warts-tests",
    publish := {},
    publishLocal := {},
    publish / skip := true
  )

lazy val root = (project in file("."))
  .aggregate(
    catsEffectWarts,
    coreWarts,
    layeringWarts,
    layeringWartsTests
  )
  .settings(
    name := "wiringbits-scala-warts",
    publish := {},
    publishLocal := {},
    publish / skip := true
  )
