package realworld.app


import cats.effect.*
import cats.effect.IO.asyncForIO
import cats.effect.std.Supervisor
import dev.profunktor.redis4cats.log4cats.*
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import realworld.config.Config
import realworld.http.MkHttpServer
import realworld.modules.{HttpApi, Repos, Services}
import realworld.resources.AppResources

object Main extends IOApp:
  given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def migrate(appResource: AppResources[IO]): IO[Unit] =
    appResource.xa.configure { ds =>
      Async[IO].delay(Flyway.configure().dataSource(ds).load().migrate())
    }

  def run(args: List[String]) =
    Config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { sp =>
          AppResources
            .make(cfg)
            .evalTap(migrate)
            .flatMap { res =>
              val repos    = Repos.make(res.redis, res.xa)
              val services = Services.make(repos)
              for httpApp <- HttpApi(cfg, repos, res.redis, services)
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