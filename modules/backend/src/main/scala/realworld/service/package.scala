package realworld

import smithy4s.Timestamp

import java.time.Instant

package object service:
  extension (i: Instant) def toTimestamp = Timestamp.fromInstant(i)
