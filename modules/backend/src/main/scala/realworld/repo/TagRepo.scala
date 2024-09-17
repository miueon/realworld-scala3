package realworld.repo

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import realworld.domain.Tag
import realworld.domain.article.ArticleId
import realworld.types.TagName

trait TagRepo[F[_]]:
  def listTagNameByPopular(): F[List[TagName]]
  def listTag(articleIds: List[ArticleId]): F[List[Tag]]
  def listTagsById(articleId: ArticleId): F[List[Tag]] =
    listTag(List(articleId))

object TagRepo:
  def make[F[_]: MonadCancelThrow](
      xa: Transactor[F]
  ): TagRepo[F] =
    new:
      def listTag(articleIds: List[ArticleId]): F[List[Tag]] =
        NonEmptyList.fromFoldable(articleIds.distinct) match
          case Some(ids) => TagRepoSQL.selectTags(ids).transact(xa)
          case None      => List.empty[Tag].pure[F]

      def listTagNameByPopular(): F[List[TagName]] = 
        TagRepoSQL.selectTagOrderByPopular().transact(xa)

object TagRepoSQL:
  import realworld.domain.Tags as t

  def selectTags(articleIds: NonEmptyList[ArticleId]): ConnectionIO[List[Tag]] =
    sql"""
    SELECT ${t.rowCol} FROM $t
    WHERE ${t.articleId in articleIds}
    """
      .queryOf(t.rowCol)
      .to[List]

  def selectTagOrderByPopular() =
    sql"""
    SELECT ${t.tag} FROM $t
    GROUP BY ${t.tag}
    ORDER BY COUNT(1) DESC
    """
      .queryOf(t.tag)
      .to[List]
end TagRepoSQL
