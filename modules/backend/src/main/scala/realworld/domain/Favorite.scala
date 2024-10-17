package realworld.domain

import doobie.TableDefinition.RowHelpers
import doobie.{Column, Composite, TableDefinition, WithSQLDefinition}
import realworld.domain.article.ArticleId
import realworld.domain.user.UserId

case class Favorite(
    articleId: ArticleId,
    userId: UserId
)

object Favorites extends TableDefinition("favorites_articles"):
  val articleId: Column[ArticleId] = Column("article_id")
  val userId: Column[UserId]       = Column("user_id")

  object FavoriteSqlDef
      extends WithSQLDefinition[Favorite](
        Composite(
          articleId.sqlDef,
          userId.sqlDef
        )(Favorite.apply)(Tuple.fromProductTyped)
      )
      with RowHelpers[Favorite](this)

  val rowCol = FavoriteSqlDef
end Favorites
