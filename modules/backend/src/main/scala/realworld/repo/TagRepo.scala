package realworld.repo

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*

import doobie.Fragments.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import realworld.domain.Tag
import realworld.domain.article.ArticleId

trait TagRepo[F[_]]:
  def findTags(articleIds: List[ArticleId]): F[List[Tag]]

object TagRepo:
  def make[F[_]: MonadCancelThrow](
      xa: Transactor[F]
  ): TagRepo[F] =
    new:
      def findTags(articleIds: List[ArticleId]): F[List[Tag]] =
        NonEmptyList.fromFoldable(articleIds.distinct) match
          case Some(ids) => TagRepoSQL.findTags(ids).transact(xa)
          case None      => List.empty[Tag].pure[F]

object TagRepoSQL:
  import realworld.domain.Tags as t

  def findTags(articleIds: NonEmptyList[ArticleId]): ConnectionIO[List[Tag]] =
    sql"""
    SELECT ${t.rowCol} FROM $t
    WHERE ${t.articleId in articleIds}
    """
      .queryOf(t.rowCol)
      .to[List]
