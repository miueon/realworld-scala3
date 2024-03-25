package realworld.routes

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L
import io.circe.*

sealed trait Page derives Codec.AsObject
object Page:
  case object Home     extends Page
  case object Login    extends Page
  case object Register extends Page
end Page
