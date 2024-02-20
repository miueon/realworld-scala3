package realworld.service

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*

import realworld.domain.Favorite
import realworld.domain.Tag
import realworld.domain.WithTotal
import realworld.domain.article.ArticleError
import realworld.domain.article.ArticleId
import realworld.domain.article.ArticleView
import realworld.domain.article.ListArticleQuery
import realworld.domain.follower.Follower
import realworld.domain.user.UserId
import realworld.http.Pagination
import realworld.repo.ArticleRepo
import realworld.repo.FavoriteRepo
import realworld.repo.FollowerRepo
import realworld.repo.TagRepo
import realworld.repo.UserRepo
import realworld.spec.Article
import realworld.spec.ArticleList
import realworld.spec.CreatedAt
import realworld.spec.FavoritesCount
import realworld.spec.Profile
import realworld.spec.Slug
import realworld.spec.TagName
import realworld.spec.CreateArticleData
import realworld.effects.GenUUID
import realworld.domain.ID
import realworld.domain.WithId
import realworld.spec.Title
import java.time.Instant
import smithy4s.Timestamp
import realworld.spec.UpdatedAt
import realworld.spec.UpdateArticleData

trait Articles[F[_]]:
  def list(
      uid: Option[UserId],
      query: ListArticleQuery,
      pagination: Pagination
  ): F[WithTotal[ArticleList]]
  def listFeed(uid: UserId, pagination: Pagination): F[WithTotal[ArticleList]]
  def getBySlug(uidOpt: Option[UserId], slug: Slug): F[Article]
  def create(uid: UserId, data: CreateArticleData): F[Article]
  def update(slug: Slug, uid: UserId, data: UpdateArticleData): F[Article]
  def delete(slug: Slug, uid: UserId): F[Unit]
  def favoriteArticle(slug: Slug, uid: UserId): F[Article]

object Articles:
  def make[F[_]: MonadCancelThrow: GenUUID](
      articleRepo: ArticleRepo[F],
      favRepo: FavoriteRepo[F],
      tagRepo: TagRepo[F],
      followerRepo: FollowerRepo[F]
  ): Articles[F] =
    new:
      def list(
          uidOpt: Option[UserId],
          query: ListArticleQuery,
          pagination: Pagination
      ): F[WithTotal[ArticleList]] =
        val result = for
          articles <- articleRepo.list(query, pagination)
          authorIds  = articles.entity.map(_.authorId)
          articleIds = articles.entity.map(_.id)
          followers <- uidOpt
            .traverse(followerRepo.listFollowers(authorIds, _))
            .map(_.getOrElse(List.empty))
          articleExtras <- articlesExtras(uidOpt, articleIds)
        yield articles.map(_.toArticleList(followers, articleExtras))
        result
      end list

      def listFeed(uid: UserId, pagination: Pagination): F[WithTotal[ArticleList]] =
        val result = for
          articles <- articleRepo.listByFollowerId(uid, pagination)
          authorIds  = articles.entity.map(_.authorId)
          articleIds = articles.entity.map(_.id)
          followers     <- authorIds.map(Follower(_, uid)).pure
          articleExtras <- articlesExtras(Some(uid), articleIds)
        yield articles.map(_.toArticleList(followers, articleExtras))
        result

      def getBySlug(uidOpt: Option[UserId], slug: Slug): F[Article] =
        val article = for
          article <- OptionT(articleRepo.findBySlug(slug))
          following <- OptionT.liftF(
            uidOpt.flatTraverse(followerRepo.findFollower(article.authorId, _)).map(_.nonEmpty)
          )
          extras <- OptionT.liftF(articleExtra(uidOpt, article.id))
        yield article.toArticleOutput(following, extras)
        article.value.flatMap {
          case None        => ArticleError.NotFound(slug).raiseError
          case Some(value) => value.pure
        }

      def create(uid: UserId, data: CreateArticleData): F[Article] =
        def articleWithId(
            id: ArticleId,
            nowTime: Instant
        ): WithId[ArticleId, realworld.domain.article.Article] =
          WithId(
            id,
            realworld.domain.article.Article(
              mkSlug(data.title, uid, nowTime),
              data.title,
              data.description,
              data.body,
              CreatedAt(nowTime.toTimestamp),
              UpdatedAt(nowTime.toTimestamp),
              uid
            )
          )
        val now = Instant.now()
        for
          articleId <- ID.make[F, ArticleId]
          row = articleWithId(articleId, now)
          article <- articleRepo.create(row, data.tagList)
        yield article.toArticleOutput(false, (data.tagList, false, FavoritesCount(0)))
      end create

      def update(slug: Slug, uid: UserId, data: UpdateArticleData): F[Article] =
        val now     = Instant.now()
        val newSlug = data.title.map(mkSlug(_, uid, now)).getOrElse(slug)

        val result = for
          article <- OptionT(articleRepo.update(data, newSlug, UpdatedAt(now.toTimestamp), uid))
          extra   <- OptionT.liftF(articleExtra(uid.some, article.id))
        yield article.toArticleOutput(false, extra)

        result.value.flatMap:
          case None        => ArticleError.NotFound(slug).raiseError
          case Some(value) => value.pure

      def delete(slug: Slug, uid: UserId): F[Unit] =
        articleRepo
          .delete(slug, uid)
          .flatMap:
            case None     => ArticleError.NotFound(slug).raiseError
            case Some(()) => ().pure

      def favoriteArticle(slug: Slug, uid: UserId): F[Article] =
        val article =
          for
            article <- OptionT(articleRepo.findBySlug(slug))
            _       <- OptionT.liftF(favRepo.create(Favorite(article.id, uid)))
            following <- OptionT.liftF(
              followerRepo.findFollower(article.authorId, uid).map(_.nonEmpty)
            )
          yield ???

        ???

      private def mkSlug(title: Title, authorId: UserId, nowTime: Instant): Slug =
        Slug(s"${title.value}-${authorId.value}-${nowTime}")

      private def articlesExtras(
          uidOpt: Option[UserId],
          articleIds: List[ArticleId]
      ): F[Map[ArticleId, (List[TagName], Boolean, FavoritesCount)]] =
        def withExtra(
            id: ArticleId,
            tagMap: Map[ArticleId, List[Tag]],
            favoriteByMap: Map[ArticleId, List[Favorite]],
            favoriteCountMap: Map[ArticleId, FavoritesCount]
        ): (ArticleId, (List[TagName], Boolean, FavoritesCount)) =
          val tags          = tagMap.getOrElse(id, List.empty).map(_.tag)
          val isFavorited   = favoriteByMap.get(id).nonEmpty
          val favoriteCount = favoriteCountMap.getOrElse(id, FavoritesCount(0))
          id -> (tags, isFavorited, favoriteCount)
        val extraMaps =
          for
            tags <- tagRepo.listTag(articleIds)
            favorites <- uidOpt
              .traverse(favRepo.listFavorite(articleIds, _))
              .map(_.getOrElse(List.empty))
            favCountMap <- favRepo.favoriteCountIdMap(articleIds)
          yield (tags.groupBy(_.articleId), favorites.groupBy(_.articleId), favCountMap)
        extraMaps.map:
          case (tagMap, favByMap, favCountMap) =>
            articleIds.map(withExtra(_, tagMap, favByMap, favCountMap)).toMap
      end articlesExtras

      def articleExtra(
          uidOpt: Option[UserId],
          articleId: ArticleId
      ): F[(List[TagName], Boolean, FavoritesCount)] =
        for
          tags        <- tagRepo.listTagsById(articleId)
          isFavorited <- uidOpt.flatTraverse(favRepo.findFavorite(articleId, _)).map(_.nonEmpty)
          favCount    <- favRepo.favoriteCount(articleId)
        yield (tags.map(_.tag), isFavorited, favCount)

      extension (a: List[ArticleView])
        def toArticleList(
            followers: List[Follower],
            articleExtras: Map[ArticleId, (List[TagName], Boolean, FavoritesCount)]
        ): ArticleList =
          val followingMap = followers.groupBy(_.userId)
          val aricles = a.map { article =>
            article.toArticleOutput(
              followingMap.get(article.authorId).nonEmpty,
              articleExtras(article.id)
            )
          }
          ArticleList(aricles)

      extension (a: ArticleView)
        def toArticleOutput(
            following: Boolean,
            articleExtra: (List[TagName], Boolean, FavoritesCount)
        ): Article =
          val (tags, isFavorited, favoritesCount) = articleExtra
          Article(
            a.slug,
            a.title,
            a.description,
            a.body,
            a.createdAt,
            a.updatedAt,
            Profile(
              a.authorName,
              following,
              a.authorBio,
              a.authorImage
            ),
            tags,
            isFavorited,
            favoritesCount
          )

end Articles
