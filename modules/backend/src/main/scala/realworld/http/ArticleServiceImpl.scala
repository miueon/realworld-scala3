package realworld.http

import realworld.spec.ArticleService
import realworld.spec.AuthHeader
import realworld.spec.Limit
import realworld.spec.ListArticleOutput
import realworld.spec.Skip
import realworld.spec.Username
import realworld.domain.article.ListArticleQuery
import realworld.spec.TagName
import realworld.service.Articles
import realworld.service.Auth
import cats.syntax.all.*
import cats.effect.*
import realworld.spec.CreateArticleData
import realworld.spec.CreateArticleOutput
import realworld.spec.ListFeedArticleOutput
import realworld.spec.Body
import realworld.spec.Description
import realworld.spec.Slug
import realworld.spec.Title
import realworld.spec.UpdateArticleOutput
import realworld.spec.GetArticleOutput

object ArticleServiceImpl:
  def make[F[_]: MonadCancelThrow](articles: Articles[F], auth: Auth[F]): ArticleService[F] =
    new:
      def listArticle(
          limit: Limit,
          skip: Skip,
          tag: Option[TagName],
          author: Option[Username],
          favorited: Option[Username],
          authHeaderOpt: Option[AuthHeader]
      ): F[ListArticleOutput] =
        val query = ListArticleQuery(tag, author, favorited)
        val result =
          for
            uidOpt            <- authHeaderOpt.traverse(auth.access(_).map(_.id))
            withTotalArticles <- articles.list(uidOpt, query, Pagination(limit, skip))
          yield withTotalArticles

        result.map(it => ListArticleOutput(it.total, it.entity.value))
      end listArticle

      def listFeedArticle(
          authHeader: AuthHeader,
          limit: Limit,
          skip: Skip
      ): F[ListFeedArticleOutput] = ???

      def getArticle(slug: Slug, authHeaderOpt: Option[AuthHeader]): F[GetArticleOutput] = ???

      def createArticle(
          article: CreateArticleData,
          authHeader: AuthHeader
      ): F[CreateArticleOutput] = ???

      def updateArticle(
          slug: Slug,
          authHeader: AuthHeader,
          title: Option[Title],
          description: Option[Description],
          body: Option[Body]
      ): F[UpdateArticleOutput] = ???

      def deleteArticle(slug: Slug, authHeader: AuthHeader): F[Unit] = ???

end ArticleServiceImpl
