package realworld
package tests

import cats.syntax.all.*
import realworld.spec.{LoginUserInputData, RegisterUserData, UpdateUserData}
import realworld.tests.integration.IntegrationSuite
import realworld.types.*
import weaver.*

class UsersSuite(globalRead: GlobalRead) extends IntegrationSuite(globalRead):
  probeTest("Register and login") { probe =>
    import probe.*

    for
      registerUserData <- (
        gen.strI(Username),
        gen.email,
        gen.strI(Password)
      ).mapN(RegisterUserData.apply)
      usrRsp <- api.users
        .registerUser(registerUserData)
        .map(_.user)
      getUsrRsp <- api.users
        .loginUser(LoginUserInputData(registerUserData.email, registerUserData.password))
        .map(_.user)
    yield
      expect.all(
        usrRsp.username == getUsrRsp.username
      )
    end for
  }

  probeTest("Update user data") { probe =>
    import probe.*

    for
      authHeader     <- userDataSupport.authenticateUser
      user           <- api.users.getUser(authHeader).map(_.user)
      updateUserData <- userDataSupport.updateUserData()
      user           <- api.users.updateUser(authHeader, updateUserData).map(_.user)
    yield
      expect.all(
        updateUserData.username.isDefined,
        user.username == updateUserData.username.get,
        user.bio == updateUserData.bio,
        user.image == updateUserData.image
      )
    end for
  }
end UsersSuite
