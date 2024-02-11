package realworld.domain

import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import realworld.domain.types.IdNewtype
import doobie.WithSQLDefinition
import doobie.SQLDefinition
import doobie.Composite
import realworld.domain.users.UserId
import realworld.domain.users.DBUser
import java.util.UUID
import monocle.Iso
import realworld.domain.types.IsUUID
import realworld.spec.Email
import realworld.spec.Username
import realworld.domain.users.EncryptedPassword

case class WithId[Id, T](id: Id, entity: T) 

object WithId:
  given withIdread[Id, T](using
      idRead: Decoder[Id],
      tRead: Decoder[T]
  ): Decoder[WithId[Id, T]] =
    Decoder[(Id, T)].map((WithId.apply[Id, T] _).tupled)

  given withIdWrite[Id, T](using
      idWrite: Encoder[Id],
      tWrite: Encoder[T]
  ): Encoder[WithId[Id, T]] =
    Encoder[(Id, T)].contramap(w => (w.id, w.entity))

  given sqlDef[Id, T](using
      idSqldef: SQLDefinition[Id],
      entitySqlDef: SQLDefinition[T]
  ): WithSQLDefinition[WithId[Id, T]] =
    object sdef
        extends WithSQLDefinition[WithId[Id, T]](
          Composite(
            idSqldef,
            entitySqlDef
          )(WithId.apply)(Tuple.fromProductTyped)
        )
    sdef
end WithId
