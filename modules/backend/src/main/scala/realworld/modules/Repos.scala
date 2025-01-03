package realworld.modules

import cats.effect.*
import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import realworld.db.DoobieTx
import realworld.repo.{ArticleRepo, CommentRepo, FavoriteRepo, FollowerRepo, TagRepo, UserRepo}

object Repos:
  def make[F[_]: MonadCancelThrow: DoobieTx: Logger](
    redis: RedisCommands[F, String, String],
    xa: Transactor[F]
  ): Repos[F] =
    new Repos[F](
      ArticleRepo.make(xa),
      CommentRepo.make(xa),
      FavoriteRepo.make(xa),
      FollowerRepo.make(xa),
      TagRepo.make(xa),
      UserRepo.make(xa)
    ) {}

sealed abstract class Repos[F[_]] private (
  val articleRepo: ArticleRepo[F],
  val commentRepo: CommentRepo[F],
  val favRepo: FavoriteRepo[F],
  val followerRepo: FollowerRepo[F],
  val tagRepo: TagRepo[F],
  val userRepo: UserRepo[F]
)
