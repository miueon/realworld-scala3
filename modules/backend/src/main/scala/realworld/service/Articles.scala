package realworld.service

import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*
import io.github.arainko.ducktape.*
import org.typelevel.log4cats.Logger
import realworld.domain.{Favorite, ID, Tag, WithId, WithTotal}
import realworld.domain.article.{ArticleError, ArticleId, ArticleView, ListArticleQuery}
import realworld.domain.follower.Follower
import realworld.domain.user.UserId
import realworld.effects.{GenUUID, Time}
import realworld.http.Pagination
import realworld.repo.{ArticleRepo, FavoriteRepo, FollowerRepo, TagRepo}
import realworld.spec.{
  Article, ArticleList, CreateArticleData, CreatedAt, FavoritesCount, Profile, Slug, UpdateArticleData, UpdatedAt
}
import realworld.types.{TagName, Title}
import smithy4s.Timestamp

import java.text.Normalizer
import java.text.Normalizer.Form
import scala.util.chaining.*

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
  def unfavoriteArticle(slug: Slug, uid: UserId): F[Article]

object Articles:
  def make[F[_]: MonadCancelThrow: GenUUID: Logger: Time](
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
        for
          articlesWithTotal <- articleRepo.list(query, pagination)
          authorIds  = articlesWithTotal.entity.map(_.authorId)
          articleIds = articlesWithTotal.entity.map(_.id)
          followers <- uidOpt
            .traverse(followerRepo.listFollowers(authorIds, _))
            .map(_.getOrElse(List.empty))
          articleExtras <- articlesExtras(uidOpt, articleIds)
        yield articlesWithTotal.map(articleList(followers, articleExtras))
      end list

      def listFeed(uid: UserId, pagination: Pagination): F[WithTotal[ArticleList]] =
        val result =
          for
            articles <- articleRepo.listByFollowerId(uid, pagination)
            authorIds  = articles.entity.map(_.authorId)
            articleIds = articles.entity.map(_.id)
            followers     <- authorIds.map(Follower(_, uid)).pure
            articleExtras <- articlesExtras(Some(uid), articleIds)
          yield articles.map(articleList(followers, articleExtras))
        result

      def getBySlug(uidOpt: Option[UserId], slug: Slug): F[Article] =
        val article =
          for
            article <- OptionT(articleRepo.findBySlug(slug))
            following <- OptionT.liftF(
              uidOpt.flatTraverse(followerRepo.findFollower(article.authorId, _)).map(_.nonEmpty)
            )
            extras <- OptionT.liftF(articleExtra(uidOpt, article.id))
          yield articleViewToArticle(following, extras)(article)
        article.value.flatMap {
          case None        => ArticleError.NotFound(slug).raiseError
          case Some(value) => value.pure
        }
      end getBySlug

      def create(uid: UserId, data: CreateArticleData): F[Article] =
        for
          articleId <- ID.make[F, ArticleId]
          timestamp <- Time[F].timestamp
          row = WithId(
            articleId,
            data
              .into[realworld.domain.article.Article]
              .transform(
                Field.const(_.slug, mkSlug(data.title, timestamp)),
                Field.const(_.createdAt, CreatedAt(timestamp)),
                Field.const(_.updatedAt, UpdatedAt(timestamp)),
                Field.const(_.authorId, uid)
              )
          )
          article <- articleRepo.create(row, data.tagList)
        yield articleViewToArticle(false, (data.tagList, false, FavoritesCount(0)))(article)
      end create

      def update(slug: Slug, uid: UserId, data: UpdateArticleData): F[Article] =
        val result =
          for
            timestamp <- OptionT.liftF(Time[F].timestamp)
            newSlug = data.title.map(mkSlug(_, timestamp)).getOrElse(slug)
            article <- OptionT(
              articleRepo.update(data, newSlug, slug, UpdatedAt(timestamp), uid)
            )
            extra <- OptionT.liftF(articleExtra(uid.some, article.id))
          yield articleViewToArticle(false, extra)(article)

        result.value.flatMap:
          case None        => ArticleError.NotFound(slug).raiseError
          case Some(value) => value.pure
      end update

      def delete(slug: Slug, uid: UserId): F[Unit] =
        articleRepo
          .delete(slug, uid)
          .flatMap:
            case None     => ArticleError.NotFound(slug).raiseError
            case Some(()) => ().pure

      def favoriteArticle(slug: Slug, uid: UserId): F[Article] =
        favOrUnfav(slug, uid)(favRepo.create)

      def unfavoriteArticle(slug: Slug, uid: UserId): F[Article] =
        favOrUnfav(slug, uid)(favRepo.delete)

      private def favOrUnfav[A](slug: Slug, uid: UserId)(f: Favorite => F[A]): F[Article] =
        val article =
          for
            article <- OptionT(articleRepo.findBySlug(slug))
            _       <- OptionT.liftF(f(Favorite(article.id, uid)))
            following <- OptionT.liftF(
              followerRepo.findFollower(article.authorId, uid).map(_.nonEmpty)
            )
            extras <- OptionT.liftF(articleExtra(uid.some, article.id))
          yield articleViewToArticle(following, extras)(article)

        article.value.flatMap:
          case None        => ArticleError.NotFound(slug).raiseError
          case Some(value) => value.pure
      end favOrUnfav

      val notAsciiRe = "[^\\p{ASCII}]".r
      val notWordsRs = "[^\\w]".r
      val spacesRe   = "\\s+".r
      def slugify(s: String): String =
        // get rid of fancy characters accents
        val normalized = Normalizer.normalize(s, Form.NFD)
        val cleaned    = notAsciiRe.replaceAllIn(normalized, "")

        val wordsOnly = notWordsRs.replaceAllIn(cleaned, " ").trim
        spacesRe.replaceAllIn(wordsOnly, "-").toLowerCase
      private def mkSlug(title: Title, nowTime: Timestamp): Slug =
        Slug(s"${slugify(title)}-${nowTime.conciseDateTime}")

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

      private def articleList(
        followers: List[Follower],
        articleExtras: Map[ArticleId, (List[TagName], Boolean, FavoritesCount)]
      )(
        articles: List[ArticleView]
      ): ArticleList =
        articles
          .map { article =>
            val followingMap = followers.groupBy(_.userId)
            articleViewToArticle(
              followingMap.get(article.authorId).nonEmpty,
              articleExtras(article.id)
            )(article)
          }
          .pipe(ArticleList.apply)

      private def articleViewToArticle(
        following: Boolean,
        articleExtra: (List[TagName], Boolean, FavoritesCount)
      )(
        article: ArticleView
      ): Article =
        val (tags, isFavorited, favoritesCount) = articleExtra
        article
          .into[Article]
          .transform(
            Field.const(_.favoritesCount, favoritesCount),
            Field.const(_.favorited, isFavorited),
            Field.const(_.tagList, tags),
            Field.const(
              _.author,
              Profile(
                article.authorName,
                following,
                article.authorBio,
                article.authorImage
              )
            )
          )
      end articleViewToArticle
end Articles
