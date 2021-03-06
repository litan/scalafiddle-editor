package scalafiddle.client.component

import diode.Action
import diode.react.ModelProxy
import japgolly.scalajs.react._
import org.scalajs.dom.raw.HTMLDivElement

import scala.scalajs.js
import scalafiddle.client._
import scalafiddle.shared._

object Sidebar {
  import japgolly.scalajs.react.vdom.all._

  import SemanticUI._

  private var sideBarRef: HTMLDivElement = _
  private var accordionRef: HTMLDivElement = _

  sealed trait LibMode

  case object SelectedLib extends LibMode

  case object AvailableLib extends LibMode

  case class Props(data: ModelProxy[FiddleData]) {
    def dispatch(a: Action) = data.dispatchCB(a)
  }

  case class State(showAllVersions: Boolean, isOpen: Boolean = false)

  case class Backend($ : BackendScope[Props, State]) {

    def render(props: Props, state: State) = {
      val fd = props.data()
      // filter the list of available libs
      val availableVersions = fd.available
        .filterNot(lib => fd.libraries.exists(_.name == lib.name))
        .filter(lib => lib.scalaVersions.contains(fd.scalaVersion))

      // hide alternative versions, if requested
      val available =
        if (state.showAllVersions)
          availableVersions
        else
          availableVersions.foldLeft(Vector.empty[Library]) {
            case (libs, lib) => if (libs.exists(l => l.name == lib.name)) libs else libs :+ lib
          }
      val libGroups = available.groupBy(_.group).toSeq.sortBy(_._1).map(group => (group._1, group._2.sortBy(_.name)))

      val (authorName, authorImage) = fd.author match {
        case Some(userInfo) if userInfo.id != "anonymous" =>
          (userInfo.name, userInfo.avatarUrl.getOrElse("/assets/images/anon.png"))
        case _ =>
          ("Anonymous", "/assets/images/anon.png")
      }
      div.ref(sideBarRef = _)(cls := "sidebar folded")(
        div.ref(accordionRef = _)(cls := "ui accordion")(
          div(cls := "title large active", "Info", i(cls := "icon dropdown")),
          div(cls := "content active")(
            div(cls := "ui form")(
              div(cls := "field")(
                label("Name"),
                input.text(placeholder := "Untitled", name := "name", value := fd.name, onChange ==> {
                  (e: ReactEventFromInput) =>
                    props.dispatch(UpdateInfo(e.target.value, fd.description))
                })
              ),
              div(cls := "field")(
                label("Description"),
                input.text(
                  placeholder := "Enter description",
                  name := "description",
                  value := fd.description,
                  onChange ==> { (e: ReactEventFromInput) =>
                    props.dispatch(UpdateInfo(fd.name, e.target.value))
                  }
                )
              ),
              div(cls := "field")(
                label("Author"),
                img(cls := "author", src := authorImage),
                authorName
              )
            )
          ),
        ),
        div(cls := "bottom")(
          div(cls := "ui icon basic button toggle", onClick ==> { (e: ReactEventFromHtml) =>
            Callback {
              sideBarRef.classList.toggle("folded")
            } >> $.modState(s => s.copy(isOpen = !state.isOpen))
          })(if (state.isOpen) Icon.angleDoubleLeft else Icon.angleDoubleRight)
        )
      )
    }

    def renderLibrary(lib: Library, mode: LibMode, scalaVersion: String, dispatch: Action => Callback) = {
      val (action, icon) = mode match {
        case SelectedLib  => (DeselectLibrary(lib), i(cls := "remove red icon"))
        case AvailableLib => (SelectLibrary(lib), i(cls := "plus green icon"))
      }
      div(cls := "item")(
        div(cls := "right floated")(
          button(cls := s"mini ui icon basic button", onClick --> dispatch(action))(icon)
        ),
        a(href := lib.docUrl, target := "_blank")(
          div(cls := "content left floated")(
            b(lib.name),
            " ",
            span(cls := (if (lib.scalaVersions.contains(scalaVersion)) "text grey" else "text red"), lib.version))
        )
      )
    }
  }

  val component = ScalaComponent
    .builder[Props]("Sidebar")
    .initialState(State(showAllVersions = false))
    .renderBackend[Backend]
    .componentDidMount(_ =>
      Callback {
        JQueryStatic(accordionRef).accordion(js.Dynamic.literal(exclusive = true, animateChildren = false))
    })
    .build

  def apply(data: ModelProxy[FiddleData]) = component(Props(data))
}
