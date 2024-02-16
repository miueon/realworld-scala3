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
    operations: [ListArticle]
}

@readonly
@http(method: "GET", uri: "/api/articles", code: 200)
@auth([])
operation ListArticle {
    input: ListArticleInput
    output: ListArticleOutput
}

// STRUCTURES
structure ListArticleInput {
    @httpQuery("tag")
    tag: TagName
    @httpQuery("author")
    author: Username
    @httpQuery("favorited")
    favorited: Username
    @httpQuery("limit")
    limit: Limit
    @httpQuery("skip")
    skip: Skip
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

structure Article{
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

string Slug
string Title
string Description
string Body

@range(min: 0)
integer FavoritesCount

list TagList {
  member: TagName
}
