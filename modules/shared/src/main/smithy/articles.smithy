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
service ArticleService {
    version: "1.0.0"
    operations: [
        ListArticle
        ListFeedArticle
        GetArticle
        CreateArticle
        UpdateArticle
        DeleteArticle
        FavoriteArticle
        UnfavoriteArticle
    ]
    errors: [CredentialsError]
}

@readonly
@http(method: "GET", uri: "/api/articles", code: 200)
@auth([])
operation ListArticle {
    input: ListArticleInput
    output: ListArticleOutput
}

@readonly
@http(method: "GET", uri: "/api/articles/feed", code: 200)
operation ListFeedArticle {
    input: ListFeedArticleInput
    output: ListFeedArticleOutput
    errors: [UnauthorizedError]
}

@readonly
@http(method: "GET", uri: "/api/article/{slug}", code: 200)
operation GetArticle {
    input: GetArticleInput
    output: GetArticleOutput
    errors: [NotFoundError]
}

@http(method: "POST", uri: "/api/articles", code: 200)
operation CreateArticle {
    input: CreateArticleInput
    output: CreateArticleOutput
    errors: [UnauthorizedError, UnprocessableEntity]
}

@http(method: "PUT", uri: "/api/articles/{slug}", code: 200)
operation UpdateArticle {
    input: UpdateArticleInput
    output: UpdateArticleOutput
    errors: [
        UnauthorizedError
        UnprocessableEntity
        NotFoundError
    ]
}

@idempotent
@http(method: "DELETE", uri: "/api/articles/{slug}", code: 204)
operation DeleteArticle {
    input: DeleteArticleInput
    errors: [NotFoundError]
}

@http(method: "POST", uri: "/api/articles/{slug}/favorite", code: 200)
operation FavoriteArticle {
    input := {
        @required
        @httpLabel
        slug: Slug
        @required
        @httpHeader("Authorization")
        authHeader: AuthHeader
    }
    output := {
        @required
        article: Article
    }
    errors: [UnauthorizedError, NotFoundError]
}

@http(method: "DELETE", uri: "/api/articles/{slug}/favorite", code: 200)
operation UnfavoriteArticle {
    input := {
        @required
        @httpLabel
        slug: Slug
        @required
        @httpHeader("Authorization")
        authHeader: AuthHeader
    }
    output := {
        @required
        article: Article
    }
}

// STRUCTURES
structure DeleteArticleInput {
    @required
    @httpLabel
    slug: Slug
    @required
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure UpdateArticleInput {
    @required
    @httpLabel
    slug: Slug
    @required
    article: UpdateArticleData
    @required
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure UpdateArticleData {
    title: Title
    description: Description
    body: Body
}

structure UpdateArticleOutput {
    @required
    article: Article
}

structure CreateArticleInput {
    @required
    article: CreateArticleData
    @required
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure CreateArticleOutput {
    @required
    article: Article
}

structure CreateArticleData {
    @required
    title: Title
    @required
    description: Description
    @required
    body: Body
    tagList: TagList = []
}

structure GetArticleInput {
    @required
    @httpLabel
    slug: Slug
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure GetArticleOutput {
    @required
    article: Article
}

structure ListFeedArticleInput {
    @httpQuery("limit")
    limit: Limit = 10
    @httpQuery("skip")
    skip: Skip = 0
    @required
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure ListFeedArticleOutput {
    @required
    articlesCount: Total
    @required
    articles: ArticleList
}

structure ListArticleInput {
    @httpQuery("tag")
    tag: TagName
    @httpQuery("author")
    author: Username
    @httpQuery("favorited")
    favorited: Username
    @httpQuery("limit")
    limit: Limit = 10
    @httpQuery("skip")
    skip: Skip = 0
    @httpHeader("Authorization")
    authHeader: AuthHeader
}

structure ListArticleOutput {
    @required
    articlesCount: Total
    @required
    articles: ArticleList
}

list ArticleList {
    member: Article
}

structure Article {
    @required
    slug: Slug
    @required
    title: Title
    @required
    description: Description
    @required
    body: Body
    @required
    tagList: TagList = []
    @required
    createdAt: CreatedAt
    @required
    updatedAt: UpdatedAt
    @required
    favorited: Boolean = false
    @required
    favoritesCount: FavoritesCount = 0
    @required
    author: Profile
}

// NEW TYPE
@length(min: 1)
string TagName

@length(min: 1)
string Slug

@length(min: 1)
string Title

@length(min: 1)
string Description

@length(min: 1)
string Body

@range(min: 0)
integer FavoritesCount

list TagList {
    member: TagName
}
