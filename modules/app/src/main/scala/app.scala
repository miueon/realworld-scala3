package realworld.app
import java.io.File
import java.io.FileInputStream

import cats.effect.IO.asyncForIO
import cats.effect.*
import cats.effect.std.Supervisor
import cats.syntax.all.*

import dev.profunktor.redis4cats.log4cats.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import realworld.config.Config
import realworld.http.MkHttpServer
import realworld.modules.HttpApi
import realworld.modules.Repos
import realworld.resources.AppResources

object Main extends IOApp:
  import scala.jdk.CollectionConverters.*
  given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]) =
    println("test")
    Config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { sp =>
          given Supervisor[IO] = sp
          AppResources
            .make(cfg)
            .flatMap { res =>
              val repos = Repos.make(res.redis, res.xa)
              for httpApp <- HttpApi(cfg, repos, res.redis)
              yield cfg.httpServerConfig -> httpApp
            }
            .flatMap { case (cfg, httpApp) =>
              MkHttpServer[IO].newEmber(cfg, httpApp)
            }
            .useForever
        }
    }
  end run
end Main
