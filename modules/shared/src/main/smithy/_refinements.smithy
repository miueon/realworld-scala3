$version: "2.0"

namespace realworld.spec
use smithy4s.meta#refinement

@trait(selector: "string")
structure nonEmptyString{}

apply realworld.spec#nonEmptyString @refinement(
  targetType: "realworld.types.NonEmptyStringR"
)
