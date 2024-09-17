package realworld.domain.article

import doobie.Column
import doobie.Composite
import doobie.TableDefinition
import doobie.WithSQLDefinition
import doobie.util.meta.Meta
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.doobie.given
import realworld.domain.*
import realworld.domain.given
import realworld.domain.types.IdNewtype
import realworld.domain.user.UserId
import realworld.spec.Bio
import realworld.spec.CreatedAt
import realworld.spec.Slug
import realworld.spec.UpdatedAt
import realworld.types.Body
import realworld.types.Description
import realworld.types.ImageUrl
import realworld.types.TagName
import realworld.types.Title
import realworld.types.Username

import scala.util.control.NoStackTrace
type ArticleId = ArticleId.Type
object ArticleId extends IdNewtype

given Meta[Slug]        = metaOf(Slug)
// given Meta[Title]       = Meta[String].refined[Not[Blank]]
// given Meta[Description] = Meta[String].refined[Not[Blank]]
// given Meta[Body]        = Meta[String].refined[Not[Blank]]

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
