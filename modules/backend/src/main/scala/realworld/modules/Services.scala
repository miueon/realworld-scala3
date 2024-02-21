package realworld.modules

import cats.effect.*
import realworld.effects.GenUUID
import realworld.service.Articles
import realworld.service.Comments
import realworld.service.Profiles
import realworld.service.Tags
import org.typelevel.log4cats.Logger

object Services:
  def make[F[_]: MonadCancelThrow: GenUUID: Logger](
      repos: Repos[F]
  ): Services[F] =
    new Services[F](
      Articles.make(repos.articleRepo, repos.favRepo, repos.tagRepo, repos.followerRepo),
      Comments.make(repos.commentRepo, repos.articleRepo, repos.followerRepo),
      Profiles.make(repos.userRepo, repos.followerRepo),
      Tags.make(repos.tagRepo)
    ) {}

sealed abstract class Services[F[_]] private (
    val articles: Articles[F],
    val comments: Comments[F],
    val profiles: Profiles[F],
    val tags: Tags[F]
)
