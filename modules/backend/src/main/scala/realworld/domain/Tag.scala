package realworld.domain

import doobie.Column
import doobie.Composite
import doobie.TableDefinition
import doobie.TableDefinition.RowHelpers
import doobie.WithSQLDefinition
import doobie.util.meta.Meta
import io.github.iltotore.iron.constraint.all.*
import realworld.domain.article.ArticleId
import realworld.types.TagName
case class Tag(articleId: ArticleId, tag: TagName)

// given Meta[TagName] = Meta[String].refined[Not[Blank]]

object Tags extends TableDefinition("tags_articles"):
  val articleId: Column[ArticleId] = Column("article_id")
  val tag: Column[TagName]         = Column("tag")

  object TagSqlDef
      extends WithSQLDefinition[Tag](
        Composite(
          articleId.sqlDef,
          tag.sqlDef
        )(Tag.apply)(Tuple.fromProductTyped)
      )
      with RowHelpers[Tag](this)

  val rowCol = TagSqlDef
end Tags
