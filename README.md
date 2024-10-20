# Wiringbits Scala Newbie Warts
Custom [WartRemover](https://www.wartremover.org/) warts from Wiringbits.

The collection intends to prevent common mistakes by developers that aren't very familiar with Scala.

For example, invoking `UnsafeRunSync` from cats-effect can lead to pretty bad application's behavior, with this library, you can prevent developers from invoking this method.


## What's included


### Cats Effect 2 Warts

A previous version is required, check [v0.3.0](https://github.com/wiringbits/wiringbits-scala-newbie-warts/tree/v0.3.0)

### Cats Effect 3 Warts
- `net.wiringbits.warts.UnsafeRunSync`: Emits a warning when `unsafeRunSync` is invoked.
- `net.wiringbits.warts.UnsafeRunAndForget`: Emits a warning when `unsafeRunAndForget` is invoked.
- `net.wiringbits.warts.UnsafeRunAsync`: Emits a warning when `unsafeRunAsync` is invoked.
- `net.wiringbits.warts.UnsafeRunAsyncOutcome`: Emits a warning when `unsafeRunAsyncOutcome` is invoked.
- `net.wiringbits.warts.UnsafeRunCancelable`: Emits a warning when `unsafeRunCancelable` is invoked.
- `net.wiringbits.warts.UnsafeRunTimed`: Emits a warning when `unsafeRunTimed` is invoked.
- `net.wiringbits.warts.UnsafeToFuture`: Emits a warning when `unsafeToFuture` is invoked.
- `net.wiringbits.warts.UnsafeToFutureCancelable`: Emits a warning when `unsafeToFutureCancelable` is invoked.

## Usage

- Be sure to use [wartremover](https://www.wartremover.org) in your project.
- Add the dependencies you are interested in:

```scala
// choose the modules you are interested in
lazy val wiringbitsWarts = List(
  "cats-effect-warts", // cats-effect 3 only
)
libraryDependencies ++= wiringbitsWarts.map { customWart =>
  "net.wiringbits" %% customWart % "0.4.2" // pick the latest version
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

- Enable all unsafe warts:
```scala
wartremoverWarnings += Wart.custom("net.wiringbits.warts.Unsafe")
```

It is recommended to turn these warnings into errors when building the project in the CI.
