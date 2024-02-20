package realworld.http

import cats.effect.*
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import realworld.service.Auth
import realworld.service.Comments
import realworld.spec.AuthHeader
import realworld.spec.CommentId
import realworld.spec.CommentService
import realworld.spec.CreateCommentData
import realworld.spec.CreateCommentOutput
import realworld.spec.ListCommentsOutput
import realworld.spec.Slug
import realworld.domain.CommentError
import realworld.spec.NotFoundError

object CommentServiceImpl:
  def make[F[_]: MonadCancelThrow: Logger](
      comments: Comments[F],
      auth: Auth[F]
  ): CommentService[F] =
    new:
      def createComment(
          slug: Slug,
          comment: CreateCommentData,
          authHeader: AuthHeader
      ): F[CreateCommentOutput] =
        val result = for
          uid     <- auth.authUserId(authHeader)
          comment <- comments.create(slug, comment.body, uid)
        yield CreateCommentOutput(comment)
        result

      def deleteComment(slug: Slug, id: CommentId, authHeader: AuthHeader): F[Unit] =
        val result = for
          uid <- auth.authUserId(authHeader)
          _   <- comments.delete(slug, id, uid)
        yield ()
        result.recoverWith:
          case CommentError.CommentNotFound(id) => NotFoundError().raise

      def listComments(slug: Slug, authHeader: Option[AuthHeader]): F[ListCommentsOutput] =
        for
          uid      <- authHeader.traverse(auth.authUserId(_))
          comments <- comments.listByArticleId(uid, slug)
        yield ListCommentsOutput(comments.value)
end CommentServiceImpl
