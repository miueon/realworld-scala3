package realworld.domain

import doobie.Column
import doobie.Composite
import doobie.TableDefinition
import doobie.TableDefinition.RowHelpers
import doobie.WithSQLDefinition
import doobie.util.meta.Meta
import realworld.domain.article.ArticleId
import realworld.domain.user.UserId
import realworld.spec.Bio
import realworld.spec.CommentBody
import realworld.spec.CommentId
import realworld.spec.CreatedAt
import realworld.spec.UpdatedAt
import realworld.types.ImageUrl
import realworld.types.Username

import scala.util.control.NoStackTrace

given Meta[CommentBody] = metaOf(CommentBody)

case class Comment(
    articleId: ArticleId,
    createdAt: CreatedAt,
    updatedAt: UpdatedAt,
    authorId: UserId,
    body: CommentBody
)

object Comments extends TableDefinition("comments"):
  val id: Column[CommentId]        = Column("id")
  val articleId: Column[ArticleId] = Column("article_id")
  val createdAt: Column[CreatedAt] = Column("created_at")
  val updatedAt: Column[UpdatedAt] = Column("updated_at")
  val authorId: Column[UserId]     = Column("author_id")
  val body: Column[CommentBody]    = Column("body")

  object CommentSqlDef
      extends WithSQLDefinition[Comment](
        Composite(
          articleId.sqlDef,
          createdAt.sqlDef,
          updatedAt.sqlDef,
          authorId.sqlDef,
          body.sqlDef
        )(Comment.apply)(Tuple.fromProductTyped)
      )
      with RowHelpers[Comment](this)

  val columns = CommentSqlDef
  val rowCol  = WithId.sqlDef(using id, columns, this)
end Comments

// API

enum CommentError extends NoStackTrace:
  case CommentNotFound(id: CommentId)

case class CommentDBView(
    id: CommentId,
    createdAt: CreatedAt,
    updatedAt: UpdatedAt,
    body: CommentBody,
    authorId: UserId,
    username: Username,
    bio: Option[Bio],
    image: Option[ImageUrl]
)
