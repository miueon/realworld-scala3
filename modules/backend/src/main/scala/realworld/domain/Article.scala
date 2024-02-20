package realworld.domain.article

import realworld.domain.types.IdNewtype
import realworld.spec.Slug
import realworld.spec.Title
import realworld.spec.Description
import realworld.spec.Body
import realworld.spec.CreatedAt
import realworld.spec.UpdatedAt
import realworld.domain.user.UserId
import doobie.util.meta.Meta
import doobie.implicits.*
import doobie.implicits.javatime.JavaTimeInstantMeta
import java.time.Instant
import smithy4s.Timestamp
import doobie.TableDefinition
import doobie.Column
import doobie.WithSQLDefinition
import doobie.Composite
import realworld.spec.Username
import realworld.domain.given
import realworld.domain.*
import realworld.spec.TagName
import realworld.spec.Bio
import realworld.spec.ImageUrl
import scala.util.control.NoStackTrace
import doobie.TableDefinition.RowHelpers

type ArticleId = ArticleId.Type
object ArticleId extends IdNewtype

given Meta[Slug]        = Meta[String].imap(Slug(_))(_.value)
given Meta[Title]       = Meta[String].imap(Title(_))(_.value)
given Meta[Description] = Meta[String].imap(Description(_))(_.value)
given Meta[Body]        = Meta[String].imap(Body(_))(_.value)

case class Article(
    slug: Slug,
    title: Title,
    description: Description,
    body: Body,
    createdAt: CreatedAt,
    updatedAt: UpdatedAt,
    authorId: UserId
)

object Articles extends TableDefinition("articles"):
  val id: Column[ArticleId]            = Column("id")
  val slug: Column[Slug]               = Column("slug")
  val title: Column[Title]             = Column("title")
  val description: Column[Description] = Column("description")
  val body: Column[Body]               = Column("body")
  val createdAt: Column[CreatedAt]     = Column("created_at")
  val updatedAt: Column[UpdatedAt]     = Column("updated_at")
  val authorId: Column[UserId]         = Column("author_id")

  object ArticleSqlDef
      extends WithSQLDefinition[Article](
        Composite(
          (
            slug.sqlDef,
            title.sqlDef,
            description.sqlDef,
            body.sqlDef,
            createdAt.sqlDef,
            updatedAt.sqlDef,
            authorId.sqlDef
          )
        )(Article.apply)(Tuple.fromProductTyped)
      )
  val columns = ArticleSqlDef
  val rowCol  = WithId.sqlDef(using id, columns, this)
end Articles

// Error

enum ArticleError extends NoStackTrace:
  case NotFound(slug: Slug)

// API
case class ListArticleQuery(
    tag: Option[TagName],
    author: Option[Username],
    favorited: Option[Username]
)

case class ArticleView(
    id: ArticleId,
    slug: Slug,
    title: Title,
    description: Description,
    body: Body,
    createdAt: CreatedAt,
    updatedAt: UpdatedAt,
    authorId: UserId,
    authorName: Username,
    authorBio: Option[Bio],
    authorImage: Option[ImageUrl]
)
