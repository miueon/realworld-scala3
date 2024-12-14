package realworld.auth

import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.password4j.{Argon2Function, Password as pswd}
import realworld.config.types.PasswordSalt
import realworld.domain.user.EncryptedPassword
import realworld.types.Password

trait Crypto:
  def encrypt(value: Password): EncryptedPassword
  def verifyPassword(
    password: Password,
    encryptedPassword: EncryptedPassword
  ): Boolean

object Crypto:
  final private val MemoryInKib          = 12
  final private val NumberOfIterations   = 20
  final private val LevelOfParallelism   = 2
  final private val LengthOfTheFinalHash = 32
  final private val Type                 = com.password4j.types.Argon2.ID
  final private val Version              = 19
  final private val Argon2: Argon2Function =
    Argon2Function.getInstance(
      MemoryInKib,
      NumberOfIterations,
      LevelOfParallelism,
      LengthOfTheFinalHash,
      Type,
      Version
    )
  def make[F[_]: Sync](passwordSalt: PasswordSalt): F[Crypto] =
    Sync[F].delay {
      new Crypto:
        def encrypt(value: Password): EncryptedPassword =
          EncryptedPassword(pswd.hash(value).`with`(Argon2).getResult())

        def verifyPassword(
          password: Password,
          encryptedPassword: EncryptedPassword
        ): Boolean =
          pswd.check(password, encryptedPassword.value) `with` Argon2
    }

end Crypto
