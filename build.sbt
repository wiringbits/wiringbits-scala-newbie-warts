ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization := "net.wiringbits"
ThisBuild / scalaVersion := "2.13.8"

// For all Sonatype accounts created on or after February 2021
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

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
  scalaVersion := "2.13.8",
  organization := "net.wiringbits",
  libraryDependencies ++= Seq(
    "org.wartremover" % "wartremover" % wartremover.Wart.PluginVersion cross CrossVersion.full
  )
)

lazy val catsEffectWarts = project
  .in(file("cats-effect-warts"))
  .settings(
    commonSettings,
    name := "cats-effect-warts",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.3.5"
    )
  )

lazy val catsEffect2Warts = project
  .in(file("cats-effect2-warts"))
  .settings(
    commonSettings,
    name := "cats-effect2-warts",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "2.5.3"
    )
  )

lazy val root = (project in file("."))
  .aggregate(
    catsEffectWarts,
    catsEffect2Warts
  )
  .settings(
    name := "wiringbits-scala-warts",
    publish := {},
    publishLocal := {},
    publish / skip := true
  )
