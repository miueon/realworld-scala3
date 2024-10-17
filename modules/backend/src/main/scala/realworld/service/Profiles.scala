package realworld.service

import cats.data.{EitherT, OptionT}
import cats.effect.*
import cats.syntax.all.*
import realworld.domain.user.{UserError, UserId}
import realworld.repo.{FollowerRepo, UserRepo}
import realworld.spec.Profile
import realworld.types.Username

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
            UserError.UserUnfollowingHimself(profile = user.entity.toProfiile(false))
          )
          _ <- EitherT.liftF(followerRepo.deleteFollower(user.id, uid))
        yield user.entity.toProfiile(false)

        profile.value.flatMap(
          _.fold(
            {
              case UserError.UserUnfollowingHimself(_, profile) => profile.pure
              case e                                            => e.raiseError[F, Profile]
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
            UserError.UserFollowingHimself(profile = user.entity.toProfiile(false))
          )
          _ <- EitherT.liftF(
            followerRepo.createFollower(user.id, uid)
          )
        yield user.entity.toProfiile(true)
        profile.value.flatMap:
          _.fold(
            {
              case UserError.UserFollowingHimself(_, profile) => profile.pure
              case e                                          => e.raiseError
            },
            _.pure
          )
      end follow
end Profiles
