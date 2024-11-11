package realworld
package tests

import cats.effect.IO
import realworld.spec.RegisterUserData
import realworld.types.*

import io.github.iltotore.iron.refine
import realworld.spec.LoginUserInputData
import realworld.tests.integration.IntegrationSuite

import weaver.*

class UsersSuite(globalRead: GlobalRead) extends IntegrationSuite(globalRead):
  test("Register") { probe =>
    import probe.*
    for
      usrRsp <- api.users
        .registerUser(RegisterUserData("123".refine, "123@123.com".refine, "123456789".refine))
        .map(_.user)
      getUsrRsp <- api.users
        .loginUser(LoginUserInputData("123@123.com".refine, "123456789".refine))
        .map(_.user)
    yield expect.all(
      usrRsp.username == getUsrRsp.username
    )
  }
end UsersSuite
