package realworld.resources

import cats.effect.Concurrent
import cats.effect.Resource
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.std.Console
import cats.syntax.all.*

import com.zaxxer.hikari.HikariConfig
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.effect.MkRedis
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.Logger
import realworld.config.types.AppConfig
import realworld.config.types.PostgresSQLConfig
import realworld.config.types.RedisConfig
import doobie.util.log.LogEvent
import doobie.util.log.*

sealed abstract class AppResources[F[_]](
    val redis: RedisCommands[F, String, String],
    val xa: Transactor[F]
)

object AppResources:
  def make[F[_]: Concurrent: Console: Logger: MkRedis: Async](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] =
    def checkRedisConnection(
        redis: RedisCommands[F, String, String]
    ): F[Unit] =
      redis.info.flatMap {
        _.get("redis_version").traverse_(v => Logger[F].info(s"Connected to Redis $v"))
      }

    def checkPostgresConnection(
        xa: Transactor[F]
    ): F[Unit] =
      sql"""
      SELECT version()
      """.query[String].unique.transact(xa).flatMap { v =>
        Logger[F].info(s"Connected to Postgres $v")
      }

    def mkRedisResource(
        c: RedisConfig
    ): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value).evalTap(checkRedisConnection)

    def mkTransactor(p: PostgresSQLConfig): Resource[F, Transactor[F]] =
      val logHandler = new LogHandler[F]:
        def run(logEvent: LogEvent): F[Unit] =
          logEvent match
            case _: Success                      => ().pure[F]
            case ExecFailure(sql, args, l, e, f) => Logger[F].error(s"sql=$sql \n args=$args")
            case ProcessingFailure(sql, args, l, e, p, f) =>
              Logger[F].error(s"sql=$sql \n args=$args")

      val xaResource = for
        hikariConfig <- Resource.pure {
          val config = new HikariConfig()
          config.setDriverClassName("org.postgresql.Driver")
          config.setJdbcUrl(p.jdbcUrl)
          config.setPassword(p.password.value)
          config.setUsername(p.user)
          config
        }
        xa <- HikariTransactor.fromHikariConfig[F](hikariConfig, logHandler = logHandler.some)
      yield xa
      xaResource
        .evalTap(checkPostgresConnection)
        .evalTap {
          _.configure { ds =>
            Async[F].delay(Flyway.configure().dataSource(ds).load().migrate())
          }
        }
    end mkTransactor

    (
      mkRedisResource(cfg.redis),
      mkTransactor(cfg.postgresSQL)
    ).parMapN(new AppResources(_, _) {})
  end make
end AppResources
