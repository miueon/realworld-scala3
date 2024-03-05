package realworld.modules

import cats.data.Kleisli
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all.*
import dev.profunktor.redis4cats.RedisCommands
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.typelevel.log4cats.Logger
import realworld.auth.Crypto
import realworld.auth.JWT
import realworld.auth.JwtExpire
import realworld.config.types.AppConfig
import realworld.http.ArticleServiceImpl
import realworld.http.CommentServiceImpl
import realworld.http.UserServiceImpl
import realworld.service.Auth
import smithy4s.http4s.SimpleRestJsonBuilder

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
      .resource
    commentR <- SimpleRestJsonBuilder
      .routes(CommentServiceImpl.make(services.comments, auth))
      .resource
    tagR <- SimpleRestJsonBuilder.routes(realworld.http.TagServiceImpl.make(services.tags)).resource
    userR <- SimpleRestJsonBuilder.routes(UserServiceImpl.make(auth, services.profiles)).resource
  yield handleErrors(articleR <+> commentR <+> tagR <+> userR)
end HttpApi
