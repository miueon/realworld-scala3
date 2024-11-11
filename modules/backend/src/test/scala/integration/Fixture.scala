package realworld
package tests
package integration

import cats.effect.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import cats.effect.kernel.Resource
import com.dimafeng.testcontainers.RedisContainer
import org.flywaydb.core.Flyway
import realworld.config.types.PostgresSQLConfig
import java.net.URI
import ciris.*
import io.github.iltotore.iron.*
import realworld.config.*
import io.github.iltotore.iron.constraint.string.*
import io.github.iltotore.iron.cats.given
import realworld.config.types.RedisConfig
import realworld.config.types.RedisURI
import realworld.config.types.AppConfig
import realworld.config.types.JwtAccessTokenKeyConfig
import realworld.config.types.PasswordSalt
import realworld.config.types.TokenExpiration
import com.comcast.ip4s.*

import dev.profunktor.redis4cats.log4cats.*

import scala.concurrent.duration.*
import realworld.config.types.HttpServerConfig
import realworld.resources.AppResources
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import realworld.modules.Repos
import realworld.modules.Services
import realworld.modules.HttpApi
import org.http4s.HttpApp
import org.http4s.Response
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.blaze.client.BlazeClientBuilder

object Fixture:
  given logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]
  private def postgresContainer =
    val start = IO(
      PostgreSQLContainer(
        dockerImageNameOverride = DockerImageName("postgres:15"),
        mountPostgresDataToTmpfs = true
      )
    ).flatTap(cont => IO(cont.start()))
    Resource.make(start)(cont => IO(cont.stop()))

  def parseUrl(url: String): IO[URI] = IO(URI.create(url.substring(5)))

  def postSQLConfig: Resource[IO, PostgresSQLConfig] =
    postgresContainer
      .evalMap(cont => parseUrl(cont.jdbcUrl).map(cont -> _))
      .evalTap { case (cont, _) => migrate(cont.jdbcUrl, cont.username, cont.password) }
      .map { case (cont, jdbcUrl) =>
        PostgresSQLConfig(
          jdbcUrl = cont.jdbcUrl.refine,
          user = cont.username.refine,
          password = Secret(cont.password.refine)
        )
      }

  def migrate(url: String, user: String, password: String): IO[Unit] =
    IO(Flyway.configure().dataSource(url, user, password).load()).flatMap { f =>
      IO(f.migrate())
    }

  private def redisContainer =
    val start = IO(
      RedisContainer(
      )
    ).flatTap(cont => IO(cont.start()))

    Resource.make(start)(cont => IO(cont.stop()))

  private def redisConfig =
    redisContainer.map(cont => RedisConfig(RedisURI(cont.redisUri.refine)))

  def resource: Resource[IO, Probe] =
    for
      shutdownLatch <- Resource.eval(IO.ref(false))
      postSQLConfig <- postSQLConfig
      redisConfig   <- redisConfig
      appConfig = AppConfig(
        tokenConfig = Secret(JwtAccessTokenKeyConfig("secret")),
        passwordSalt = Secret(PasswordSalt("salt")),
        tokenExpiration = TokenExpiration(30.seconds),
        postgresSQL = postSQLConfig,
        redis = redisConfig,
        httpServerConfig = HttpServerConfig(host = host"localhost", port"0")
      )
      appResource <- AppResources.make[IO](appConfig)
      repos    = Repos.make(appResource.redis, appResource.xa)
      services = Services.make(repos)
      routes <- HttpApi(appConfig, repos, appResource.redis, services)
      latchedRoutes = HttpApp[IO] { case req =>
        shutdownLatch.get.flatMap { isEnd =>
          if isEnd then
            IO.pure(
              Response[IO](
                org.http4s.Status.InternalServerError
              ).withEntity("Server is shutting down")
            )
          else routes.run(req)
        }
      }
      uri <- BlazeServerBuilder[IO]
        .withHttpApp(latchedRoutes)
        .bindHttp(0, "localhost")
        .resource
        .map(_.baseUri)

      client <- BlazeClientBuilder[IO].resource.onFinalize(shutdownLatch.set(true))
      probe  <- Probe.make(client, uri, appConfig)
    yield probe
    end for
  end resource
end Fixture
