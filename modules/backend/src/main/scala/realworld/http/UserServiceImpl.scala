package realworld.http

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

object UserService:
  def make[F[_]](users: Users[F]): UserService[F] =
    new:
      def getProfile(
          username: Username,
          auth: Option[AuthHeader]
      ): F[GetProfileOutput] = ???
      def loginUser(user: LoginUserInputData): F[LoginUserOutput] = ???
      def getUser(auth: AuthHeader): F[GetUserOutput]             = ???
      def updateUser(
          auth: AuthHeader,
          user: UpdateUserData
      ): F[UpdateUserOutput] =
        ???
      def registerUser(user: RegisterUserData): F[RegisterUserOutput] = ???
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
