package realworld.http

import cats.Monad
import cats.MonadError
import cats.MonadThrow
import cats.data.OptionT
import cats.effect.kernel.Sync
import cats.syntax.all.*

import org.typelevel.log4cats.Logger
import realworld.domain.user.UserError
import realworld.service.Auth
import realworld.service.Profiles
import realworld.spec.AuthHeader
import realworld.spec.FollowUserOutput
import realworld.spec.ForbiddenError
import realworld.spec.GetProfileOutput
import realworld.spec.GetUserOutput
import realworld.spec.LoginUserInputData
import realworld.spec.LoginUserOutput
import realworld.spec.NotFoundError
import realworld.spec.RegisterUserData
import realworld.spec.RegisterUserOutput
import realworld.spec.UnfollowUserOutput
import realworld.spec.UnprocessableEntity
import realworld.spec.UpdateUserData
import realworld.spec.UpdateUserOutput
import realworld.spec.UserService
import realworld.spec.Username
import smithy4s.Smithy4sThrowable

object UserServiceImpl:
  def make[F[_]:  MonadThrow: Logger](
      auth: Auth[F],
      profiles: Profiles[F]
  ): UserService[F] =
    new:
      def getProfile(
          username: Username,
          authHeaderOpt: Option[AuthHeader]
      ): F[GetProfileOutput] =
        val result = for
          uidOpt <- authHeaderOpt.map(auth.authUserId).sequence
          profile         <- profiles.get(username, uidOpt)
        yield GetProfileOutput(profile)
        result
          .onError(e => Logger[F].warn(e)(s"Failed to get profile for user: $username"))
          .recoverWith:
            case UserError.ProfileNotFound() => NotFoundError().raise

      def unfollowUser(
          username: Username,
          authHeader: AuthHeader
      ): F[UnfollowUserOutput] =
        val result = for
          userSession <- auth.access(authHeader)
          profile     <- profiles.unfollow(username, userSession.id)
        yield UnfollowUserOutput(profile)
        result.recoverWith:
          case UserError.ProfileNotFound() => NotFoundError().raise

      def followUser(
          username: Username,
          authHeader: AuthHeader
      ): F[FollowUserOutput] =
        val result = for
          userSession <- auth.access(authHeader)
          profile     <- profiles.follow(username, userSession.id)
        yield FollowUserOutput(profile)
        result.recoverWith:
          case UserError.ProfileNotFound() => NotFoundError().raise

      def loginUser(user: LoginUserInputData): F[LoginUserOutput] =
        auth
          .login(user)
          .map(LoginUserOutput(_))
          .onError(e => Logger[F].warn(e)(s"Failed to login user: $user"))
          .recoverWith:
            case UserError.UserNotFound()           => NotFoundError().raise
            case UserError.UserPasswordNotMatched() => ForbiddenError().raise

      def getUser(authHeader: AuthHeader): F[GetUserOutput] =
        auth.access(authHeader).map(u => GetUserOutput(u.user))

      def updateUser(
          authHeader: AuthHeader,
          user: UpdateUserData
      ): F[UpdateUserOutput] =
        for
          u <- auth.access(authHeader)
          rs <-
            auth
              .update(u.id, user)
              .map(_.toUser(u.user.token))
              .map(UpdateUserOutput(_))
              .recoverWith:
                case UserError.UserNotFound() => NotFoundError().raise
                case UserError.EmailAlreadyExists() | UserError.UsernameAlreadyExists() =>
                  UnprocessableEntity().raise
        yield rs

      def registerUser(user: RegisterUserData): F[RegisterUserOutput] =
        auth
          .register(user)
          .map(RegisterUserOutput(_))
          .onError(e =>
            Logger[F]
              .warn(s"Failed to register user: $user, error: ${e.getMessage()}")
          )
          .recoverWith:
            case UserError.EmailAlreadyExists() =>
              UnprocessableEntity().raise

end UserServiceImpl
