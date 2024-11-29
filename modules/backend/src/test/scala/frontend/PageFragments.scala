package realworld
package tests
package frontend

import cats.effect.*
import com.indoorvivants.weaver.playwright.*
import com.microsoft.playwright.options.AriaRole
import weaver.*
import com.microsoft.playwright.Page

class PageFragments(
  pc: PageContext,
  policy: PlaywrightRetry
):
  import pc.*

  import Expectations.*
  import Expectations.Helpers.*

  private def eventually[A](ioa: IO[A])(f: A => Expectations) = 
    PlaywrightExpectations.eventually(ioa, policy)(f)

  def submitRegistration(login: String, email: String, password: String): IO[Unit] = 
    for
      _ <- eventually(page(_.title())) { title =>
        expect.same(title, "Conduit: Register")
      }

      signUpButton <- page(_.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Sign up")))
      usernameField <- page(_.getByPlaceholder("Username"))
      emailField <- page(_.getByPlaceholder("Email"))
      passwordField <- page(_.getByPlaceholder("Password"))
      _ <- IO(usernameField.fill(login))
      _ <- IO(emailField.fill(email))
      _ <- IO(passwordField.fill(password))
      _ <- IO(signUpButton.click())
    yield ()
