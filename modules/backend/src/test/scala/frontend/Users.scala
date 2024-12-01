package realworld
package tests
package frontend

import com.microsoft.playwright.options.AriaRole
import weaver.*

class UsersSpec(global: GlobalRead) extends FrontendSuite(global):
  frontendTest("Register user") { (probe, pc, fragments) =>
    import pc.*
    for
      _ <- page(_.navigate(probe.serverUri.toString))
      _ <- page(_.getByRole(AriaRole.LINK).getByText("Sign up").click())
      registerUserData <- probe.userDataSupport.registerUserData()
      _ <- fragments.submitRegistration(registerUserData.username, registerUserData.email, registerUserData.password)
    yield success
  }

  frontendTest("Add article") { (probe, pc, fragments) =>
    import pc.*

    for 
      _ <- page(_.navigate(probe.serverUri.toString))
      _ <- page(_.getByRole(AriaRole.LINK).getByText("Sign up").click())
      registerUserData <- probe.userDataSupport.registerUserData()
      _ <- fragments.submitRegistration(registerUserData.username, registerUserData.email, registerUserData.password)
      _ <- page(_.getByRole(AriaRole.LINK).getByText("New Article").click())
      createArticleData <- probe.articleDataSupport.createArticleData()
      _ <- fragments.submitArticle(createArticleData)
    yield success
  }
end UsersSpec
