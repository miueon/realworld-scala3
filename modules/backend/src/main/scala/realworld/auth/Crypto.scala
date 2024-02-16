package realworld.auth

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

import cats.effect.kernel.Sync
import cats.syntax.all.*

import io.github.iltotore.iron.autoRefine
import io.github.iltotore.iron.refine
import realworld.config.types.PasswordSalt
import realworld.domain.user.DecryptCipher
import realworld.domain.user.EncryptCipher
import realworld.domain.user.EncryptedPassword
import realworld.spec.Password
import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import com.password4j.Argon2Function
import com.password4j.Password as pswd
import realworld.domain.user.Users.password
import realworld.spec

trait Crypto:
  def encrypt(value: Password): EncryptedPassword
  def verifyPassword(
      password: Password,
      encryptedPassword: EncryptedPassword
  ): Boolean

object Crypto:
  private final val MemoryInKib          = 12
  private final val NumberOfIterations   = 20
  private final val LevelOfParallelism   = 2
  private final val LengthOfTheFinalHash = 32
  private final val Type                 = com.password4j.types.Argon2.ID
  private final val Version              = 19
  private final val Argon2: Argon2Function =
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
          EncryptedPassword(pswd.hash(value.value).`with`(Argon2).getResult())

        def verifyPassword(
            password: realworld.spec.Password,
            encryptedPassword: EncryptedPassword
        ): Boolean =
          pswd.check(password.value, encryptedPassword.value) `with` Argon2
    }

end Crypto
