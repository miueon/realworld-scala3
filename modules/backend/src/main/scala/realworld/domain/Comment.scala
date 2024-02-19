package realworld.domain

import realworld.domain.article.ArticleId
import realworld.spec.CreatedAt
import realworld.spec.UpdatedAt
import realworld.domain.user.UserId
import realworld.spec.CommentBody
import doobie.util.meta.Meta
import realworld.domain.types.Newtype
import doobie.TableDefinition
import doobie.Column
import doobie.WithSQLDefinition
import doobie.Composite

type CommentId = CommentId.Type
object CommentId extends Newtype[Int]

given Meta[CommentBody] = Meta[String].imap(CommentBody(_))(_.value)

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

  val columns = CommentSqlDef
  val rowCol  = WithId.sqlDef(using id, columns, this)
end Comments


