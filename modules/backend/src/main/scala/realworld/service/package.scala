package realworld

import monocle.Iso
import java.time.Instant
import smithy4s.Timestamp
import cats.syntax.all.*

package object service:
  extension (i: Instant)
    def toTimestamp = Timestamp.fromInstant(i)

