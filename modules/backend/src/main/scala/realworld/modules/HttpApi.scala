package realworld.modules

import cats.syntax.all.*
import org.http4s.HttpApp
import cats.effect.kernel.Resource
import realworld.config.types.AppConfig
import realworld.service.Auth
import realworld.auth.JwtExpire
import cats.effect.kernel.Async
import realworld.auth.JWT
import dev.profunktor.redis4cats.RedisCommands
import realworld.auth.Crypto
import org.typelevel.log4cats.Logger
import smithy4s.http4s.SimpleRestJsonBuilder
import realworld.http.UserServiceImpl
import org.http4s.HttpRoutes
import cats.data.Kleisli
import org.http4s.dsl.request

def HttpApi[F[_]: Async: Logger](
    config: AppConfig,
    repos: Repos[F],
    redis: RedisCommands[F, String, String]
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
      Kleisli(request =>
        Logger[F].error(s"Request failed, ${request.toString()}")
      )
    }

  for
    auth  <- Resource.eval(makeAuth)
    users <- SimpleRestJsonBuilder.routes(UserServiceImpl.make(auth)).resource
  yield handleErrors(users)
end HttpApi
