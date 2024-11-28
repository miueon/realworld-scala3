package realworld
package tests
package frontend

import scala.concurrent.duration.*
import com.indoorvivants.weaver.playwright.*
import org.http4s.*
import cats.syntax.all.*
import weaver.*
import cats.effect.*
import java.nio.file.Paths
import com.indoorvivants.weaver.playwright.PageContext
import com.indoorvivants.weaver.playwright.PlaywrightRetry
import com.microsoft.playwright.options.AriaRole

class PageFragements(
  pc: PageContext,
  probe: Probe,
  policy: PlaywrightRetry
):
  import pc.*

  import Expectations.*
  import Expectations.Helpers.*

  private def eventually[A](ioa: IO[A])(f: A => Expectations) = 
    PlaywrightExpectations.eventually(ioa, policy)(f)

  def submitRegistration(login: String, password: String): IO[Unit] = 
    for
      _ <- eventually(page(_.title())) { title =>
        expect.same(title, "Conduit: Register")
      }

      _ <- page(_.getByRole(AriaRole.BUTTON).getByText("Sign up"))

    yield ()
