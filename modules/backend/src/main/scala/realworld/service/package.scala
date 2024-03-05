package realworld

import java.time.Instant
import smithy4s.Timestamp

package object service:
  extension (i: Instant)
    def toTimestamp = Timestamp.fromInstant(i)

