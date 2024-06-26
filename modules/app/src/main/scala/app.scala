package realworld.app


import cats.effect.IO.asyncForIO
import cats.effect.*
import cats.effect.std.Supervisor

import dev.profunktor.redis4cats.log4cats.*

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import realworld.config.Config
import realworld.http.MkHttpServer
import realworld.modules.HttpApi
import realworld.modules.Repos
import realworld.modules.Services
import realworld.resources.AppResources

object Main extends IOApp:
  given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]) =
    Config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { sp =>
          AppResources
            .make(cfg)
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