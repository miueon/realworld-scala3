package realworld
package tests

import cats.effect.IO
import weaver.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger

trait RealworldSuite extends IOSuite:
  given logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]
  override type Res = Probe

  def probeTest(name: weaver.TestName)(f: Probe => IO[weaver.Expectations]) =
    test(name) { probe =>
      f(probe).attempt
        .flatMap(IO.fromEither)
    }
end RealworldSuite
