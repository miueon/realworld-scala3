package realworld.http
import cats.effect.kernel.Async
import cats.effect.kernel.Resource

import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner

import org.typelevel.log4cats.Logger
import realworld.config.types.HttpServerConfig
import org.http4s.server.middleware.GZip

trait MkHttpServer[F[_]]:
  def newEmber(cfg: HttpServerConfig, httpApp: HttpApp[F]): Resource[F, Server]

object MkHttpServer:
  def apply[F[_]](using mkHttpServer: MkHttpServer[F]): MkHttpServer[F] =
    mkHttpServer

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(
      s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}"
    )

  given forAsyncLogger[F[_]: Async: Logger]: MkHttpServer[F] with
    def newEmber(
        cfg: HttpServerConfig,
        httpApp: HttpApp[F]
    ): Resource[F, Server] =
      EmberServerBuilder
        .default[F]
        .withHost(cfg.host)
        .withPort(cfg.port)
        .withHttpApp(GZip(httpApp))
        .build
        .evalTap(showEmberBanner)
end MkHttpServer
