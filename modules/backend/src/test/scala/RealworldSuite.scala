package realworld
package tests

import cats.effect.IO
import weaver.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger

trait RealworldSuite extends IOSuite:
  given logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]
  override type Res = Probe
end RealworldSuite
