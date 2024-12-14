package realworld.service

import cats.data.{EitherT, OptionT}
import cats.effect.*
import cats.syntax.all.*
import io.github.arainko.ducktape.*
import realworld.domain.user.{DBUser, UserError, UserId}
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
        val profile =
          for
            user <- OptionT(userRepo.findByUsername(username))
            following: Boolean <- OptionT.liftF(
              uid
                .flatTraverse(
                  followerRepo.findFollower(user.id, _)
                )
                .map(_.nonEmpty)
            )
          yield dbUserToProfile(following)(user.entity)
        profile.value.flatMap {
          case None        => UserError.ProfileNotFound().raiseError
          case Some(value) => value.pure
        }
      end get

      def unfollow(username: Username, uid: UserId): F[Profile] =
        val profile =
          for
            user <- EitherT.fromOptionF(
              userRepo.findByUsername(username),
              UserError.ProfileNotFound()
            )
            _ <- EitherT.cond(
              user.id =!= uid, (),
              UserError.UserUnfollowingHimself(profile = dbUserToProfile(false)(user.entity))
            )
            _ <- EitherT.liftF(followerRepo.deleteFollower(user.id, uid))
          yield dbUserToProfile(false)(user.entity)

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
        val profile =
          for
            user <- EitherT.fromOptionF(
              userRepo.findByUsername(username),
              UserError.ProfileNotFound()
            )
            _ <- EitherT.cond(
              user.id =!= uid, (),
              UserError.UserFollowingHimself(profile = dbUserToProfile(false)(user.entity))
            )
            _ <- EitherT.liftF(
              followerRepo.createFollower(user.id, uid)
            )
          yield dbUserToProfile(true)(user.entity)
        profile.value.flatMap:
          _.fold(
            {
              case UserError.UserFollowingHimself(_, profile) => profile.pure
              case e                                          => e.raiseError
            },
            _.pure
          )
      end follow

  private def dbUserToProfile(following: Boolean)(user: DBUser): Profile =
    user.into[Profile].transform(
      Field.const(_.following, following)
    )
end Profiles
