package realworld
import scala.quoted.*

package object macroutil {
  inline def deriveInstance[T, Typeclass[_]]: Typeclass[T] = ${
    deriveInstanceImpl[T, Typeclass]
  }

  def deriveInstanceImpl[T: Type, Typeclass[_]: Type](using Quotes): Expr[Typeclass[T]] =
    import quotes.reflect.*

    // Get the type representation of T
    val tpe: TypeRepr = TypeRepr.of[T]

    // Get the symbol of T to check for opaque types
    val tpeSymbol: Symbol = tpe.typeSymbol

// Step 1: Check if T is an opaque type
    if tpeSymbol.flags.is(Flags.Opaque) then
      // Get the underlying type of the opaque type
      val underlyingType: TypeRepr =
        tpe.asInstanceOf[TypeRef].translucentSuperType

      // Step 2: Search for a Typeclass instance for the underlying type
      val typeclassForUnderlying: ImplicitSearchResult =
        Implicits.search(TypeRepr.of[Typeclass].appliedTo(underlyingType))

      // Handle the search result
      typeclassForUnderlying match
        case success: ImplicitSearchSuccess =>
          // Found an implicit instance, lift it to the opaque type
          success.tree.asExpr.asInstanceOf[Expr[Typeclass[T]]]
        case failure: ImplicitSearchFailure =>
          report.errorAndAbort(
            s"Could not find an instance of ${Type.show[Typeclass]}[${underlyingType.show}]"
          )
    else report.errorAndAbort(s"${tpe.show} is not an opaque type")
  end deriveInstanceImpl

}
