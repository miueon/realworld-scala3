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
use smithy4s.meta#unwrap
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
    input := with [PaginatedInputMixin, AuthHeaderOptMixin] {
        @httpQuery("tag")
        tag: TagName
        @httpQuery("author")
        author: Username
        @httpQuery("favorited")
        favorited: Username
    }
    output := with [ListArticleOutputMixin] {}
}

@readonly
@http(method: "GET", uri: "/api/articles/feed", code: 200)
operation ListFeedArticle {
    input := with [PaginatedInputMixin, AuthHeaderMixin] {}
    output := with [ListArticleOutputMixin] {}
    errors: [UnauthorizedError]
}

@readonly
@http(method: "GET", uri: "/api/article/{slug}", code: 200)
operation GetArticle {
    input := with [AuthHeaderOptMixin] {
        @required
        @httpLabel
        slug: Slug
    }
    output := {
        @required
        article: Article
    }
    errors: [NotFoundError]
}

@http(method: "POST", uri: "/api/articles", code: 200)
operation CreateArticle {
    input := with [AuthHeaderMixin] {
        @required
        article: CreateArticleData
    }
    output := {
        @required
        article: Article
    }
    errors: [UnauthorizedError, UnprocessableEntity]
}

@http(method: "PUT", uri: "/api/articles/{slug}", code: 200)
operation UpdateArticle {
    input := with [AuthHeaderMixin] {
        @required
        @httpLabel
        slug: Slug
        @required
        article: UpdateArticleData
    }
    output := {
        @required
        article: Article
    }
    errors: [
        UnauthorizedError
        UnprocessableEntity
        NotFoundError
    ]
}

@idempotent
@http(method: "DELETE", uri: "/api/articles/{slug}", code: 204)
operation DeleteArticle {
    input := with [AuthHeaderMixin] {
        @required
        @httpLabel
        slug: Slug
    }
    errors: [NotFoundError]
}

@http(method: "POST", uri: "/api/articles/{slug}/favorite", code: 200)
operation FavoriteArticle {
    input := with [AuthHeaderMixin] {
        @required
        @httpLabel
        slug: Slug
    }
    output := {
        @required
        article: Article
    }
    errors: [UnauthorizedError, NotFoundError]
}

@http(method: "DELETE", uri: "/api/articles/{slug}/favorite", code: 200)
operation UnfavoriteArticle {
    input := with [AuthHeaderMixin] {
        @required
        @httpLabel
        slug: Slug
    }
    output := {
        @required
        article: Article
    }
}

// MIXINS
@mixin
structure ListArticleOutputMixin {
    @required
    articlesCount: Total
    @required
    articles: ArticleList
}

// STRUCTURES
structure UpdateArticleData {
    title: Title
    description: Description
    body: Body
    tagList: TagList = []
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
@TagNameFormat
@unwrap
string TagName

string Slug

@TitleFormat
@unwrap
string Title

@DescriptionFormat
@unwrap
string Description

@BodyFormat
@unwrap
string Body

integer FavoritesCount

list TagList {
    member: TagName
}
