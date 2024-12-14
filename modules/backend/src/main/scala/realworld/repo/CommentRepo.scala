package realworld.repo

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import realworld.domain.{Comment, CommentDBView}
import realworld.domain.article.ArticleId
import realworld.domain.user.UserId
import realworld.spec.CommentId

trait CommentRepo[F[_]]:
  def listCommentsByArticleId(articleId: ArticleId): F[List[CommentDBView]]
  def create(comment: Comment): F[CommentDBView]
  def delete(commentId: CommentId, articleId: ArticleId, authorId: UserId): F[Option[Unit]]

object CommentRepo:
  import CommentSQL as C
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): CommentRepo[F] =
    new:
      def listCommentsByArticleId(articleId: ArticleId): F[List[CommentDBView]] =
        C.selectCommentsByArticleId(articleId).transact(xa)
      def create(comment: Comment): F[CommentDBView] =
        val trx =
          for
            id   <- C.insert()(comment)
            view <- C.selectCommentById(id)
          yield view

        trx.transact(xa)
      def delete(commentId: CommentId, articleId: ArticleId, authorId: UserId): F[Option[Unit]] =
        C.delete(commentId, articleId, authorId).transact(xa)
end CommentRepo

private object CommentSQL:
  import realworld.domain.Comments
  import realworld.domain.given
  import realworld.domain.user.Users

  private val c = Comments as "c"
  private val u = Users as "u"
  object CommentViews
  extends WithSQLDefinition[CommentDBView](
    Composite(
      c.c(_.id).sqlDef,
      c.c(_.createdAt).sqlDef,
      c.c(_.updatedAt).sqlDef,
      c.c(_.body).sqlDef,
      c.c(_.authorId).sqlDef,
      u.c(_.username).sqlDef,
      u.c(_.bio).sqlDef,
      u.c(_.image).sqlDef
    )(CommentDBView.apply)(Tuple.fromProductTyped)
  )

  private val commentViewFr =
    fr"""
    SELECT ${CommentViews} FROM $c INNER JOIN $u ON ${c.c(_.authorId)} = ${u.c(_.id)}
    """

  def selectCommentsByArticleId(articleId: ArticleId) =
    val q =
      commentViewFr ++ fr"""
      WHERE ${c.c(_.articleId) === articleId} ORDER BY ${c.c(_.createdAt)} DESC
      """
    q.queryOf(CommentViews).to[List]

  def selectCommentById(id: CommentId) =
    val q = commentViewFr ++ fr"""
    WHERE ${c.c(_.id) === id}  
    """
    q.queryOf(CommentViews).unique

  def insert() =
    Comments.columns.insert.withUniqueGeneratedKeys[CommentId](Comments.id.rawName)

  def delete(commentId: CommentId, articleId: ArticleId, authorId: UserId) =
    val q =
      sql"""
      DELETE FROM ${Comments} WHERE ${Comments.id === commentId} AND ${Comments.articleId === articleId} AND ${Comments.authorId === authorId}
      """
    q.update.run.map(affectedToOption)
end CommentSQL
