package realworld.domain.follower

import doobie.Column
import doobie.Composite
import doobie.TableDefinition
import doobie.WithSQLDefinition
import realworld.domain.user.UserId
import doobie.TableDefinition.RowHelpers

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
      ) with RowHelpers[Follower](this)

  val rowCol = UserSqlDef
