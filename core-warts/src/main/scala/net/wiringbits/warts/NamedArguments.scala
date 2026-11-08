package net.wiringbits.warts

import org.wartremover.{WartTraverser, WartUniverse}

/** Warns at a call site when the invoked method, function or constructor declares two or more parameters sharing the
  * same type (anywhere in a single parameter list, not only adjacent ones) and the arguments for those same typed
  * parameters are passed positionally instead of by name.
  *
  * Two positional arguments of the same type can be swapped without a compile error, silently passing the wrong values.
  * For example `def merge(survivorId: UUID, loserId: UUID, userId: UUID)` called as `merge(a, b, c)` hides a possible
  * swap, while `merge(survivorId = a, loserId = b, userId = c)` turns a swap into a visible diff.
  *
  * Naming the same typed arguments is enough to satisfy the wart. Distinct typed arguments may still be positional.
  * Using named arguments is always acceptable, the wart only flags the absence of names where same typed positional
  * arguments appear.
  *
  * The wart does not flag: calls where every parameter has a distinct type, callees with fewer than two parameters of
  * any one type, single argument calls, operator/symbolic calls, varargs and calls whose owner is the Scala or Java
  * standard library (which also excludes applying a function value, whose owner is scala.FunctionN, and ordinary
  * combinators such as map, zip and fold where naming is not idiomatic).
  *
  * Known limitations, where the wart deliberately stays silent to avoid false positives: calls that rely on default
  * argument values (the compiler fills omitted arguments positionally with references to synthetic default getters, so
  * such calls are skipped). Calls inside compiler generated methods, such as the apply and copy of a case class with
  * same typed fields, are skipped too, while user written calls to those methods are still checked. Overloads are
  * handled correctly because the resolved method type is inspected. Each parameter list of a curried or multi parameter
  * list method is checked independently.
  */
object NamedArguments extends WartTraverser {
  private[warts] val message =
    """Two or more arguments of the same type are passed positionally.
      |Pass them by name so an accidental swap becomes a visible diff, for example
      |merge(survivorId = a, loserId = b) instead of merge(a, b).
      |If you know what you are doing, suppress the warning by annotating your code with:
      |@SuppressWarnings(Array("net.wiringbits.warts.NamedArguments"))
      |""".stripMargin

  def apply(u: WartUniverse): u.Traverser = {
    new u.Traverser(this) {
      import q.reflect.*

      private def isRepeated(tpe: TypeRepr): Boolean = tpe match {
        case AppliedType(t, _) => t.typeSymbol == defn.RepeatedParamClass
        case _ => false
      }

      // A parameter list has a risky arrangement when some type appears at two or more positions.
      private def sameTypedIndices(paramTypes: List[TypeRepr]): Set[Int] = {
        val indexed = paramTypes.zipWithIndex
        indexed.collect {
          case (tpe, i) if indexed.exists { case (other, j) => j != i && tpe =:= other } => i
        }.toSet
      }

      // The compiler normalizes arguments into declaration order, keeping explicit names as NamedArg nodes, so an
      // argument is positional exactly when it is not a NamedArg.
      private def isPositional(arg: Term): Boolean = arg match {
        case NamedArg(_, _) => false
        case _ => true
      }

      // Omitted arguments filled from default values show up positionally as references to synthetic getters.
      private def usesDefaultArguments(args: List[Term]): Boolean =
        args.exists(_.symbol.name.contains("$default$"))

      private def isStandardLibraryOwner(sym: Symbol): Boolean = {
        val owner = sym.maybeOwner
        if (owner.isNoSymbol) false
        else {
          val name = owner.fullName
          name == "scala" || name == "java" || name.startsWith("scala.") || name.startsWith("java.") ||
          name.startsWith("javax.")
        }
      }

      private def isSymbolicOrOperator(name: String): Boolean =
        name != "<init>" && name.headOption.exists(c => !(c.isLetter || c == '_'))

      // Calls inside compiler generated methods, such as the apply and copy of a case class, cannot be fixed by the
      // user. Anonymous functions and vals are synthetic too but hold user code, so the nearest enclosing method that
      // is not an anonymous function decides. Default getters are synthetic but their bodies are user written.
      private def isInsideSyntheticMethod(owner: Symbol): Boolean = {
        @annotation.tailrec
        def loop(sym: Symbol): Boolean =
          if (sym.isNoSymbol || sym.isClassDef) false
          else if (sym.isDefDef && !sym.isAnonymousFunction)
            sym.flags.is(Flags.Synthetic) && !sym.name.contains("$default$")
          else loop(sym.maybeOwner)

        loop(owner)
      }

      private def shouldWarn(fun: Term, args: List[Term]): Boolean = {
        val sym = fun.symbol
        val name = sym.name

        if (name.isEmpty || isSymbolicOrOperator(name)) false
        else if (isStandardLibraryOwner(sym)) false
        else if (usesDefaultArguments(args)) false
        else
          fun.tpe.widen match {
            case mt: MethodType =>
              val paramTypes = mt.paramTypes
              if (paramTypes.sizeIs < 2 || paramTypes.exists(isRepeated)) false
              else if (args.sizeIs != paramTypes.size) false
              else {
                val risky = sameTypedIndices(paramTypes)
                risky.exists(i => isPositional(args(i)))
              }
            case _ => false
          }
      }

      override def traverseTree(tree: Tree)(owner: Symbol): Unit = {
        tree match {
          case _ if hasWartAnnotation(tree) => ()
          case Apply(fun, args) =>
            if (!isInsideSyntheticMethod(owner) && shouldWarn(fun, args)) error(tree.pos, message)
            super.traverseTree(tree)(owner)
          case _ =>
            super.traverseTree(tree)(owner)
        }
      }
    }
  }
}
