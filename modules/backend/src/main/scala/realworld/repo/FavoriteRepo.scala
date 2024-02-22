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
import cats.Functor

trait FavoriteRepo[F[_]: Functor]:
  def listFavorite(articleIds: List[ArticleId], userId: UserId): F[List[Favorite]]
  def findFavorite(articleId: ArticleId, userId: UserId): F[Option[Favorite]] =
    listFavorite(List(articleId), userId).map(_.headOption)
  def favoriteCountIdMap(articleIds: List[ArticleId]): F[Map[ArticleId, FavoritesCount]]
  def favoriteCount(articleId: ArticleId): F[FavoritesCount] =
    favoriteCountIdMap(List(articleId)).map(_.getOrElse(articleId, FavoritesCount(0)))
  def create(fav: Favorite): F[Favorite]
  def delete(fav: Favorite): F[Unit]

object FavoriteRepo:
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): FavoriteRepo[F] =
    new:
      def favoriteCountIdMap(
          articleIds: List[ArticleId]
      ): F[Map[ArticleId, FavoritesCount]] =
        NonEmptyList.fromList(articleIds.distinct) match
          case Some(ids) =>
            FavoriteSQL
              .selectFavArticleIdCount(ids)
              .transact(xa)
              .map(_.map(t => t._1 -> FavoritesCount(t._2)).toMap)
          case None => Map.empty[ArticleId, FavoritesCount].pure[F]

      def listFavorite(articleIds: List[ArticleId], userId: UserId): F[List[Favorite]] =
        NonEmptyList.fromList(articleIds.distinct) match
          case Some(ids) => FavoriteSQL.selectFavorites(ids, userId).transact(xa)
          case None      => List.empty[Favorite].pure[F]

      def create(fav: Favorite): F[Favorite] =
        FavoriteSQL.insert().run(fav).transact(xa).map(_ => fav)

      def delete(fav: Favorite): F[Unit] =
        FavoriteSQL.delete(fav.articleId, fav.userId).transact(xa).map(_ => ())

end FavoriteRepo

private object FavoriteSQL:
  import realworld.domain.Favorites as f

  def selectFavArticleIdCount(
      articleIds: NonEmptyList[ArticleId]
  ): ConnectionIO[List[(ArticleId, Int)]] =
    sql"""
    SELECT ${f.articleId}, COUNT(1) FROM $f
    WHERE ${f.articleId in articleIds}
    GROUP BY ${f.articleId}
    """
      .query[(ArticleId, Int)]
      .to[List]

  def selectFavorites(
      articleIds: NonEmptyList[ArticleId],
      userId: UserId
  ): ConnectionIO[List[Favorite]] =
    sql"""
    SELECT ${f.rowCol} FROM $f
    WHERE ${f.articleId in articleIds} AND ${f.userId === userId}
    """
      .queryOf(f.rowCol)
      .to[List]

  def insert() =
    f.rowCol.insertOnConflictDoNothing0

  def delete(articleId: ArticleId, uid: UserId) =
    sql"""
    DELETE FROM $f WHERE ${f.articleId === articleId} AND ${f.userId === uid}
    """.update.run
end FavoriteSQL
