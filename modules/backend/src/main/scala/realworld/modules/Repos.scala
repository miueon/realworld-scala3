package realworld.modules

import cats.data.*
import cats.effect.*
import cats.syntax.all.*

import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import realworld.db.DoobieTx
import realworld.repo.UserRepo
import realworld.repo.FollowerRepo

object Repos:
  def make[F[_]: MonadCancelThrow: DoobieTx](
      redis: RedisCommands[F, String, String],
      xa: Transactor[F]
  ): Repos[F] =
    new Repos[F](
      UserRepo.make(xa),
      FollowerRepo.make(xa)
    ) {}

sealed abstract class Repos[F[_]] private (
    val userRepo: UserRepo[F],
    val followerRepo: FollowerRepo[F]
)
