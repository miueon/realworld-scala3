package realworld.http

import realworld.spec.ArticleService
import realworld.spec.AuthHeader
import realworld.spec.Limit
import realworld.spec.ListArticleOutput
import realworld.spec.Skip
import realworld.spec.Username
import realworld.domain.article.ListArticleQuery
import realworld.spec.TagName

object ArticleServiceImpl:
  def make[F[_]](): ArticleService[F] =
    new:
      def listArticle(
          tag: Option[TagName],
          author: Option[Username],
          favorited: Option[Username],
          limit: Option[Limit],
          skip: Option[Skip],
          authHeader: Option[AuthHeader]
      ): F[ListArticleOutput] =
        val query = ListArticleQuery(tag, author, favorited)
        
        ???
