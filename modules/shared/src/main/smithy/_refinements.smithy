$version: "2.0"

namespace realworld.spec

use smithy4s.meta#refinement
use smithy4s.meta#unwrap
use smithy4s.meta#scalaImports

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
    targetType: "realworld.types.Title",
    providerImport: "realworld.types.providers.given",
)
structure TitleFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.Description",
    providerImport: "realworld.types.providers.given",
)
structure DescriptionFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.Body",
    providerImport: "realworld.types.providers.given",
)
structure BodyFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.TagName",
    providerImport: "realworld.types.providers.given",
)
structure TagNameFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.CommentBody",
    providerImport: "realworld.types.providers.given",
)
structure CommentBodyFormat{}

@trait(selector: "string")
@refinement(
    targetType: "realworld.types.ImageUrl",
    providerImport: "realworld.types.providers.given",
)
structure ImageUrlFormat{}

@trait(selector: "list")
@refinement(
    targetType: "realworld.types.NonEmptyList",
    providerImport: "realworld.types.providers.given",
    parameterised: true
)
structure nonEmptyListFormat{}