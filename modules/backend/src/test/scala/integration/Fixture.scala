package realworld
package tests
package integration

import _root_.cats.data.Chain
import cats.Show
import cats.effect.*
import cats.effect.kernel.Resource
import cats.syntax.all.*
import ciris.*
import com.comcast.ip4s.*
import com.dimafeng.testcontainers.{PostgreSQLContainer, RedisContainer}
import dev.profunktor.redis4cats.log4cats.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.constraint.string.*
import org.flywaydb.core.Flyway
import org.http4s.{HttpApp, Response}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.testcontainers.utility.DockerImageName
import realworld.config.*
import realworld.config.types.{
  AppConfig, HttpServerConfig, JwtAccessTokenKeyConfig, PasswordSalt, PostgresSQLConfig, RedisConfig, RedisURI,
  TokenExpiration
}
import realworld.effects.Time
import realworld.modules.{HttpApi, Repos, Services}
import realworld.resources.AppResources
import weaver.Log
import weaver.Log.Entry
import weaver.LoggerImplicits.*

import java.net.URI
import scala.concurrent.duration.*

object Fixture:
  // given logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]
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

  private def collectedLog(ref: Ref[IO, Chain[Entry]]): Log[IO] =
    new Log[IO](Time[IO].now.map(_.toEpochMilli())) {

      def log(l: => Entry): IO[Unit] =
        ref.update(_ |+| l.pure[Chain])
    }

  def resource: Resource[IO, Probe] =
    for
      shutdownLatch <- Resource.eval(IO.ref(false))
      logRef        <- Ref[IO].of(Chain.empty[Log.Entry]).toResource
      logInstance   = collectedLog(logRef)
      given Log[IO] = logInstance
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
      probe  <- Probe.make(client, uri, appConfig, logRef)
    yield probe
    end for
  end resource
end Fixture
