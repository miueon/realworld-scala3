package realworld
package tests

import cats.syntax.all.*
import realworld.spec.{AuthHeader, LoginUserInputData, RegisterUserData}
import realworld.types.*

class UserDataSupport(probe: Probe):
  import probe.*

  def registerUserData() =
    (gen.strI(Username), gen.email, gen.strI(Password)).mapN(RegisterUserData.apply)

  def login(email: Email, password: Password) =
    api.users.loginUser(LoginUserInputData(email, password)).map(resp => AuthHeader(s"Token ${resp.user.token.get}"))

  def authenticateUser =
    for
      registerUserData <- registerUserData()
      _                <- api.users.registerUser(registerUserData)
      authHeader       <- login(registerUserData.email, registerUserData.password)
    yield authHeader

  def prepareTwoUsers =
    for
      user1 <- registerUserData()
      user2 <- registerUserData()
      _     <- api.users.registerUser(user1)
      _     <- api.users.registerUser(user2)
    yield (user1, user2)

  def prepareTwoUsersWithFollowing =
    for 
      (user1, user2) <- prepareTwoUsers
      authHeader2     <- login(user2.email, user2.password)
      _               <- api.users.followUser(user1.username, authHeader2)
    yield (user1, user2)
end UserDataSupport
