package realworld.domain.users

import java.util.UUID
import javax.crypto.Cipher

import doobie.*
import realworld.domain.Instances.given
import realworld.domain.types.DeriveType
import realworld.domain.types.Newtype
import realworld.spec.Bio
import realworld.spec.Email
import realworld.spec.ImageUrl
import realworld.spec.Token
import realworld.spec.User
import realworld.spec.Username
import realworld.types.NonEmptyStringR
import realworld.domain.types.IsUUID
import realworld.domain.types.Wrapper
import realworld.domain.types.IdNewtype

type UserId = UserId.Type
object UserId extends IdNewtype

type EncryptedPassword = EncryptedPassword.Type
object EncryptedPassword extends Newtype[String]

case class EncryptCipher(value: Cipher)

case class DecryptCipher(value: Cipher)

given Meta[Email]    = Meta[String].imap(Email(_))(_.value)
given Meta[Token]    = Meta[String].imap(Token(_))(_.value)
given Meta[Username] = Meta[NonEmptyStringR].imap(Username(_))(_.value)
given Meta[EncryptedPassword] =
  Meta[String].imap(EncryptedPassword(_))(_.value)
given Meta[Bio]      = Meta[String].imap(Bio(_))(_.value)
given Meta[ImageUrl] = Meta[String].imap(ImageUrl(_))(_.value)

object Users extends TableDefinition("users"):
  val id: Column[UserId]                  = Column("id")
  val email: Column[Email]                = Column("email")
  val token: Column[Option[Token]]        = Column("token")
  val username: Column[Username]          = Column("username")
  val password: Column[EncryptedPassword] = Column("password")
  val bio: Column[Option[Bio]]            = Column("bio")
  val image: Column[Option[ImageUrl]]     = Column("image")

  object UserSqlDef
      extends WithSQLDefinition[User](
        Composite(
          (
            email.sqlDef,
            username.sqlDef,
            token.sqlDef,
            bio.sqlDef,
            image.sqlDef
          )
        )(User.apply)(Tuple.fromProductTyped)
      )
  val columns = UserSqlDef

end Users

case class UserWithPassword(
    user: User,
    password: EncryptedPassword
)
import Users.*
object UserWithPassword:
    object SqlDef extends WithSQLDefinition[UserWithPassword](
      Composite(
        UserSqlDef.sqlDef,
        password.sqlDef
      )(UserWithPassword.apply)(Tuple.fromProductTyped)
    )
    val columns = SqlDef
