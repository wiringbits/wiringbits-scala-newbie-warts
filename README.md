# Wiringbits Scala Newbie Warts
Custom [WartRemover](https://www.wartremover.org/) warts from Wiringbits.

The collection intends to prevent common mistakes by developers that aren't very familiar with Scala.

For example, invoking `UnsafeRunSync` from cats-effect can lead to pretty bad application's behavior, with this library, you can prevent developers from invoking this method.


## What's included

Included warts:
- `net.wiringbits.warts.UnsafeRunSync`: Emits a warning when `UnsafeRunSync` from cats-effect is invoked (available for cats-effect 2 and 3).

## Usage

- Be sure to use [wartremover](https://www.wartremover.org) in your project.
- Add the dependencies you are interested in:

```scala
// choose the modules you are interested in
lazy val wiringbitsWarts = List(
  "cats-effect-warts", // cats-effect 3
  "cats-effect2-warts" // cats-effect 2
)
libraryDependencies ++= wiringbitsWarts.map { customWart =>
  "net.wiringbits" %% customWart % "0.2.0" // pick the latest version
}
wartremoverClasspaths ++= {
  (Compile / dependencyClasspath).value.files
    .find(item => wiringbitsWarts.exists(item.name.contains))
    .map(_.toURI.toString)
    .toList
}
```

- Enable the warts:

```scala
wartremoverWarnings += Wart.custom("net.wiringbits.warts.UnsafeRunSync")
```

It is recommended to turn these warnings into errors when building the project in the CI.
