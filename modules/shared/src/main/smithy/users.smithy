$version: "2.0"

metadata suppressions = [{
    id: "HttpHeaderTrait"
    namespace: "realworld.spec"
    reason: ""
}]
namespace realworld.spec

use alloy#simpleRestJson
use alloy#uuidFormat
use realworld.spec#UnauthorizedError
use realworld.spec#UnprocessableEntity
use smithy4s.meta#validateNewtype
use smithy4s.meta#unwrap
use smithy4s.meta#scalaImports
// SERVICES

@simpleRestJson
@httpBearerAuth
service UserService {
    version: "1.0.0"
    operations: [
        LoginUser
        RegisterUser
        GetUser
        UpdateUser
        GetProfile
        FollowUser
        UnfollowUser
    ]
    errors: [UnauthorizedError, CredentialsError]
}

@http(method: "POST", uri: "/api/users/login", code: 200)
@auth([])
operation LoginUser {
    input := {
        @required
        user: LoginUserInputData
    }
    output := with [UserOutputMixin] {}
    errors: [NotFoundError, ForbiddenError]
}

@idempotent
@http(method: "PUT", uri: "/api/users", code: 200)
@auth([])
operation RegisterUser {
    input := {
        @required
        user: RegisterUserData
    }
    output := with [UserOutputMixin] {}
    errors: [ValidationError, UnprocessableEntity]
}

@readonly
@http(method: "GET", uri: "/api/user", code: 200)
operation GetUser {
    input := with [AuthHeaderMixin] {}
    output := with [UserOutputMixin] {}
}

@idempotent
@http(method: "PUT", uri: "/api/user", code: 200)
operation UpdateUser {
    input := with [AuthHeaderMixin] {
        @required
        user: UpdateUserData
    }
    output := with [UserOutputMixin] {}
    errors: [UnprocessableEntity]
}

@readonly
@http(method: "GET", uri: "/api/profiles/{username}", code: 200)
@auth([])
operation GetProfile {
    input := with [UsernameLabelMixin] {
        @httpHeader("Authorization")
        auth: AuthHeader
    }
    output := with [ProfileMixin] {}
    errors: [NotFoundError]
}

@http(method: "POST", uri: "/api/profiles/{username}/follow", code: 200)
operation FollowUser {
    input := with [UsernameLabelMixin, AuthHeaderMixin] {}
    output := with [ProfileMixin] {}
}

@idempotent
@http(method: "DELETE", uri: "/api/profiles/{username}/follow", code: 200)
operation UnfollowUser {
    input := with [UsernameLabelMixin, AuthHeaderMixin] {}
    output := with [ProfileMixin] {}
}

// Mixins
@mixin
structure UserOutputMixin {
    @required
    user: User
}

@mixin
structure ProfileMixin {
    @required
    profile: Profile
}

@mixin
structure UsernameLabelMixin {
    @required
    @httpLabel
    username: Username
}

// STRUCTURES
structure RegisterUserData {
    @required
    username: Username
    @required
    email: Email
    @required
    password: Password
}

structure LoginUserInputData {
    @required
    email: Email
    @required
    password: Password
}

structure UpdateUserData {
    email: Email
    username: Username
    password: Password
    bio: Bio
    image: ImageUrl
}

structure User {
    @required
    email: Email
    token: Token
    @required
    username: Username
    bio: Bio
    image: ImageUrl
}

structure Profile {
    @required
    username: Username
    bio: Bio
    image: ImageUrl
    @required
    following: Boolean
}

@PasswordFormat
@unwrap
string Password

@EmailFormat
@unwrap
string Email

string Token

@UsernameFormat
@unwrap
string Username

string Bio

@error("client")
@httpError(400)
structure CredentialsError {
    @required
    message: String
}
