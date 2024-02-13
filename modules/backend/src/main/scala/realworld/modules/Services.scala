package realworld.modules

import cats.effect.*

import realworld.service.Profiles

object Services:
  def make[F[_]: MonadCancelThrow](
      repos: Repos[F]
  ): Services[F] =
    new Services[F](
      Profiles.make(repos.userRepo, repos.followerRepo)
    ) {}

sealed abstract class Services[F[_]] private (
    val profiles: Profiles[F]
)
