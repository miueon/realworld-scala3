package realworld.http

import cats.effect.*

import org.typelevel.log4cats.Logger
import realworld.spec.AuthHeader
import realworld.spec.CommentId
import realworld.spec.CommentService
import realworld.spec.CreateCommentData
import realworld.spec.CreateCommentOutput
import realworld.spec.ListCommentsOutput
import realworld.spec.Slug

object CommentServiceImpl:
  def make[F[_]: MonadCancelThrow: Logger](): CommentService[F] =
    new:
      def createComment(
          slug: Slug,
          comment: CreateCommentData,
          authHeader: AuthHeader
      ): F[CreateCommentOutput] = ???

      def deleteComment(slug: Slug, id: CommentId, authHeader: AuthHeader): F[Unit] = ???

      def listComments(slug: Slug, authHeader: Option[AuthHeader]): F[ListCommentsOutput] = ???
