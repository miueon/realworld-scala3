package realworld.service

import realworld.domain.user.UserId
import realworld.domain.article.ListArticleQuery
import realworld.http.Pagination
import realworld.domain.WithTotal
import realworld.spec.ArticleList
import realworld.repo.ArticleRepo
import realworld.repo.UserRepo
import realworld.repo.FavoriteRepo
import realworld.repo.TagRepo
import cats.Monad
import cats.syntax.all.*
import realworld.repo.FollowerRepo
import realworld.domain.article.ArticleView
import realworld.domain.article.ArticleId
import realworld.spec.TagName
import realworld.spec.FavoritesCount
import realworld.domain.Favorite
import realworld.domain.Tag
import realworld.domain.follower.Follower
import realworld.spec.Profile

trait Articles[F[_]]:
  def list(
      uid: Option[UserId],
      query: ListArticleQuery,
      pagination: Pagination
  ): F[WithTotal[ArticleList]]

  def listFeed(uid: UserId, pagination: Pagination): F[WithTotal[ArticleList]]

object Articles:
  def make[F[_]: Monad](
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
            .traverse(followerRepo.findFollowers(authorIds, _))
            .map(_.getOrElse(List.empty))
          articleExtras <- articlesExtras(uidOpt, articleIds)
        yield articles.map(_.toArticleList(followers, articleExtras))
        result
      end list

      def listFeed(uid: UserId, pagination: Pagination): F[WithTotal[ArticleList]] = 
        
        ???

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
            tags <- tagRepo.findTags(articleIds)
            favorites <- uidOpt
              .traverse(favRepo.findFavorites(articleIds, _))
              .map(_.getOrElse(List.empty))
            favCountMap <- favRepo.favoriteCountIdMap(articleIds)
          yield (tags.groupBy(_.articleId), favorites.groupBy(_.articleId), favCountMap)
        extraMaps.map:
          case (tagMap, favByMap, favCountMap) =>
            articleIds.map(withExtra(_, tagMap, favByMap, favCountMap)).toMap
      end articlesExtras

      extension (a: List[ArticleView])
        def toArticleList(
            followers: List[Follower],
            articleExtras: Map[ArticleId, (List[TagName], Boolean, FavoritesCount)]
        ): ArticleList =
          val followingMap = followers.groupBy(_.userId)
          val aricles = a.map { article =>
            val (tags, isFavorited, favoritesCount) = articleExtras(article.id)
            realworld.spec.Article(
              article.slug,
              article.title,
              article.description,
              article.body,
              article.createdAt,
              article.updatedAt,
              Profile(
                article.authorName,
                followingMap.get(article.authorId).nonEmpty,
                article.authorBio,
                article.authorImage
              ),
              tags,
              isFavorited,
              favoritesCount
            )
          }
          ArticleList(aricles)

end Articles
