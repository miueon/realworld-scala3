package realworld
package tests
package frontend

import cats.effect.*
import cats.syntax.all.*
import com.indoorvivants.weaver.playwright.*
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import realworld.spec.CreateArticleData
import realworld.types.*
import weaver.*

class PageFragments(
  pc: PageContext,
  policy: PlaywrightRetry
):
  import Expectations.*
  import Expectations.Helpers.*
  import pc.*

  private def eventually[A](ioa: IO[A])(f: A => Expectations) =
    PlaywrightExpectations.eventually(ioa, policy)(f)

  def submitRegistration(login: Username, email: Email, password: Password): IO[Unit] =
    for
      _ <- eventually(page(_.title())) { title =>
        expect.same(title, "Conduit: Register")
      }

      signUpButton  <- page(_.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Sign up")))
      usernameField <- page(p => p.getByRole(AriaRole.TEXTBOX).and(p.getByPlaceholder("Username")))
      emailField    <- page(p => p.getByRole(AriaRole.TEXTBOX).and(p.getByPlaceholder("Email")))
      passwordField <- page(p => p.getByRole(AriaRole.TEXTBOX).and(p.getByPlaceholder("Password")))
      _             <- IO(usernameField.fill(login))
      _             <- IO(emailField.fill(email))
      _             <- IO(passwordField.fill(password))
      _             <- IO(signUpButton.click())
    yield ()

  def submitArticle(createArticleData: CreateArticleData): IO[Unit] =
    for
      _ <- eventually(page(_.title())) { title =>
        expect.same(title, "Conduit: New Article")
      }
      titleField       <- page(p => p.getByRole(AriaRole.TEXTBOX).and(p.getByPlaceholder("Article Title")))
      descriptionField <- page(p => p.getByRole(AriaRole.TEXTBOX).and(p.getByPlaceholder("What's this article about?")))
      bodyField        <- page(p => p.getByRole(AriaRole.TEXTBOX).and(p.getByPlaceholder("Write your article (in markdown)")))
      tagsField        <- page(p => p.getByRole(AriaRole.TEXTBOX).and(p.getByPlaceholder("Enter tags")))
      publishButton    <- page(_.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Publish Article")))
      _                <- IO(titleField.fill(createArticleData.title))
      _                <- IO(descriptionField.fill(createArticleData.description))
      _                <- IO(bodyField.fill(createArticleData.body))
      _                <- createArticleData.tagList.map(tag => IO(tagsField.fill(tag)) >> IO(tagsField.press("Enter"))).sequence
      _                <- IO(publishButton.click())
    yield ()
end PageFragments
