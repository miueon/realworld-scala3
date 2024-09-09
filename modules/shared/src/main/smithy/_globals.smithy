$version: "2.0"

namespace realworld.spec

@error("client")
@httpError(400)
structure ValidationError {
    @required
    message: String
}

@error("client")
@httpError(401)
structure UnauthorizedError {
    message: String
}

@error("client")
@httpError(403)
structure ForbiddenError {}

@error("client")
@httpError(404)
structure NotFoundError {
    message: String
}

@error("client")
@httpError(422)
structure UnprocessableEntity {
    errors: ValidationErrors
}

map ValidationErrors {
    key: String
    value: StringList
}

list StringList {
    member: String
}

string AuthHeader

string ImageUrl

@range(min: 0)
integer Skip

@range(min: 1)
integer Limit

@range(min: 0)
integer Total

timestamp CreatedAt

timestamp UpdatedAt

// Mixin
@mixin
structure AuthHeaderMixin {
    @httpHeader("Authorization")
    @required
    auth: AuthHeader
}
