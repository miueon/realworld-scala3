package realworld.domain.follower

import doobie.TableDefinition.RowHelpers
import doobie.{Column, Composite, TableDefinition, WithSQLDefinition}
import realworld.domain.user.UserId

case class Follower(
    userId: UserId,
    followerId: UserId
)

object Followers extends TableDefinition("followers"):
  val userId: Column[UserId]     = Column("user_id")
  val followerId: Column[UserId] = Column("follower_id")

  object UserSqlDef
      extends WithSQLDefinition[Follower](
        Composite(
          userId.sqlDef,
          followerId.sqlDef
        )(Follower.apply)(Tuple.fromProductTyped)
      )
      with RowHelpers[Follower](this)

  val rowCol = UserSqlDef
end Followers
