package realworld.modules

import cats.MonadThrow
import cats.syntax.all.*
import org.http4s.AuthScheme
import org.http4s.Credentials
import org.http4s.HttpApp
import org.http4s.Response
import org.http4s.headers.Authorization
import realworld.auth.JWT
import realworld.spec.Token
import realworld.spec.UnauthorizedError
import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.http4s.ServerEndpointMiddleware

object JwtAuthMiddleware:
  def apply[F[_]: MonadThrow](
      jwt: JWT[F]
  ): ServerEndpointMiddleware[F] =
    new ServerEndpointMiddleware.Simple[F]:
      def prepareWithHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): HttpApp[F] => HttpApp[F] =
        serviceHints.get[smithy.api.HttpBearerAuth] match
          case Some(_) =>
            endpointHints.get[smithy.api.Auth] match
              case Some(auths) if auths.value.isEmpty => identity
              case _                                  => middleware(jwt)

          case None => identity

  def middleware[F[_]: MonadThrow](
      jwt: JWT[F]
  ): HttpApp[F] => HttpApp[F] = inputApp =>
    HttpApp[F]: request =>
      val maybeKey = request.headers
        .get[`Authorization`]
        .collect {
          case Authorization(Credentials.Token(AuthScheme.Bearer, value)) =>
            value
        }
        .map(Token.apply)

      val isAuthorized = maybeKey.map(jwt.validate).getOrElse(false.pure[F])
      isAuthorized.ifM(
        ifTrue = inputApp(request),
        ifFalse =
          UnauthorizedError("Not authorized!".some).raiseError[F, Response[F]]
      )
end JwtAuthMiddleware
