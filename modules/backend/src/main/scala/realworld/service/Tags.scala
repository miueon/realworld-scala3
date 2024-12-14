package realworld.service

import cats.Functor
import cats.syntax.all.*
import realworld.repo.TagRepo
import realworld.spec.TagList

trait Tags[F[_]]:
  def list(): F[TagList]

object Tags:
  def make[F[_]: Functor](
    tagRepo: TagRepo[F]
  ): Tags[F] =
    new:
      def list(): F[TagList] =
        tagRepo.listTagNameByPopular().map(TagList.apply)
