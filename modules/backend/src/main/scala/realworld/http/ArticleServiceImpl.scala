package realworld.http

import cats.effect.*
import cats.syntax.all.*

import realworld.domain.article.ListArticleQuery
import realworld.service.Articles
import realworld.service.Auth
import realworld.spec.ArticleService
import realworld.spec.AuthHeader
import realworld.spec.Body
import realworld.spec.CreateArticleData
import realworld.spec.CreateArticleOutput
import realworld.spec.Description
import realworld.spec.GetArticleOutput
import realworld.spec.Limit
import realworld.spec.ListArticleOutput
import realworld.spec.ListFeedArticleOutput
import realworld.spec.Skip
import realworld.spec.Slug
import realworld.spec.TagName
import realworld.spec.Title
import realworld.spec.UpdateArticleOutput
import realworld.spec.Username
import org.typelevel.log4cats.Logger
import realworld.domain.article.ArticleError
import realworld.spec.NotFoundError

object ArticleServiceImpl:
  def make[F[_]: MonadCancelThrow: Logger](
      articles: Articles[F],
      auth: Auth[F]
  ): ArticleService[F] =
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
      ): F[ListFeedArticleOutput] =
        val result =
          for
            uid               <- auth.access(authHeader).map(_.id)
            withTotalArticles <- articles.listFeed(uid, Pagination(limit, skip))
          yield withTotalArticles
        result.map(it => ListFeedArticleOutput(it.total, it.entity.value))

      def getArticle(slug: Slug, authHeaderOpt: Option[AuthHeader]): F[GetArticleOutput] =
        val result =
          for
            uidOpt  <- authHeaderOpt.traverse(auth.access(_).map(_.id))
            article <- articles.getBySlug(uidOpt, slug)
          yield article

        result
          .map(GetArticleOutput(_))
          .onError(e => Logger[F].warn(e)(s"Failed to get article: $slug"))
          .recoverWith:
            case ArticleError.NotFound(slug) => NotFoundError().raise

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
