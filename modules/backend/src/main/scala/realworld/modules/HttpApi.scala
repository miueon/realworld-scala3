package realworld.modules

import cats.data.Kleisli
import cats.effect.kernel.{Async, Resource}
import cats.syntax.all.*
import dev.profunktor.redis4cats.RedisCommands
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes, StaticFile}
import org.typelevel.log4cats.Logger
import realworld.auth.{Crypto, JWT, JwtExpire}
import realworld.config.types.AppConfig
import realworld.http.{ArticleServiceImpl, CommentServiceImpl, UserServiceImpl}
import realworld.service.Auth
import realworld.spec.UnprocessableEntity
import smithy4s.http.HttpPayloadError
import smithy4s.http4s.SimpleRestJsonBuilder

import java.nio.file.Paths

def HttpApi[F[_]: Async: Logger](
    config: AppConfig,
    repos: Repos[F],
    redis: RedisCommands[F, String, String],
    services: Services[F]
): Resource[F, HttpApp[F]] =
  def makeAuth: F[Auth[F]] =
    for
      jwt <- JwtExpire
        .make[F]
        .map(JWT.make(_, config.tokenConfig.value, config.tokenExpiration))
      crypto <- Crypto.make(config.passwordSalt.value)
    yield Auth.make(config.tokenExpiration, jwt, repos.userRepo, redis, crypto)

  def handleErrors(routes: HttpRoutes[F]) =
    routes.orNotFound.onError { exc =>
      Kleisli(request => Logger[F].error(exc)(s"Request failed, ${request.toString()}"))
    }

  for
    auth     <- Resource.eval(makeAuth)
    services <- Resource.pure(services)

    articleR <- SimpleRestJsonBuilder
      .routes(ArticleServiceImpl.make(services.articles, auth))
      .mapPayloadErrorToUnprocessableEntity
      .resource
    commentR <- SimpleRestJsonBuilder
      .routes(CommentServiceImpl.make(services.comments, auth))
      .mapPayloadErrorToUnprocessableEntity
      .resource
    tagR <- SimpleRestJsonBuilder
      .routes(realworld.http.TagServiceImpl.make(services.tags))
      .mapPayloadErrorToUnprocessableEntity
      .resource
    userR <- SimpleRestJsonBuilder
      .routes(UserServiceImpl.make(auth, services.profiles))
      .mapPayloadErrorToUnprocessableEntity
      .resource
  yield handleErrors(articleR <+> commentR <+> tagR <+> userR <+> Static().routes)
  end for
end HttpApi

extension [F[_]: Async](r: SimpleRestJsonBuilder.RouterBuilder[?, F])
  def mapPayloadErrorToUnprocessableEntity =
    r.mapErrors { case HttpPayloadError(p, exp, msg) =>
      UnprocessableEntity(errors = Map(p.toString -> List(exp, msg)).some)
    }

final case class Static[F[_]: Async: Logger]() extends Http4sDsl[F]:
  def routes =
    val indexHtml = StaticFile
      .fromResource[F]("static/index.html", None, preferGzipped = true)
      .getOrElseF(NotFound())

    HttpRoutes.of[F] {
      case req @ GET -> Root / "static" / "assets" / filename
          if filename.endsWith(".js") || filename.endsWith(".js.map") =>
        StaticFile
          .fromResource(
            Paths.get("static/assets", filename).toString,
            Some(req),
            preferGzipped = true
          )
          .getOrElseF(NotFound())

      case req @ GET -> Root        => indexHtml
      case req if req.method == GET => indexHtml
    }
  end routes
end Static
