package realworld.service

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*
import realworld.domain.article.{ArticleError, ArticleId}
import realworld.domain.follower.Follower
import realworld.domain.user.UserId
import realworld.domain.{Comment, CommentDBView}
import realworld.effects.Time
import realworld.repo.{ArticleRepo, CommentRepo, FollowerRepo}
import realworld.spec.{
  CommentBody,
  CommentId,
  CommentView,
  CommentViewList,
  CreatedAt,
  Profile,
  Slug,
  UpdatedAt
}

import smithy4s.Timestamp

trait Comments[F[_]]:
  def create(slug: Slug, body: CommentBody, uid: UserId): F[CommentView]
  def delete(slug: Slug, id: CommentId, uid: UserId): F[Unit]
  def listByArticleId(uidOpt: Option[UserId], slug: Slug): F[CommentViewList]

object Comments:
  def make[F[_]: MonadCancelThrow: Time](
      commentRepo: CommentRepo[F],
      articleRepo: ArticleRepo[F],
      followerRepo: FollowerRepo[F]
  ): Comments[F] =
    new:
      def create(slug: Slug, body: CommentBody, uid: UserId): F[CommentView] =
        def mkComment(articleId: ArticleId, nowTime: Timestamp): Comment =
          Comment(
            articleId,
            CreatedAt(nowTime),
            UpdatedAt(nowTime),
            uid,
            body
          )

        val result =
          for
            articleView <- OptionT(articleRepo.findBySlug(slug))
            timestamp <- OptionT.liftF(Time[F].timestamp)
            commentDBView <- OptionT.liftF(
              commentRepo.create(mkComment(articleView.id, timestamp))
            )
          yield commentDBView.toCommentView(false)
        result.value.flatMap:
          case None              => ArticleError.NotFound(slug).raiseError
          case Some(commentView) => commentView.pure
      end create
      def delete(slug: Slug, id: CommentId, uid: UserId): F[Unit] =
        val result = for
          articleId <- OptionT(articleRepo.findBySlug(slug).map(_.map(_.id)))
          _         <- OptionT.liftF(commentRepo.delete(id, articleId, uid))
        yield ()
        result.value.flatMap:
          case None     => ArticleError.NotFound(slug).raiseError
          case Some(()) => ().pure
      def listByArticleId(uidOpt: Option[UserId], slug: Slug): F[CommentViewList] =
        val comments = for
          articleView <- OptionT(articleRepo.findBySlug(slug))
          comments    <- OptionT.liftF(commentRepo.listCommentsByArticleId(articleView.id))
          authorIds = comments.map(_.authorId).toList
          followers <- OptionT.liftF(
            uidOpt.traverse(followerRepo.listFollowers(authorIds, _)).map(_.getOrElse(List.empty))
          )
        yield mkComments(comments, followers)
        comments.value.flatMap:
          case None        => ArticleError.NotFound(slug).raiseError
          case Some(value) => CommentViewList(value).pure

      def mkComments(comments: List[CommentDBView], followers: List[Follower]) =
        // val authorFollowerMap = followers.groupBy(_.userId)

        comments.map(c => c.toCommentView(followers.contains(c.authorId)))

      extension (c: CommentDBView)
        def toCommentView(following: Boolean) =
          CommentView(
            c.id,
            c.createdAt,
            c.updatedAt,
            c.body,
            Profile(c.username, following, c.bio, c.image)
          )
end Comments
