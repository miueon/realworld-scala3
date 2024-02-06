package realworld.http

import cats.Monad
import cats.MonadError
import cats.MonadThrow
import cats.syntax.all.*

import org.typelevel.log4cats.Logger
import realworld.service.Auth
import realworld.service.Users
import realworld.spec.AuthHeader
import realworld.spec.FollowUserOutput
import realworld.spec.GetProfileOutput
import realworld.spec.GetUserOutput
import realworld.spec.LoginUserInputData
import realworld.spec.LoginUserOutput
import realworld.spec.RegisterUserData
import realworld.spec.RegisterUserOutput
import realworld.spec.UnfollowUserOutput
import realworld.spec.UpdateUserData
import realworld.spec.UpdateUserOutput
import realworld.spec.UserService
import realworld.spec.Username
import realworld.validation.validateUserName
import realworld.validation.validateUserPassword
import realworld.domain.users.UserError
import realworld.spec.NotFoundError
import realworld.spec.UnprocessableEntity
import smithy4s.Smithy4sThrowable

object UserService:
  def make[F[_]: MonadThrow: Logger](
      users: Users[F],
      auth: Auth[F]
  ): UserService[F] =
    new:
      def getProfile(
          username: Username,
          auth: Option[AuthHeader]
      ): F[GetProfileOutput] = ???

      def loginUser(user: LoginUserInputData): F[LoginUserOutput] =
        auth.login(user).map(LoginUserOutput(_))

      def getUser(authHeader: AuthHeader): F[GetUserOutput] =
        auth.access(authHeader).map(u => GetUserOutput(u.user))

      def updateUser(
          authHeader: AuthHeader,
          user: UpdateUserData
      ): F[UpdateUserOutput] =
        auth.access(authHeader).flatMap { u =>
          val validation = List(
            user.username.map(validateUserName),
            user.password.map(validateUserPassword)
          ).traverse_(a =>
            a.orElse(Right(()).some)
              .traverse_(validations =>
                MonadError[F, Throwable].fromEither(validations)
              )
          )

          // users.updateUser(u.id, )
          auth
            .update(u.uid, user)
            .map(_.toUser(u.user.token))
            .map(UpdateUserOutput(_))
            .recoverWith:
              case UserError.UserNotFound() => raise(NotFoundError())
              case UserError.EmailAlreadyExists() |
                  UserError.UsernameAlreadyExists() =>
                raise(UnprocessableEntity())
        }

      def raise[T <: Smithy4sThrowable, A](t: T): F[A] =
        MonadError[F, Throwable].raiseError(t)

      def registerUser(user: RegisterUserData): F[RegisterUserOutput] =
        auth
          .register(user)
          .map(RegisterUserOutput(_))
          .onError(_ => Logger[F].warn(s"Failed to register user: $user"))

      def unfollowUser(
          username: Username,
          auth: AuthHeader
      ): F[UnfollowUserOutput] = ???

      def followUser(
          username: Username,
          auth: AuthHeader
      ): F[FollowUserOutput] =
        ???
end UserService
