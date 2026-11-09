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

### Core Warts
- `net.wiringbits.warts.NamedArguments`: Emits a warning when a call passes positional arguments for two or more parameters that share the same type.

### Layering Warts
- `net.wiringbits.warts.Layering`: Enforces a package layering, each layer may only reference the layers it declares. See [Layering](#layering).

## Usage

- Be sure to use [wartremover](https://www.wartremover.org) in your project.
- Add the modules you are interested in, `wartremoverDependencies` puts them on the wart classloader only, they stay off your application classpath:

```scala
wartremoverDependencies ++= List(
  "cats-effect-warts", // cats-effect 3 only
  "core-warts", // plain Scala, no extra dependencies
  "layering-warts" // package layering, see below
).map { customWart =>
  "net.wiringbits" %% customWart % "<latest>" // see https://github.com/wiringbits/wiringbits-scala-newbie-warts/releases
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

## Layering

The `Layering` wart enforces a directed graph between your packages: code in a layer may only reference the layers listed in its `calls`. For example, controllers may call actions but actions must never call controllers.

Each module declares its layers in a `.wartremover-layering.conf` at its base directory, like `server/.wartremover-layering.conf`. The wart uses the nearest file found in the compiled source's directory or its parents. A missing or invalid file is reported as a compiler error.

```hocon
# the packages this module owns, references to code outside them are not checked
roots = ["com.example", "controllers"]

# the longest matching package prefix decides the layer, so com.example.repositories.daos belongs to daos
layers = [
  { name = controllers,  packages = ["controllers"],                      calls = [actions, shared] }
  { name = actions,      packages = ["com.example.actions"],              calls = [repositories, shared] }
  { name = repositories, packages = ["com.example.repositories"],         calls = [daos, shared] }
  { name = daos,         packages = ["com.example.repositories.daos"],    calls = [daos, shared] }
  { name = shared,       packages = ["com.example.models"],               calls = [shared] }
]

# a class may reference a class of its own layer defined in the same file, defaults to true
allowSameFileSiblings = true
```

- A reference to a layer missing from `calls` is flagged, a layer only calls its own classes when it lists itself.
- Every package under `roots` taking part in a reference, as caller or callee, must belong to a layer.
- Type only references, base types, package objects and top level definitions are not checked.

Enable it with:

```scala
wartremoverErrors += Wart.custom("net.wiringbits.warts.Layering")
```

Suppress it with `@SuppressWarnings(Array("net.wiringbits.warts.Layering"))`.
