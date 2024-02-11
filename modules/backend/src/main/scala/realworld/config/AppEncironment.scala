package realworld.config

import cats.implicits.*

import ciris.ConfigDecoder
import realworld.domain.types.DeriveType

enum AppEnvironment extends DeriveType[AppEnvironment]:
  case Test
  case Prod

object AppEnvironment:
  given ConfigDecoder[String, AppEnvironment] =
    ConfigDecoder[String].mapOption("realworld.config.AppEnvironment")(n =>
      Either.catchNonFatal(AppEnvironment.valueOf(n)).toOption
    )
