package realworld
package tests

import cats.syntax.all.*
import realworld.spec.{LoginUserInputData, RegisterUserData}
import realworld.tests.integration.IntegrationSuite
import realworld.types.*
import weaver.*
import realworld.spec.Bio
import realworld.spec.UpdateUserData

class UsersSuite(globalRead: GlobalRead) extends IntegrationSuite(globalRead):
  test("Register and login") { (probe, log) =>
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
    yield expect.all(
      usrRsp.username == getUsrRsp.username
    )
    end for
  }

  test("Update user data") { probe =>
    import probe.*

    for
      authHeader <- userDataSupport.authenticateUser
      user       <- api.users.getUser(authHeader).map(_.user)
      updateUserData <- (gen.strI(Username), gen.str(Bio), gen.strI(ImageUrl)).mapN(
        (username, description, imageUrl) =>
          UpdateUserData(username = Some(username), bio = Some(description), image = Some(imageUrl))
      )
      user <- api.users.updateUser(authHeader, updateUserData).map(_.user)
    yield expect.all(
      updateUserData.username.isDefined,
      user.username == updateUserData.username.get,
      user.bio == updateUserData.bio,
      user.image == updateUserData.image
    )
    end for
  }
end UsersSuite
