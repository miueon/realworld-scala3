package realworld
package tests

import cats.effect.IO
import cats.syntax.all.*
import weaver.*

trait RealworldSuite extends IOSuite:
  export weaver.LoggerImplicits.*
  override type Res = Probe

  def probeTest(name: weaver.TestName)(f: Probe => IO[weaver.Expectations]) =
    test(name) { (probe, log) =>
      val dumpLogs = probe.logs.get.flatMap { l =>
        l.sortBy(_.timestamp).map(log.log(_)).sequence.void
      }

      f(probe).attempt.flatTap(_ => dumpLogs).flatMap(IO.fromEither)
    }
end RealworldSuite
