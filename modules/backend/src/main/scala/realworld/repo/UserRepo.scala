package realworld.repo

import cats.data.*
import cats.effect.*
import cats.syntax.all.*
import doobie.*
import doobie.free.connection.ConnectionIO
import doobie.implicits.*
import io.github.iltotore.iron.doobie.given
import realworld.domain.WithId
import realworld.domain.user.{DBUser, EncryptedPassword, UserId}
import realworld.spec.{RegisterUserData, UpdateUserData}
import realworld.types.{Email, Username}

trait UserRepo[F[_]]:
  def findById(id: UserId): F[Option[WithId[UserId, DBUser]]]
  def findByEmail(email: Email): F[Option[WithId[UserId, DBUser]]]
  def findByUsername(username: Username): F[Option[WithId[UserId, DBUser]]]
  def create(
    uid: UserId,
    user: RegisterUserData,
    encryptedPassword: EncryptedPassword
  ): F[Int]
  def update(
    uid: UserId,
    updateData: UpdateUserData,
    hashedPsw: Option[EncryptedPassword]
  ): F[Option[WithId[UserId, DBUser]]]
end UserRepo

object UserRepo:
  import UserSQL as u
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    new UserRepo[F]():
      def findById(id: UserId): F[Option[WithId[UserId, DBUser]]] =
        u.get(id).transact(xa)

      def findByEmail(email: Email): F[Option[WithId[UserId, DBUser]]] =
        u.selectByEmail(email).transact(xa)

      def findByUsername(
        username: Username
      ): F[Option[WithId[UserId, DBUser]]] =
        u.selectByUsername(username).transact(xa)

      def create(
        uid: UserId,
        user: RegisterUserData,
        encryptedPassword: EncryptedPassword
      ): F[Int] =
        u.insert(uid, user.username, user.email, encryptedPassword).transact(xa)

      def update(
        uid: UserId,
        updateData: UpdateUserData,
        hashedPsw: Option[EncryptedPassword]
      ): F[Option[WithId[UserId, DBUser]]] =
        u.update(uid, updateData, hashedPsw).transact(xa)
end UserRepo

private object UserSQL:
  import realworld.domain.user.Users as u
  import realworld.domain.user.given

  private def queryUser(
    conditionFragment: Fragment
  ) =
    (fr"SELECT ${u.rowCol} FROM $u " ++ conditionFragment)
      .queryOf(u.rowCol)

  def get(userId: UserId): ConnectionIO[Option[WithId[UserId, DBUser]]] =
    queryUser(fr"WHERE ${u.id === userId} ").option

  def selectByEmail(email: Email): ConnectionIO[Option[WithId[UserId, DBUser]]] =
    sql"""
    SELECT ${u.rowCol} FROM $u WHERE ${u.email === email}
    """
      .queryOf(u.rowCol)
      .option

  def selectByUsername(
    username: Username
  ): ConnectionIO[Option[WithId[UserId, DBUser]]] =
    sql"""
    SELECT ${u.rowCol} FROM $u WHERE ${u.username === username}
    """
      .queryOf(u.rowCol)
      .option

  def insert(
    id: UserId,
    username: Username,
    email: Email,
    password: EncryptedPassword
  ): ConnectionIO[Int] =
    insertInto(
      u,
      NonEmptyVector.of(
        u.id --> id,
        u.username --> username,
        u.email --> email,
        u.password --> password
      )
    ).update.run

  def update(
    uid: UserId,
    updateData: UpdateUserData,
    hashedPsw: Option[EncryptedPassword]
  ): ConnectionIO[Option[WithId[UserId, DBUser]]] =
    val emailopt    = updateData.email.map(u.email --> _)
    val usernameopt = updateData.username.map(u.username --> _)
    val pswopt      = hashedPsw.map(u.password --> _)

    val optionalFields = List(emailopt, usernameopt, pswopt).flatten
    val mandatoryFields =
      List(u.bio --> updateData.bio, u.image --> updateData.image)

    NonEmptyList
      .fromList(optionalFields ++ mandatoryFields)
      .fold(get(uid)) { (nf: NonEmptyList[(Fragment, Fragment)]) =>
        sql"""
          ${updateTable[NonEmptyList](u, nf)} WHERE ${u.id === uid}
        """.update
          .withUniqueGeneratedKeys[WithId[UserId, DBUser]](
            u.rowCol.columns.map(_.rawName).toList*
          )
          .some
          .sequence
      }
  end update
end UserSQL
