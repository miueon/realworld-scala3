package realworld.domain

import cats.Functor
import doobie.{Composite, SQLDefinition, TableDefinition, WithSQLDefinition}
import doobie.TableDefinition.RowHelpers
import io.circe.{Decoder, Encoder}
import realworld.spec.Total

import scala.annotation.targetName

case class WithId[Id, T](id: Id, entity: T)

object WithId:
  given withIdread[Id, T](
    using
    idRead: Decoder[Id],
    tRead: Decoder[T]
  ): Decoder[WithId[Id, T]] =
    Decoder[(Id, T)].map((WithId.apply[Id, T]).tupled)

  given withIdWrite[Id, T](
    using
    idWrite: Encoder[Id],
    tWrite: Encoder[T]
  ): Encoder[WithId[Id, T]] =
    Encoder[(Id, T)].contramap(w => (w.id, w.entity))

  given sqlDef[Id, T](
    using
    idSqldef: SQLDefinition[Id],
    entitySqlDef: SQLDefinition[T],
    tableDef: TableDefinition
  ): (WithSQLDefinition[WithId[Id, T]] & TableDefinition.RowHelpers[WithId[Id, T]]) =
    new WithSQLDefinition[WithId[Id, T]](
      Composite(
        idSqldef,
        entitySqlDef
      )((id, e) => WithId(id, e))(Tuple.fromProductTyped[WithId[Id, T]])
    ) with TableDefinition.RowHelpers[WithId[Id, T]](tableDef) {}

end WithId

case class WithTotal[T](total: Total, entity: T)
object WithTotal:
  @targetName("helper")
  def apply[T](total: Int, entity: T): WithTotal[T] = WithTotal(Total(total), entity)
  given Functor[WithTotal] = new Functor[WithTotal]:
    def map[A, B](fa: WithTotal[A])(f: A => B): WithTotal[B] =
      WithTotal(fa.total, f(fa.entity))
