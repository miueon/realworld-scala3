package realworld.http

import cats.Monad
import cats.MonadError
import cats.MonadThrow
import cats.effect.kernel.Sync
import cats.syntax.all.*

import org.typelevel.log4cats.Logger
import realworld.domain.users.UserError
import realworld.service.Auth
import realworld.spec.AuthHeader
import realworld.spec.FollowUserOutput
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
import realworld.validation.validateUpdateUserBody
import realworld.validation.validateUserName
import realworld.validation.validateUserPassword
import smithy4s.Smithy4sThrowable
import realworld.spec.ForbiddenError

object UserServiceImpl:
  def make[F[_]: Sync: MonadThrow: Logger](
      auth: Auth[F]
  ): UserService[F] =
    new:
      def getProfile(
          username: Username,
          auth: Option[AuthHeader]
      ): F[GetProfileOutput] = ???

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
          rs <- withValidation(validateUpdateUserBody(user)) { valid =>
            auth
              .update(u.uid, valid)
              .map(_.toUser(u.user.token))
              .map(UpdateUserOutput(_))
              .recoverWith:
                case UserError.UserNotFound() => NotFoundError().raise
                case UserError.EmailAlreadyExists() |
                    UserError.UsernameAlreadyExists() =>
                  UnprocessableEntity().raise
          }
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

      def unfollowUser(
          username: Username,
          auth: AuthHeader
      ): F[UnfollowUserOutput] = ???

      def followUser(
          username: Username,
          authHeader: AuthHeader
      ): F[FollowUserOutput] = ???
      // auth
      //   .access(authHeader)
      //   .flatMap: userSessions =>
      //     users
      //       .followUser(username, userSessions)
      //       .map(FollowUserOutput(_))
end UserServiceImpl
