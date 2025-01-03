package realworld.components.pages

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L.*
import com.raquo.laminar.modifiers.RenderableText
import com.raquo.laminar.nodes.ReactiveHtmlElement
import monocle.syntax.all.*
import org.scalajs.dom.{HTMLElement, MouseEvent}
import realworld.AppState
import realworld.api.*
import realworld.components.Component
import realworld.components.widgets.TagListWidget
import realworld.routes.{JsRouter, Page}
import realworld.spec.{Article, CommentBody, CommentId, CommentView, CreateCommentData, Profile, Slug}
import utils.Utils
import utils.Utils.{classTupleToClassName, some, someWriterF, toList, toSignal, writerF}

import scala.util.{Failure, Success, Try}

import concurrent.ExecutionContext.Implicits.global

case class CommentSectionState(
  comments: Option[List[CommentView]] = None,
  commentBody: String = "",
  submittingComment: Boolean = false
)
case class MetaSectionState(
  submittingFavorite: Boolean = false,
  submittingFollow: Boolean = false,
  deletingArticle: Boolean = false
)

final case class ArticleDetailPage(
  pageSignal: Signal[Page.ArticleDetailPage]
)(using
  state: AppState,
  api: Api
) extends Component:
  private val commentSectionVar = Var(CommentSectionState())
  private val commentBodyWriter = commentSectionVar.writerF(_.focus(_.commentBody).optic)
  private val commentsWriter    = commentSectionVar.writerF(_.focus(_.comments).optic)
  private val commentSubmittingWriter =
    commentSectionVar.writerF(_.focus(_.submittingComment).optic)
  private val metaSectionVar                   = Var(MetaSectionState())
  private val articleVar: Var[Option[Article]] = Var(None)
  private val authorWriter                     = articleVar.someWriterF(_.focus(_.author).optic)

  private val deleteCommentObserver = Observer[(Slug, CommentId)]: (slug, cid) =>
    api
      .promise(a =>
        for
          _ <- a.commentPromise.deleteComment(slug, cid, state.authHeader.get)
          listCommentResult <- a.commentPromise
            .listComments(slug, state.authHeader.get.some)
        yield listCommentResult.comments
      )
      .collect { case comments => commentsWriter.onNext(comments.some) }

  private val onLoad = pageSignal.flatMap { case Page.ArticleDetailPage(slug) =>
    api.promiseStream(a =>
      for
        articleOutput  <- a.articlePromise.getArticle(slug, state.authHeader)
        commentsOutput <- a.commentPromise.listComments(slug, state.authHeader)
      yield (articleOutput.article, commentsOutput.comments)
    )
  }.recoverToTry --> Observer[Try[(Article, List[CommentView])]]:
    case Failure(exception) => JsRouter.redirectTo(Page.Home)
    case Success(article, comments) =>
      state.titleWriter.onNext(s"Conduit: ${article.title}")
      Var.set(
        articleVar        -> article.some,
        commentSectionVar -> CommentSectionState(comments = comments.some)
      )

  private def display() =
    articleVar.signal
      .splitOption(
        (article, articleSignal) =>
          div(
            cls := "article-page",
            articlePageBanner(article, articleSignal),
            div(
              cls := "container page",
              div(
                cls := "row article-content",
                div(cls := "col-md-12", article.body, whiteSpace.preWrap),
                TagListWidget(article.tagList)
              )
            ),
            hr(),
            div(cls := "article-actions", articleMeta(article, articleSignal)),
            commentSection()
          ),
        ifEmpty = div("Loading article...")
      )

  def body: HtmlElement =
    div(
      onLoad,
      child <-- display()
    )

  def articlePageBanner(article: Article, articleSignal: Signal[Article]) =
    div(
      cls := "banner",
      div(
        cls := "container",
        h1(article.title),
        articleMeta(article, articleSignal)
      )
    )

  def articleMeta(article: Article, articleSignal: Signal[Article]) =
    div(
      cls := "article-meta",
      articleAuthorInfo(article),
      state.user match
        case Some(u) if u.username == article.author.username => ownerArticleMetaActions(article)
        case _                                                => viewerArticleMetaActions(article, articleSignal)
    )

  def ownerArticleMetaActions(article: Article): Seq[Modifier[ReactiveHtmlElement[HTMLElement]]] =
    List(
      button(
        cls := "btn btn-outline-secondary btn-sm",
        JsRouter.navigateTo(Page.EditArticlePage(article.slug)),
        i(cls := "ion-plus-round"),
        " Edit Article"
      ),
      " ",
      button(
        cls := "btn btn-outline-danger btn-sm",
        disabled <-- metaSectionVar.signal.map(_.deletingArticle),
        i(cls := "ion-heart"),
        " Delete Article",
        onClick.preventDefault --> { _ =>
          metaSectionVar.update(_.copy(deletingArticle = true))
          state.authHeader.collect { case header =>
            api
              .promise(_.articlePromise.deleteArticle(auth = header, slug = article.slug))
              .onComplete {
                case Failure(exception) => JsRouter.redirectTo(Page.Login)
                case Success(value)     => JsRouter.redirectTo(Page.Home)
              }
          }
        }
      )
    )

  def viewerArticleMetaActions(
    article: Article,
    articleSignal: Signal[Article]
  ): Seq[Modifier[ReactiveHtmlElement[HTMLElement]]] =
    val followingVar    = Var(article.author.following)
    val favoritedVar    = Var(article.favorited)
    val followingSignal = articleSignal.map(_.author.following)
    val favoritedSignal = articleSignal.map(_.favorited)
    val onFollowObserver = Observer[MouseEvent]: _ =>
      state.authHeader.fold(JsRouter.redirectTo(Page.Register)) { case header =>
        metaSectionVar.update(_.copy(submittingFollow = true))
        api
          .promise(a =>
            if followingVar.now() then
              a.userPromise.unfollowUser(article.author.username, header).map(_.profile)
            else a.userPromise.followUser(article.author.username, header).map(_.profile)
          )
          .onComplete {
            case Failure(exception) => JsRouter.redirectTo(Page.Login)
            case Success(profile) =>
              authorWriter.onNext(profile)
              metaSectionVar.update(_.copy(submittingFollow = false))
          }
      }

    def onFavorite(_e: MouseEvent) =
      state.authHeader.fold(JsRouter.redirectTo(Page.Register)) { case header =>
        metaSectionVar.update(_.copy(submittingFavorite = true))
        api
          .promise(a =>
            if favoritedVar.now() then
              a.articlePromise.unfavoriteArticle(header, article.slug).map(_.article)
            else a.articlePromise.favoriteArticle(header, article.slug).map(_.article)
          )
          .onComplete {
            case Failure(exception) => JsRouter.redirectTo(Page.Login)
            case Success(article) =>
              articleVar.set(article.some)
              metaSectionVar.update(_.copy(submittingFavorite = false))
          }
      }
    List(
      favoritedSignal --> favoritedVar.writer,
      followingSignal --> followingVar.writer,
      button(
        cls := "btn btn-sm",
        cls <-- followingSignal.map { following =>
          classTupleToClassName(
            "btn-outline-secondary" -> !following,
            "btn-secondary"         -> following
          )
        },
        disabled <-- metaSectionVar.signal.map(_.submittingFollow),
        i(cls := "ion-plus-round"),
        child.text <-- followingSignal
          .combineWith(articleSignal)
          .map((following, article) =>
            s" ${if following then "Unfollow" else "Follow"} ${article.author.username}"
          ),
        onClick.preventDefault --> onFollowObserver
      ),
      " ",
      button(
        cls := "btn btn-sm",
        cls <-- favoritedSignal.map { favorited =>
          classTupleToClassName(
            "btn-outline-primary" -> !favorited,
            "btn-primary"         -> favorited
          )
        },
        disabled <-- metaSectionVar.signal.map(_.submittingFavorite),
        i(cls := "ion-heart"),
        child.text <-- favoritedSignal.map(favorited =>
          s" ${if favorited then "Unfavorite" else "Favorite"} Article"
        ),
        span(cls := "counter", child.text <-- articleSignal.map(_.favoritesCount.value)),
        onClick.preventDefault --> onFavorite
      )
    )
  end viewerArticleMetaActions

  def articleAuthorInfo(article: Article) =
    import typings.dateFns.formatMod
    val profilePage = Page.ProfilePage(article.author.username)
    List(
      img(JsRouter.navigateTo(profilePage)),
      div(
        cls := "info",
        a(cls    := "author", JsRouter.navigateTo(profilePage)),
        span(cls := "date", formatMod.format(article.createdAt.value.toDate, "PP"))
      )
    )

  def commentSection() =
    div(
      cls := "row",
      div(
        cls := "col-xs-12 col-md-8 offset-md-2",
        state.user match
          case None =>
            p(
              styleAttr := "display: inherit",
              a(JsRouter.navigateTo(Page.Login), "Sign in"),
              " or ",
              a(JsRouter.navigateTo(Page.Register), "sign up"),
              " to add comments on this article."
            )
          case Some(value) =>
            commentForm()
        ,
        children <-- commentSectionVar.signal
          .distinctBy(_.comments)
          .map(_.comments)
          .splitOption(
            (_, commentsSignal) => commentsSignal.split(_.id)(articleComment),
            ifEmpty = div("Loading comments...").toList.toSignal
          )
          .flatten
      )
    )

  import typings.dateFns.formatMod
  def articleComment(cid: CommentId, comment: CommentView, commentSignal: Signal[CommentView]) =
    div(
      cls := "card",
      div(
        cls := "card-block",
        p(cls := "card-text", child.text <-- commentSignal.map(_.body.value))
      ),
      div(
        cls := "card-footer",
        a(
          cls := "comment-author",
          JsRouter.navigateTo(Page.ProfilePage(comment.author.username)),
          img(
            cls := "comment-author-img",
            src := s"${comment.author.image.getOrElse(Utils.defaultAvatarUrl)}"
          )
        ),
        " ",
        a(
          cls := "comment-author",
          JsRouter.navigateTo(Page.ProfilePage(comment.author.username)),
          comment.author.username
        ),
        span(cls := "date-posted", formatMod.format(comment.createdAt.value.toDate, "PP")),
        state.user match
          case Some(u) if u.username == comment.author.username =>
            span(
              cls := "mod-options",
              i(
                cls        := "ion-trash-a",
                aria.label := "Delete comment",
                onClick.preventDefault
                  .mapTo(articleVar.now().map(_.slug).get -> cid) --> deleteCommentObserver
              )
            )
          case _ => Mod.empty
      )
    )

  def commentForm() =
    form(
      cls := "card comment-form",
      div(
        cls := "card-block",
        textArea(
          cls         := "form-control",
          placeholder := "Write a comment...",
          rows        := 3,
          controlled(
            value <-- commentSectionVar.signal.map(_.commentBody),
            onInput.mapToValue --> commentBodyWriter
          )
        )
      ),
      div(
        cls := "card-footer",
        img(src := s"${state.user.flatMap(_.image).getOrElse(Utils.defaultAvatarUrl)}"),
        button(
          cls := "btn btn-sm btn-primary",
          "Post Comment",
          disabled <-- commentSectionVar.signal.map(_.submittingComment)
        )
      ),
      onSubmit.preventDefault.mapTo(commentSectionVar.now().commentBody) --> Observer[String] {
        comment =>
          commentSubmittingWriter.onNext(true)
          state.authHeader.fold(JsRouter.redirectTo(Page.Login))(authHeader =>
            api
              .promise(a =>
                val slug = articleVar.now().get.slug
                for
                  _ <- a.commentPromise
                    .createComment(
                      slug,
                      CreateCommentData(CommentBody(comment)),
                      authHeader
                    )
                  listCommentsResult <- a.commentPromise.listComments(slug, authHeader.some)
                yield listCommentsResult.comments
              )
              .collect { comments =>
                commentSubmittingWriter.onNext(false)
                commentsWriter.onNext(comments.some)
              }
          )
      }
    )

end ArticleDetailPage
