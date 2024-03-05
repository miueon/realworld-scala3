package realworld.repo

import cats.Functor
import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import realworld.domain.follower.Follower
import realworld.domain.follower.Followers.followerId
import realworld.domain.user.UserId

trait FollowerRepo[F[_]: Functor]:
  def findFollower(followeeId: UserId, followerId: UserId): F[Option[Follower]] =
    listFollowers(List(followeeId), followerId).map(_.headOption)
  def listFollowers(followeeIds: List[UserId], followerId: UserId): F[List[Follower]]
  def deleteFollower(followeeId: UserId, followerId: UserId): F[Unit]
  def createFollower(followeeId: UserId, followerId: UserId): F[Follower]

object FollowerRepo:
  import ProfileSQL as p
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): FollowerRepo[F] =
    new:
      def listFollowers(followeeIds: List[UserId], followerId: UserId): F[List[Follower]] =
        NonEmptyList.fromList(followeeIds) match
          case Some(ids) => p.selectFollowers(ids, followerId).transact(xa)
          case None      => List.empty[Follower].pure[F]

      def deleteFollower(followeeId: UserId, followerId: UserId): F[Unit] =
        p.delete(followeeId, followerId).map(_ => ()).transact(xa)

      def createFollower(followeeId: UserId, followerId: UserId): F[Follower] =
        val follower = Follower(followeeId, followerId)
        p.insert()
          .run(follower)
          .map(_ => follower)
          .transact(xa)
end FollowerRepo

private object ProfileSQL:
  import realworld.domain.follower.Followers as f

  def selectFollowers(
      followeeIds: NonEmptyList[UserId],
      followerId: UserId
  ): ConnectionIO[List[Follower]] =
    fr"""
      SELECT ${f.rowCol} FROM $f
      WHERE ${f.userId in followeeIds} AND ${f.followerId === followerId}
    """.queryOf(f.rowCol).to[List]

  def delete(followeeId: UserId, followerId: UserId): ConnectionIO[Int] =
    fr"""
      DELETE FROM $f
      WHERE ${f.userId === followeeId} AND ${f.followerId === followerId}
    """.update.run

  def insert() =
    f.rowCol.insertOnConflictDoNothing0
end ProfileSQL
