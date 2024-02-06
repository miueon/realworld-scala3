$version: "2.0"

metadata suppressions = [{
    id: "HttpHeaderTrait"
    namespace: "realworld.spec"
    reason: ""
}]
namespace realworld.spec

use alloy#simpleRestJson
use alloy#uuidFormat
use alloy.common#emailFormat
use realworld.spec#UnauthorizedError
use realworld.spec#UnprocessableEntity
use realworld.spec#nonEmptyString
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
    errors: [UnauthorizedError]
}

@http(method: "POST", uri: "/api/users/login", code: 200)
@auth([])
operation LoginUser {
    input: LoginUserInput
    output: LoginUserOutput
    errors: [CredentialsError]
}

@idempotent
@http(method: "PUT", uri: "/api/users", code: 200)
@auth([])
operation RegisterUser {
    input: RegisterUserInput
    output: RegisterUserOutput
    errors: [ValidationError]
}

@readonly
@http(method: "GET", uri: "/api/user", code: 200)
operation GetUser {
    input: GetUserInput
    output: GetUserOutput
}

@idempotent
@http(method: "PUT", uri: "/api/user", code: 200)
operation UpdateUser {
    input: UpdateUserInput
    output: UpdateUserOutput
    errors: [CredentialsError, UnprocessableEntity]
}

@readonly
@http(method: "GET", uri: "/api/profiles/{username}", code: 200)
operation GetProfile {
    input: GetProfileInput
    output: GetProfileOutput
    errors: [NotFoundError]
}

@http(method: "POST", uri: "/api/profiles/{username}/follow", code: 200)
operation FollowUser {
    input: FollowUserInput
    output: FollowUserOutput
}

@idempotent
@http(method: "DELETE", uri: "/api/profiles/{username}/follow", code: 200)
operation UnfollowUser {
    input: UnfollowUserInput
    output: UnfollowUserOutput
}

// STRUCTURES
structure UnfollowUserInput {
    @required
    @httpLabel
    username: Username
    @required
    @httpHeader("Authorization")
    auth: AuthHeader
}

structure UnfollowUserOutput {
    @required
    profile: Profile
}

structure FollowUserInput {
    @required
    @httpLabel
    username: Username
    @required
    @httpHeader("Authorization")
    auth: AuthHeader
}

structure FollowUserOutput {
    @required
    profile: Profile
}

structure GetProfileInput {
    @required
    @httpLabel
    username: Username
    @httpHeader("Authorization")
    auth: AuthHeader
}

structure GetProfileOutput {
    @required
    profile: Profile
}

structure GetUserInput {
    @httpHeader("Authorization")
    @required
    auth: AuthHeader
}

structure GetUserOutput {
    @required
    user: User
}

structure RegisterUserInput {
    @required
    user: RegisterUserData
}

structure RegisterUserData {
    @required
    username: Username
    @required
    email: Email
    @required
    password: Password
}

structure RegisterUserOutput {
    @required
    user: User
}

structure LoginUserInput {
    @required
    user: LoginUserInputData
}

structure LoginUserInputData {
    @required
    email: Email
    @required
    password: Password
}

structure LoginUserOutput {
    @required
    user: User
}

structure UpdateUserInput {
    @httpHeader("Authorization")
    @required
    auth: AuthHeader
    @required
    user: UpdateUserData
}

structure UpdateUserData {
    email: Email
    username: Username
    password: Password
    bio: Bio
    image: ImageUrl
}

structure UpdateUserOutput {
    @required
    user: User
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
    following: Boolean
}

// @uuidFormat
// string UserId
@nonEmptyString
string Password

@emailFormat
string Email

string Token

@nonEmptyString
string Username

string Bio

@error("client")
@httpError(400)
structure CredentialsError {
    @required
    message: String
}
