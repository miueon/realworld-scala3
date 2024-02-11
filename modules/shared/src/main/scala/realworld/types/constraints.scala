package realworld.types
// import io.github.iltotore.iron.*
// import io.github.iltotore.iron.cats.*
// import io.github.iltotore.iron.cats.given
// import io.github.iltotore.iron.cats.given_Order_:|
// import io.github.iltotore.iron.circe.given
// import io.github.iltotore.iron.ciris.*
// import io.github.iltotore.iron.constraint.all.*
// import io.github.iltotore.iron.constraint.any.DescribedAs
// import io.github.iltotore.iron.constraint.any.Not
// import io.github.iltotore.iron.constraint.numeric.Interval.*
// import io.github.iltotore.iron.constraint.string.Blank
// import alloy.common.EmailFormat
// import smithy4s.RefinementProvider
// import realworld.spec.NonEmptyString
// import smithy4s.Refinement

// type NonEmptyOrBlank = MinLength[1] & Not[Blank] DescribedAs
//   "Should not be empty or blank"
// opaque type NonEmptyStringR = String :| NonEmptyOrBlank
// object NonEmptyStringR
//     extends RefinedTypeOps[String, NonEmptyOrBlank, NonEmptyStringR]:
//   given RefinementProvider[NonEmptyString, String, NonEmptyStringR] =
//     Refinement.drivenBy[NonEmptyString](
//       (v: String) => v.refineEither,
//       _.value
//     )
