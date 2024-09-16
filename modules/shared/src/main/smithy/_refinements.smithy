$version: "2.0"

namespace realworld.spec

use smithy4s.meta#refinement
use smithy4s.meta#unwrap
use smithy4s.meta#scalaImports

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.NonEmptyString",
    providerImport: "realworld.types.providers.given",
)
structure nonEmptyStringFormat {}


@trait(selector: "string")
@refinement(
    targetType: "realworld.types.Username",
    providerImport: "realworld.types.providers.given",
)
structure UsernameFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.Password",
    providerImport: "realworld.types.providers.given",
)
structure PasswordFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.Email",
    providerImport: "realworld.types.providers.given",
)
structure EmailFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.Password",
    providerImport: "realworld.types.providers.given",
)
structure PasswordFormat{}