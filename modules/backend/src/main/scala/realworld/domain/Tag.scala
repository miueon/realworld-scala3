package realworld.domain

import realworld.domain.article.ArticleId
import realworld.spec.TagName
import doobie.TableDefinition
import doobie.Column
import doobie.util.meta.Meta
import doobie.WithSQLDefinition
import doobie.Composite

case class Tag(articleId: ArticleId, tag: TagName)

given Meta[TagName] = Meta[String].imap(TagName(_))(_.value)

object Tags extends TableDefinition("tags_articles"):
  val articleId: Column[ArticleId] = Column("article_id")
  val tag: Column[TagName] = Column("tag")

  object TagSqlDef extends WithSQLDefinition[Tag](
    Composite(
      articleId.sqlDef,
      tag.sqlDef
    )(Tag.apply)(Tuple.fromProductTyped)
  )

  val rowCol = TagSqlDef