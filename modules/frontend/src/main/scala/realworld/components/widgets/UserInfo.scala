package realworld.components.widgets

import com.raquo.laminar.api.L.*
import io.github.iltotore.iron.*
import monocle.syntax.all.*
import org.scalajs.dom.MouseEvent
import realworld.AppState
import realworld.api.*
import realworld.components.Component
import realworld.routes.{JsRouter, Page}
import realworld.spec.Profile
import utils.Utils.writerF

import scala.util.{Failure, Success}

import concurrent.ExecutionContext.Implicits.global
case class UserInfoState(
    isSubmitting: Boolean = false,
    isFollowing: Boolean = false,
    username: String = ""
)
final case class UserInfo(profileSignal: Signal[Profile], profileObserver: Observer[Profile])(using
    state: AppState,
    api: Api
) extends Component:
  val userInfoStateVar = Var(UserInfoState())
  val userInfoProfileUpdater = userInfoStateVar.updater[Profile] { (state, cur) =>
    state.copy(isFollowing = cur.following, username = cur.username)
  }
  val isSubmittingWriter = userInfoStateVar.writerF(_.focus(_.isSubmitting).optic)
  def editProfileButton() =
    button(
      cls := "btn btn-sm btn-outline-secondary action-btn",
      i(cls := "ion-gear-a", " Edit Profile Settings"),
      JsRouter.navigateTo(Page.Setting)
    )

  def toggleFollowButton(
  ) =
    val onFollowObserver = Observer[MouseEvent] { _ =>
      state.authHeader.fold(JsRouter.redirectTo(Page.Register)) { case header =>
        isSubmittingWriter.onNext(true)
        val userInfoState = userInfoStateVar.now()
        {
          if userInfoState.isFollowing then
            api.promise(
              _.userPromise.unfollowUser(userInfoState.username.assume, header).map(_.profile)
            )
          else
            api
              .promise(
                _.userPromise.followUser(userInfoState.username.assume, header).map(_.profile)
              )
        }
          .onComplete {
            case Failure(exception) => JsRouter.redirectTo(Page.Login)
            case Success(profile) =>
              profileObserver.onNext(profile)
              isSubmittingWriter.onNext(false)
          }
      }
    }
    button(
      cls := "btn btn-sm btn-outline-secondary action-btn",
      disabled <-- userInfoStateVar.signal.map(_.isSubmitting),
      i(
        cls := "ion-plus-round",
        s" ${if userInfoStateVar.now().isFollowing then "Unfollow" else "Follow"} ${userInfoStateVar.now().username}"
      ),
      onClick.preventDefault --> onFollowObserver
    )
  end toggleFollowButton

  def body: HtmlElement =
    div(
      profileSignal --> userInfoProfileUpdater,
      cls := "user-info",
      div(
        cls := "container",
        div(
          cls := "row",
          div(
            cls := "col-xs-12 col-md-10 offset-md-1",
            img(cls := "user-img"),
            h4(child.text <-- profileSignal.map(_.username)),
            p(child.maybe <-- profileSignal.map(_.bio.map(_.value))),
            child <-- profileSignal.splitOne(_.username)((username, _, _) =>
              if state.user.map(_.username == username).getOrElse(false) then editProfileButton()
              else toggleFollowButton()
            )
          )
        )
      )
    )
  end body
end UserInfo
