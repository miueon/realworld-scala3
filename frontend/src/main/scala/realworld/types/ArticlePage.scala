package realworld.types

import realworld.spec.Article
import realworld.spec.Total
import realworld.spec.ListFeedArticleOutput
import realworld.spec.ListArticleOutput
import utils.Utils.some

case class ArticlePage(
    articleCount: Total = Total(0),
    articles: Option[List[Article]] = None
)
object ArticlePage:
  extension (a: ListFeedArticleOutput | ListArticleOutput)
    def toPage = a match
      case ListFeedArticleOutput(count, articles) =>
        ArticlePage(count, articles.some)
      case ListArticleOutput(articlesCount, articles) =>
        ArticlePage(articlesCount, articles.some)
