package realworld.service

import realworld.domain.user.UserId
import realworld.domain.article.ListArticleQuery
import realworld.http.Pagination
import realworld.domain.WithTotal
import realworld.spec.ArticleList

trait Articles[F[_]]:
  def list(
      uid: Option[UserId],
      query: ListArticleQuery,
      pagination: Pagination
  ): F[WithTotal[ArticleList]]

object Articles:
  def make[F[_]](): Articles[F] =
    new:
      def list(
          uid: Option[UserId],
          query: ListArticleQuery,
          pagination: Pagination
      ): F[WithTotal[ArticleList]] = ???
