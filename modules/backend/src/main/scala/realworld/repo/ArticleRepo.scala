package realworld.repo

import cats.effect.*

import doobie.Fragments.*
import doobie.*
import doobie.implicits.*
import realworld.domain.WithId
import realworld.domain.WithTotal
import realworld.domain.article.*
import realworld.domain.user.UserId
import realworld.http.Pagination
import realworld.spec.Bio
import realworld.spec.Body
import realworld.spec.CreatedAt
import realworld.spec.Description
import realworld.spec.ImageUrl
import realworld.spec.Slug
import realworld.spec.Title
import realworld.spec.Total
import realworld.spec.UpdatedAt
import realworld.spec.Username

trait ArticleRepo[F[_]]:
  def list(
      query: ListArticleQuery,
      pagination: Pagination
  ): F[WithTotal[List[ArticleView]]]

  def listByFollowerId(followerId: UserId, pagination: Pagination): F[WithTotal[List[ArticleView]]]

  def getBySlug(slug: Slug): F[Option[ArticleView]]

object ArticleRepo:
  import ArticleSQL as A
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): ArticleRepo[F] =
    new:
      def list(
          query: ListArticleQuery,
          pagination: Pagination
      ): F[WithTotal[List[ArticleView]]] =
        val result = for
          articles <- A.list(query, pagination)
          total    <- A.listTotal(query)
        yield WithTotal(total, articles)
        result.transact(xa)

      def listByFollowerId(
          followerId: UserId,
          pagination: Pagination
      ): F[WithTotal[List[ArticleView]]] =
        val result = for
          articles <- A.listByFollowerId(followerId, pagination)
          total    <- A.listFeedTotal(followerId)
        yield WithTotal(total, articles)
        result.transact(xa)

      def getBySlug(slug: Slug): F[Option[ArticleView]] = A.getBySlug(slug).transact(xa)
end ArticleRepo

private object ArticleSQL:
  import realworld.domain.article.Articles
  import realworld.domain.Tags
  import realworld.domain.user.Users
  import realworld.domain.Favorites
  import realworld.domain.follower.Followers as fo
  import realworld.domain.given

  private val a  = Articles as "a"
  private val t  = Tags as "t"
  private val au = Users as "u"  // Author
  private val cu = Users as "cu" // Current user
  private val f  = Favorites as "f"

  object ArticleViews
      extends WithSQLDefinition[ArticleView](
        Composite(
          (
            a.c(_.id).sqlDef,
            a.c(_.slug).sqlDef,
            a.c(_.title).sqlDef,
            a.c(_.description).sqlDef,
            a.c(_.body).sqlDef,
            a.c(_.createdAt).sqlDef,
            a.c(_.updatedAt).sqlDef,
            a.c(_.authorId).sqlDef,
            au.c(_.username).sqlDef,
            au.c(_.bio).sqlDef,
            au.c(_.image).sqlDef
          )
        )(ArticleView.apply)(Tuple.fromProductTyped)
      )
  end ArticleViews

  private val articleViewFr =
    sql"SELECT ${ArticleViews} FROM $a INNER JOIN $au ON ${a.c(_.authorId)} = ${au.c(_.id)}"

  def list(
      query: ListArticleQuery,
      pagination: Pagination
  ): ConnectionIO[List[ArticleView]] =
    val q =
      articleViewFr
        ++ whereWithQuery(query)
        ++ recentWithPagination(pagination)
    q.queryOf(ArticleViews)
      .to[List]

  def listByFollowerId(
      followerId: UserId,
      pagination: Pagination
  ): ConnectionIO[List[ArticleView]] =
    val q =
      articleViewFr
        ++ whereWithFollower(followerId)
        ++ recentWithPagination(pagination)
    q.queryOf(ArticleViews)
      .to[List]

  def getBySlug(slug: Slug): ConnectionIO[Option[ArticleView]] =
    val q =
      articleViewFr ++ fr"WHERE ${a.c(_.slug) === slug}"
    q.queryOf(ArticleViews).option

  def listTotal(query: ListArticleQuery): ConnectionIO[Int] =
    val q =
      fr"SELECT COUNT(*) FROM $a INNER JOIN $au ON ${a.c(_.authorId)} = ${au.c(_.id)}" ++ whereWithQuery(
        query
      )
    q.query[Int].unique

  def listFeedTotal(followerId: UserId): ConnectionIO[Int] =
    val q =
      fr"SELECT COUNT(*) FROM $a INNER JOIN $au ON ${a.c(_.authorId)} = ${au.c(_.id)}" ++ whereWithFollower(
        followerId
      )
    q.query[Int].unique

  private def whereWithQuery(query: ListArticleQuery) =
    val tag = query.tag.map(tagName =>
      fr"${a.c(_.id)} IN (SELECT DISTINCT ${t.c(_.articleId)} FROM $t WHERE ${t.c(_.tag) === tagName}) "
    )
    val author = query.author.map(username => fr"${au.c(_.username) === username}")
    val favorited = query.favorited.map(username =>
      fr"${a.c(_.id)} IN (SELECT DISTINCT ${f.c(_.articleId)} FROM $f INNER JOIN $cu ON ${f
          .c(_.userId)} = ${cu.c(_.id)} WHERE ${cu.c(_.username) === username})"
    )
    whereAndOpt(tag, author, favorited)

  private def whereWithFollower(followerId: UserId) =
    fr"${a.c(_.authorId)} IN (SELECT DISTINCT ${fo.userId} FROM $fo WHERE ${fo.userId === followerId} "

  private def recentWithPagination(pagination: Pagination) =
    fr"""
    ORDER BY ${a.c(_.createdAt)} DESC
    LIMIT ${pagination.limit} OFFSET ${pagination.skip}
    """
end ArticleSQL
