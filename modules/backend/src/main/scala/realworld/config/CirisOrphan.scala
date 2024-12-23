package realworld.config

export CirisOrphan.given
import cats.syntax.all.*
import ciris.ConfigDecoder
import com.comcast.ip4s.{Host, Port}
import realworld.domain.types.Wrapper

import java.time.Instant
import java.util.UUID

object CirisOrphan:
  given ConfigDecoder[String, Instant] =
    ConfigDecoder[String].mapOption("java.time.Instant")(s =>
      Either.catchNonFatal(Instant.parse(s)).toOption
    )

  given ConfigDecoder[String, UUID] =
    ConfigDecoder[String].mapOption("java.util.UUID")(s =>
      Either.catchNonFatal(UUID.fromString(s)).toOption
    )

  given ConfigDecoder[String, Host] =
    ConfigDecoder[String].mapOption("com.comcast.ip4s.Host")(Host.fromString)

  given ConfigDecoder[String, Port] =
    ConfigDecoder[String].mapOption("com.comcast.ip4s.Port")(Port.fromString)

  given [A, B](
    using wp: Wrapper[A, B],
    cd: ConfigDecoder[String, A]
  ): ConfigDecoder[String, B] =
    cd.map(a => wp.iso.get(a))
end CirisOrphan
