package realworld
package tests
package frontend

import cats.effect.*
import cats.effect.kernel.Outcome.Succeeded
import cats.syntax.all.*
import com.indoorvivants.weaver.playwright.*
import com.indoorvivants.weaver.playwright.BrowserConfig.Chromium
import com.microsoft.playwright.BrowserType.LaunchOptions
import weaver.*

import java.nio.file.Paths
import scala.concurrent.duration.*

case class Resources(
    probe: Probe,
    pw: PlaywrightRuntime
)

abstract class FrontendSuite(global: GlobalRead) extends weaver.IOSuite with PlaywrightIntegration:

  override type Res = Resources

  override def sharedResource: Resource[IO, Res] =
    integration.Fixture.resource.flatMap { pb =>
      PlaywrightRuntime
        .single(browser =
          Chromium(
            Some(
              LaunchOptions()
                .setHeadless(sys.env.contains("CI"))
                .setSlowMo(sys.env.get("CI").map(_ => 0).getOrElse(1000))
            )
          )
        )
        .map { pw =>
          Resources(pb, pw)
        }
    }
  end sharedResource

  val (poolSize, timeout) =
    if sys.env.contains("CI") then 1 -> 30.seconds
    else 5                           -> 5.seconds

  override def getPlaywright(res: Res): PlaywrightRuntime =
    res.pw

  override def retryPolicy: PlaywrightRetry =
    PlaywrightRetry.linear(10, 500.millis)

  def configure(pc: PageContext) = pc.page(_.setDefaultTimeout(timeout.toMillis))

  def frontendTest(name: TestName)(f: (Probe, PageContext, PageFragments) => IO[Expectations]) =
    test(name) { (res, logs) =>
      getPageContext(res).evalTap(configure).use { pc =>
        def screenshot(pc: PageContext, name: String) =
          val path = Paths.get("playwright-screenshots", name + ".png")
          pc.screenshot(path) *> logs.info(
            s"Screenshot of last known page state saved to ${path.toAbsolutePath()}"
          )

        def testName = name.name.collect {
          case c if c.isWhitespace => "_"
          case o                   => o
        }

        f(res.probe, pc, PageFragments(pc, retryPolicy))
          .guaranteeCase {
            case Outcome.Errored(e) => screenshot(pc, s"${testName}_error")
            case Succeeded(ioa) =>
              ioa.flatMap { exp =>
                if exp.run.isValid then IO.unit
                else screenshot(pc, s"${testName}_failure")
              }
            case _ => IO.unit
          }
      }
    }
end FrontendSuite
