package realworld.repo

import cats.data.{NonEmptyList, OptionT}
import cats.effect.*
import cats.syntax.all.*
import doobie.*
import doobie.Fragments.*
import doobie.implicits.*
import org.typelevel.log4cats.Logger
import realworld.db.{*, given}
import realworld.domain.{Tag, WithId, WithTotal}
import realworld.domain.article.*
import realworld.domain.user.UserId
import realworld.http.Pagination
import realworld.spec.{Bio, CreatedAt, Slug, UpdateArticleData, UpdatedAt}
import realworld.types.TagName
import cats.~>

trait ArticleRepo[F[_]]:
  def list(
    query: ListArticleQuery,
    pagination: Pagination
  ): F[WithTotal[List[ArticleView]]]
  def listByFollowerId(followerId: UserId, pagination: Pagination): F[WithTotal[List[ArticleView]]]
  def findBySlug(slug: Slug): F[Option[ArticleView]]
  def create(article: WithId[ArticleId, Article], tags: List[TagName]): F[ArticleView]
  def update(
    data: UpdateArticleData,
    newSlug: Slug,
    oldSlug: Slug,
    updatedAt: UpdatedAt,
    authorId: UserId
  ): F[Option[ArticleView]]
  def delete(slug: Slug, authorId: UserId): F[Option[Unit]]
end ArticleRepo

object ArticleRepo:
  import ArticleSQL as A
  def make[F[_]: MonadCancelThrow: DoobieTx: Logger](xa: Transactor[F]): ArticleRepo[F] =
    new:
      def list(
        query: ListArticleQuery,
        pagination: Pagination
      ): F[WithTotal[List[ArticleView]]] =
        val result =
          for
            articles <- A.list(query, pagination)
            total    <- A.listTotal(query)
          yield WithTotal(total, articles)
        result.transact(xa)

      def listByFollowerId(
        followerId: UserId,
        pagination: Pagination
      ): F[WithTotal[List[ArticleView]]] =
        val result =
          for
            articles <- A.listByFollowerId(followerId, pagination)
            total    <- A.listFeedTotal(followerId)
          yield WithTotal(total, articles)
        result.transact(xa)

      def findBySlug(slug: Slug): F[Option[ArticleView]] = A.selectBySlug(slug).transact(xa)

      def create(article: WithId[ArticleId, Article], tags: List[TagName]): F[ArticleView] =
        val trx =
          for
            _       <- A.insertRow().run(article)
            _       <- A.insertTag().updateMany(tags.map(tag => Tag(article.id, tag)))
            article <- A.selectById(article.id)
          yield article
        trx.transact(xa)

      def update(
        data: UpdateArticleData,
        newSlug: Slug,
        oldSlug: Slug,
        updatedAt: UpdatedAt,
        authorId: UserId
      ): F[Option[ArticleView]] =
        xa.transaction.use { f =>
          given ConnectionIO ~> F = f
          val r =
            for
              article <- OptionT[F, ArticleView](A.selectBySlug(oldSlug))
              _       <- OptionT.liftF[F, Int](A.update(data, newSlug, article.id, updatedAt, authorId))
              _       <- OptionT.liftF[F, Int](A.deleteTagByArticleId(article.id))
              _ <- OptionT.liftF[F, Int](
                A.insertTag().updateMany(data.tagList.map(t => Tag(article.id, t)))
              )
              article <- OptionT[F, ArticleView](A.selectById(article.id).map(_.some))
            yield article
          r.value
        }
      end update

      def delete(slug: Slug, authorId: UserId): F[Option[Unit]] =
        A.deleteBySlug(slug, authorId).transact(xa)
end ArticleRepo

private object ArticleSQL:
  import realworld.domain.Favorites
  import realworld.domain.Tags
  import realworld.domain.article.Articles
  import realworld.domain.follower.Followers as fo
  import realworld.domain.given
  import realworld.domain.user.Users

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
    sql"""
    SELECT ${ArticleViews} FROM $a INNER JOIN $au ON ${a.c(_.authorId)} = ${au.c(_.id)} 
    """

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

  def selectBySlug(slug: Slug): ConnectionIO[Option[ArticleView]] =
    val q =
      articleViewFr ++ fr"""
      WHERE ${a.c(_.slug) === slug}
      """
    q.queryOf(ArticleViews).option

  def selectById(id: ArticleId): ConnectionIO[ArticleView] =
    val q =
      articleViewFr ++ fr"""
      WHERE ${a.c(_.id) === id}
      """
    q.queryOf(ArticleViews).unique

  def listTotal(query: ListArticleQuery): ConnectionIO[Int] =
    val q =
      fr"""
      SELECT COUNT(*) FROM $a INNER JOIN $au ON ${a.c(_.authorId)} = ${au.c(_.id)}
      """ ++ whereWithQuery(
          query
        )
    q.query[Int].unique

  def listFeedTotal(followerId: UserId): ConnectionIO[Int] =
    val q =
      fr"""
      SELECT COUNT(*) FROM $a INNER JOIN $au ON ${a.c(_.authorId)} = ${au.c(_.id)}
      """ ++ whereWithFollower(
          followerId
        )
    q.query[Int].unique

  def insertRow() =
    Articles.rowCol.insert

  def insertTag() =
    Tags.rowCol.insert

  def deleteTagByArticleId(aid: ArticleId) =
    sql"""
    DELETE FROM $t WHERE ${t.c(_.articleId)} = $aid
    """.update.run

  import Articles as ar
  def update(
    data: UpdateArticleData,
    newSlug: Slug,
    articleId: ArticleId,
    updatedAt: UpdatedAt,
    authorId: UserId
  ): ConnectionIO[Int] =
    val titleOpt       = data.title.map(t => ar.title --> t)
    val descriptionOpt = data.description.map(d => ar.description --> d)
    val bodyOpt        = data.body.map(b => ar.body --> b)

    val fields      = List(titleOpt, descriptionOpt, bodyOpt).flatten
    val otherFields = NonEmptyList(ar.updatedAt --> updatedAt, List(ar.slug --> newSlug))

    if fields.isEmpty then 0.pure[ConnectionIO]
    else sql"""
          ${updateTable[NonEmptyList](ar, otherFields ++ fields)} 
          WHERE ${ar.authorId === authorId} AND ${ar.id === articleId}
          """.update.run
  end update

  def deleteBySlug(slug: Slug, authorId: UserId): ConnectionIO[Option[Unit]] =
    val q =
      sql"""
    DELETE FROM $ar WHERE ${ar.slug === slug} AND ${ar.authorId === authorId}
    """
    q.update.run.map(affectedToOption)

  private def whereWithQuery(query: ListArticleQuery) =
    val tag = query.tag.map(tagName => fr"""
      ${a.c(_.id)} IN (SELECT DISTINCT ${t.c(_.articleId)} FROM $t WHERE ${t.c(_.tag) === tagName}) 
      """)
    val author = query.author.map(username => fr" ${au.c(_.username) === username} ")
    val favorited = query.favorited.map(username =>
      fr"""
      ${a.c(_.id)} IN (SELECT DISTINCT ${f.c(_.articleId)} FROM $f INNER JOIN $cu ON ${f
          .c(_.userId)} = ${cu.c(_.id)} WHERE ${cu.c(_.username) === username})
          """
    )
    whereAndOpt(tag, author, favorited)
  end whereWithQuery

  private def whereWithFollower(followerId: UserId) =
    fr"""
    WHERE ${a.c(
        _.authorId
      )} IN (SELECT DISTINCT ${fo.userId} FROM $fo WHERE ${fo.followerId === followerId} )
    """

  private def recentWithPagination(pagination: Pagination) =
    fr"""
    ORDER BY ${a.c(_.createdAt)} DESC
    LIMIT ${pagination.limit} OFFSET ${pagination.skip}
    """
end ArticleSQL
