package realworld.domain

import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import realworld.domain.types.IdNewtype
import doobie.WithSQLDefinition
import doobie.SQLDefinition
import doobie.Composite
import realworld.domain.user.UserId
import realworld.domain.user.DBUser
import java.util.UUID
import monocle.Iso
import realworld.domain.types.IsUUID
import realworld.spec.Email
import realworld.spec.Username
import realworld.domain.user.EncryptedPassword
import realworld.spec.Total
import cats.Functor
import scala.annotation.targetName

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

case class WithTotal[T](total: Total, entity: T)
object WithTotal:
  @targetName("helper")
  def apply[T](total: Int, entity: T): WithTotal[T] = WithTotal(Total(total), entity)
  given Functor[WithTotal] = new Functor[WithTotal]:
    def map[A, B](fa: WithTotal[A])(f: A => B): WithTotal[B] =
      WithTotal(fa.total, f(fa.entity))
