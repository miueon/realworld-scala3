package realworld.domain

import realworld.domain.article.ArticleId
import realworld.domain.user.UserId
import doobie.TableDefinition
import doobie.Column
import doobie.WithSQLDefinition
import doobie.Composite

case class Favorite(
  articleId: ArticleId,
  userId: UserId
)

object Favorites extends TableDefinition("favorites_articles"):
  val articleId: Column[ArticleId] = Column("article_id")
  val userId: Column[UserId] = Column("user_id")

  object FavoriteSqlDef extends WithSQLDefinition[Favorite](
    Composite(
      articleId.sqlDef,
      userId.sqlDef
    )(Favorite.apply)(Tuple.fromProductTyped)
  )

  val rowCol = FavoriteSqlDef