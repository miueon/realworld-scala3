package realworld.repo

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import realworld.domain.Favorite
import realworld.domain.article.ArticleId
import realworld.domain.user.UserId
import realworld.spec.FavoritesCount

trait FavoriteRepo[F[_]]:
  def findFavorites(articleIds: List[ArticleId], userId: UserId): F[List[Favorite]]
  def favoriteCountIdMap(articleIds: List[ArticleId]): F[Map[ArticleId, FavoritesCount]]

object FavoriteRepo:
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): FavoriteRepo[F] =
    new:
      def favoriteCountIdMap(
          articleIds: List[ArticleId]
      ): F[Map[ArticleId, FavoritesCount]] =
        NonEmptyList.fromList(articleIds.distinct) match
          case Some(ids) =>
            FavoriteSQL
              .favoriteArticleIdCountList(ids)
              .transact(xa)
              .map(_.map(t => t._1 -> FavoritesCount(t._2)).toMap)
          case None => Map.empty[ArticleId, FavoritesCount].pure[F]

      def findFavorites(articleIds: List[ArticleId], userId: UserId): F[List[Favorite]] =
        NonEmptyList.fromList(articleIds.distinct) match
          case Some(ids) => FavoriteSQL.findFavorites(ids, userId).transact(xa)
          case None      => List.empty[Favorite].pure[F]
end FavoriteRepo

private object FavoriteSQL:
  import realworld.domain.Favorites as f

  def favoriteArticleIdCountList(
      articleIds: NonEmptyList[ArticleId]
  ): ConnectionIO[List[(ArticleId, Int)]] =
    sql"""
    SELECT ${f.articleId} COUNT(1) FROM $f
    WHERE ${f.articleId in articleIds}
    GROUP BY ${f.articleId}
    """
      .query[(ArticleId, Int)]
      .to[List]

  def findFavorites(
      articleIds: NonEmptyList[ArticleId],
      userId: UserId
  ): ConnectionIO[List[Favorite]] =
    sql"""
    SELECT ${f.rowCol} FROM $f
    WHERE ${f.articleId in articleIds} AND ${f.userId === userId}
    """
      .queryOf(f.rowCol)
      .to[List]
end FavoriteSQL
