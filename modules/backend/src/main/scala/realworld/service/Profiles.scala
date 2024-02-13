package realworld.service

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*

import realworld.domain.users.UserError
import realworld.domain.users.UserId
import realworld.repo.FollowerRepo
import realworld.repo.UserRepo
import realworld.spec.Profile
import realworld.spec.Username
import cats.data.EitherT

trait Profiles[F[_]]:
  def get(username: Username, uid: Option[UserId]): F[Profile]
  def unfollow(username: Username, uid: UserId): F[Profile]
  def follow(username: Username, uid: UserId): F[Profile]

object Profiles:
  def make[F[_]: MonadCancelThrow](
      userRepo: UserRepo[F],
      followerRepo: FollowerRepo[F]
  ): Profiles[F] =
    new:
      def get(username: Username, uid: Option[UserId]): F[Profile] =
        val profile = for
          user <- OptionT(userRepo.findByUsername(username))
          following: Boolean <- OptionT.liftF(
            uid
              .flatTraverse(
                followerRepo.findFollower(user.id, _)
              )
              .map(_.nonEmpty)
          )
        yield user.entity.toProfiile(following)
        profile.value.flatMap {
          case None        => UserError.ProfileNotFound().raiseError
          case Some(value) => value.pure
        }
      end get

      def unfollow(username: Username, uid: UserId): F[Profile] =
        val profile = for
          user <- EitherT.fromOptionF(
            userRepo.findByUsername(username),
            UserError.ProfileNotFound()
          )
          _ <- EitherT.cond(
            user.id =!= uid,
            (),
            UserError.UserUnfollowingHimself(user.entity.toProfiile(false))
          )
          _ <- EitherT.liftF(followerRepo.deleteFollower(user.id, uid))
        yield user.entity.toProfiile(false)

        profile.value.flatMap(
          _.fold(
            {
              case UserError.UserUnfollowingHimself(profile) => profile.pure
              case e                                         => e.raiseError[F, Profile]
            },
            _.pure[F]
          )
        )
      end unfollow

      def follow(username: Username, uid: UserId): F[Profile] =
        val profile = for
          user <- EitherT.fromOptionF(
            userRepo.findByUsername(username),
            UserError.ProfileNotFound()
          )
          _ <- EitherT.cond(
            user.id =!= uid,
            (),
            UserError.UserFollowingHimself(user.entity.toProfiile(false))
          )
          _ <- EitherT.liftF(
            followerRepo.createFollower(user.id, uid)
          )
        yield user.entity.toProfiile(true)
        profile.value.flatMap:
          _.fold(
            {
              case UserError.UserFollowingHimself(profile) => profile.pure
              case e                                       => e.raiseError
            },
            _.pure
          )
      end follow
end Profiles
