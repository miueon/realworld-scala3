package realworld.http

import cats.effect.*
import cats.syntax.all.*

import org.typelevel.log4cats.Logger
import realworld.spec.ListTagOutput
import realworld.spec.TagService
import realworld.service.Tags

object TagServiceImpl:
  def make[F[_]: MonadCancelThrow: Logger](tags: Tags[F]): TagService[F] =
    new:
      def listTag(): F[ListTagOutput] =
        tags.list().map(ts => ListTagOutput.apply(ts.value))
