package realworld.config

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.any.{DescribedAs, Not}
import io.github.iltotore.iron.constraint.numeric.Interval.*
import io.github.iltotore.iron.constraint.string.Blank

type NonEmptyOrBlank = MinLength[1] & Not[Blank] DescribedAs "Should not be empty or blank"
type NonEmptyStringR = String :| NonEmptyOrBlank
object NonEmptyStringR extends RefinedTypeOps[String, NonEmptyOrBlank, NonEmptyStringR]

type PosInt = Int :| Greater[0]
object PosInt extends RefinedTypeOps[Int, Greater[0], PosInt]

type UserPortNumberCons = Closed[1024, 49151]
type UserPortNumber     = Int :| UserPortNumberCons
object UserPortNumber extends RefinedTypeOps[Int, UserPortNumberCons, UserPortNumber]
