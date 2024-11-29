package realworld
package tests
package frontend

import cats.effect.*
import cats.syntax.all.*
import com.indoorvivants.weaver.playwright.*
import com.microsoft.playwright.options.AriaRole
import org.http4s.*
import weaver.*

import java.nio.file.Paths
import scala.concurrent.duration.*

class UsersSpec(global: GlobalRead) extends FrontendSuite(global):
  frontendTest("Register user") { (probe, pc, fragments) =>
    import pc.*
    for
      _ <- page(_.navigate(probe.serverUri.toString))
      _ <- page(_.getByRole(AriaRole.LINK).getByText("Sign up").click())
      _ <- fragments.submitRegistration("test", "test@test.com", "test1234")
    yield success
  }
