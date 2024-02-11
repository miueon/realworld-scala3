package realworld.modules

import cats.data.*
import cats.effect.*
import cats.syntax.all.*

import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import realworld.db.DoobieTx
import realworld.repo.UserRepo

object Repos:
  def make[F[_]: MonadCancelThrow: DoobieTx](
      redis: RedisCommands[F, String, String],
      xa: Transactor[F]
  ): Repos[F] =
    new Repos[F](
      UserRepo.make(xa)
    ) {}

sealed abstract class Repos[F[_]] private (
    val userRepo: UserRepo[F]
)
