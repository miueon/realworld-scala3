package realworld.http

import cats.MonadThrow
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import realworld.domain.user.UserError
import realworld.service.{Auth, Profiles}
import realworld.spec.{
  AuthHeader,
  FollowUserOutput,
  ForbiddenError,
  GetProfileOutput,
  GetUserOutput,
  LoginUserInputData,
  LoginUserOutput,
  NotFoundError,
  RegisterUserData,
  RegisterUserOutput,
  UnfollowUserOutput,
  UnprocessableEntity,
  UpdateUserData,
  UpdateUserOutput,
  UserService
}
import realworld.types.Username

object UserServiceImpl:
  def make[F[_]: MonadThrow: Logger](
      auth: Auth[F],
      profiles: Profiles[F]
  ): UserService[F] =
    new:
      def getProfile(
          username: Username,
          authHeaderOpt: Option[AuthHeader]
      ): F[GetProfileOutput] =
        val result = for
          uidOpt  <- authHeaderOpt.map(auth.authUserId).sequence
          profile <- profiles.get(username, uidOpt)
        yield GetProfileOutput(profile)
        result
          .onError(e => Logger[F].warn(e)(s"Failed to get profile for user: $username"))
          .recoverWith:
            case UserError.ProfileNotFound(msg) => NotFoundError(msg.some).raise

      def unfollowUser(
          username: Username,
          authHeader: AuthHeader
      ): F[UnfollowUserOutput] =
        val result = for
          userSession <- auth.access(authHeader)
          profile     <- profiles.unfollow(username, userSession.id)
        yield UnfollowUserOutput(profile)
        result.recoverWith:
          case UserError.ProfileNotFound(msg) => NotFoundError(msg.some).raise

      def followUser(
          username: Username,
          authHeader: AuthHeader
      ): F[FollowUserOutput] =
        val result = for
          userSession <- auth.access(authHeader)
          profile     <- profiles.follow(username, userSession.id)
        yield FollowUserOutput(profile)
        result.recoverWith:
          case UserError.ProfileNotFound(msg) => NotFoundError(msg.some).raise

      def loginUser(user: LoginUserInputData): F[LoginUserOutput] =
        auth
          .login(user)
          .map(LoginUserOutput(_))
          .recoverWith:
            case UserError.UserNotFound(msg)           => NotFoundError(msg.some).raise
            case UserError.UserPasswordNotMatched(msg) => ForbiddenError(msg.some).raise

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
              .update(u, user)
              .map(userSession => UpdateUserOutput(userSession.user))
              .recoverWith {
                case UserError.UserNotFound(msg) => NotFoundError(msg.some).raise
                case UserError.EmailAlreadyExists(_) =>
                  UnprocessableEntity(Map("Email" -> List("already existed")).some).raise
                case UserError.UsernameAlreadyExists(_) =>
                  UnprocessableEntity(Map("Username" -> List("already existed")).some).raise
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
            case UserError.EmailAlreadyExists(_) =>
              UnprocessableEntity(Map("Email" -> List("already exisits")).some).raise
            case UserError.UsernameAlreadyExists(_) =>
              UnprocessableEntity(Map("Username" -> List("already exisits")).some).raise

end UserServiceImpl
