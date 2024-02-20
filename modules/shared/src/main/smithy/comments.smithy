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
// SERVICES

@simpleRestJson
@httpBearerAuth
service CommentService {
    version: "1.0.0"
    operations: [CreateComment, ListComments, DeleteComment]
    errors: [CredentialsError, NotFoundError]
}

@http(method: "POST", uri: "/api/articles/{slug}/comments", code: 200)
operation CreateComment {
    input: CreateCommentInput
    output: CreateCommentOutput
}

@readonly
@http(method: "GET", uri: "/api/articles/{slug}/comments", code: 200)
@auth([])
operation ListComments {
    input := {
        @required
        @httpLabel
        slug: Slug
        @httpHeader("Authorization")
        authHeader: AuthHeader
    }
    output := {
        @required
        comments: CommentViewList 
    }
}

@idempotent
@http(method: "DELETE", uri: "/api/articles/{slug}/comments/{id}", code: 204)
operation DeleteComment {
    input: DeleteCommentInput 
}

// STRUCTURES
structure DeleteCommentInput {
    @required
    @httpLabel
    slug: Slug
    @required
    @httpLabel
    id: CommentId
    @required
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure CreateCommentInput {
    @required
    @httpLabel
    slug: Slug
    @required
    comment: CreateCommentData
    @required
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure CreateCommentData {
    @required
    body: CommentBody
}

structure CreateCommentOutput {
    @required
    comment: CommentView
}

structure CommentView {
    @required
    id: CommentId
    @required
    createdAt: CreatedAt
    @required
    updatedAt: UpdatedAt
    @required
    body: CommentBody
    @required
    author: Profile
}

list  CommentViewList {
    member: CommentView
}

integer CommentId

@length(min: 1)
string CommentBody
