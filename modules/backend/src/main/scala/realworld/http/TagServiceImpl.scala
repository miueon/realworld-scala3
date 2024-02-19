package realworld.http

import cats.effect.*

import org.typelevel.log4cats.Logger
import realworld.spec.ListTagOutput
import realworld.spec.TagService

object TagServiceImpl:
  def make[F[_]: MonadCancelThrow: Logger](): TagService[F] =
    new:
      def listTag(): F[ListTagOutput] = ???
