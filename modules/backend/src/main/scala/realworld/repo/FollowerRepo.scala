package realworld.repo

import cats.Functor
import cats.data.NonEmptyList
import cats.data.NonEmptyVector
import cats.effect.*
import cats.syntax.all.*

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import realworld.domain.follower.Follower
import realworld.domain.follower.Followers.followerId
import realworld.domain.user.UserId

trait FollowerRepo[F[_]]:
  def findFollower(followeeId: UserId, followerId: UserId)(using
      Fun: Functor[F]
  ): F[Option[Follower]] =
    Fun.map(findFollowers(List(followeeId), followerId))(_.headOption)
  def findFollowers(followeeIds: List[UserId], followerId: UserId): F[List[Follower]]
  def deleteFollower(followeeId: UserId, followerId: UserId): F[Unit]
  def createFollower(followeeId: UserId, followerId: UserId): F[Follower]

object FollowerRepo:
  import ProfileSQL as p
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): FollowerRepo[F] =
    new:
      def findFollowers(followeeIds: List[UserId], followerId: UserId): F[List[Follower]] =
        NonEmptyList.fromList(followeeIds) match
          case Some(ids) => p.findFollowers(ids, followerId).transact(xa)
          case None      => List.empty[Follower].pure[F]

      def deleteFollower(followeeId: UserId, followerId: UserId): F[Unit] =
        p.deleteFollower(followeeId, followerId).map(_ => ()).transact(xa)

      def createFollower(followeeId: UserId, followerId: UserId): F[Follower] =
        p.createFollower(followeeId, followerId)
          .map(_ => Follower(followeeId, followerId))
          .transact(xa)
end FollowerRepo

private object ProfileSQL:
  import realworld.domain.follower.Followers as f

  def findFollowers(
      followeeIds: NonEmptyList[UserId],
      followerId: UserId
  ): ConnectionIO[List[Follower]] =
    fr"""
      SELECT ${f.rowCol} FROM $f
      WHERE ${f.userId in followeeIds} AND ${f.followerId === followerId}
    """.queryOf(f.rowCol).to[List]

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
