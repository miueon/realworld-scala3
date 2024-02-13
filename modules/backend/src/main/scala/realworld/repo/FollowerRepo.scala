package realworld.repo

import cats.effect.*

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import realworld.domain.follower.Follower
import realworld.domain.users.UserId
import cats.data.NonEmptyVector

trait FollowerRepo[F[_]]:
  def findFollower(followeeId: UserId, followerId: UserId): F[Option[Follower]]
  def deleteFollower(followeeId: UserId, followerId: UserId): F[Unit]
  def createFollower(followeeId: UserId, followerId: UserId): F[Follower]

object FollowerRepo:
  import ProfileSQL as p
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): FollowerRepo[F] =
    new:
      def findFollower(
          followeeId: UserId,
          followerId: UserId
      ): F[Option[Follower]] =
        p.findFollower(followeeId, followerId).transact(xa)

      def deleteFollower(followeeId: UserId, followerId: UserId): F[Unit] =
        p.deleteFollower(followeeId, followerId).map(_ => ()).transact(xa)

      def createFollower(followeeId: UserId, followerId: UserId): F[Follower] =
        p.createFollower(followeeId, followerId)
          .map(_ => Follower(followeeId, followerId))
          .transact(xa)
end FollowerRepo

private object ProfileSQL:
  import realworld.domain.follower.Follower as f

  def findFollower(
      followeeId: UserId,
      followerId: UserId
  ): ConnectionIO[Option[Follower]] =
    fr"""
      SELECT ${f.rowCol} FROM $f
      WHERE ${f.userId === followeeId} AND ${f.followerId === followerId}
    """.queryOf(f.rowCol).option

  def deleteFollower(followeeId: UserId, followerId: UserId): ConnectionIO[Int] =
    fr"""
      DELETE FROM $f
      WHERE ${f.userId === followeeId} AND ${f.followerId === followerId}
    """.update.run

  def createFollower(followeeId: UserId, followerId: UserId): ConnectionIO[Int] =
    insertInto(
      f,
      NonEmptyVector.of(
        f.userId --> followeeId,
        f.followerId --> followerId
      )
    ).update.run
end ProfileSQL
