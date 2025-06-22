You are an expert in Scala 3, Cats Effect, Iron, ducktape, Monocle, FS2, Doobie, Http4s, and the Typelevel ecosystem.

## Core Functional Programming Philosophy

**Fundamental FP Concepts Integration**
This project exemplifies advanced functional programming through these core concepts:

1. **Algebraic Data Types (ADTs)** - Use Scala 3 `enum` for sum types with exhaustive pattern matching
2. **Pattern-matching** - Leverage comprehensive pattern matching in error recovery and business logic
3. **Higher-Order Functions (HOFs)** - Compose operations using `map`, `flatMap`, `traverse`, `sequence`
4. **Closures** - Capture context in lambda expressions and for-comprehensions
5. **Immutability** - Exclusively use immutable data structures and `val` declarations
6. **Totality** - Ensure all functions handle all inputs via `Option`, `Either`, and exhaustive matching
7. **Streams** - Process data efficiently with FS2 streams for backpressure and resource safety
8. **Lazy streams** - Use `LazyList` for potentially infinite sequences with lazy evaluation
9. **Stream fusion** - Leverage FS2's fusion optimization for efficient stream composition
10. **Deforestation** - Eliminate intermediate data structures through stream fusion and direct composition

**Category Theory Foundations** 11. **Functor** - Map over containers while preserving structure (`map` operations) 12. **Applicative** - Combine independent computations with `mapN` and applicative validation 13. **Monad** - Chain dependent computations with for-comprehensions and `flatMap` 14. **Parser Combinators** - Build complex parsers from simple components (used in config parsing) 15. **Applicative Validation** - Accumulate validation errors without short-circuiting 16. **Semigroup** - Combine values associatively with `|+|` operator 17. **Monoid** - Fold collections with identity elements and associative operations

**Type System Mastery** 18. **Parametric Polymorphism vs Ad-hoc Polymorphism** - Generic types vs type class instances 19. **Type Inference** - Leverage Scala 3's improved type inference for cleaner code 20. **Contravariant** - Use contravariant functors for input types in type classes 21. **Profunctor** - Handle both input and output type variance in functional abstractions

**Data Transformation Patterns** 22. **Lens** - Focus on nested data updates with Monocle optics 23. **Prism** - Safely extract values from sum types with pattern matching 24. **fold** - Reduce collections to single values with `foldLeft`, `foldMap` 25. **traverse** - Effectfully process collections while preserving structure

**Advanced Functional Patterns** 26. **Recursion Schemes** - Abstract recursion patterns with catamorphisms and anamorphisms 27. **Tagless Final** - Abstract over effect types with higher-kinded type parameters 28. **Recursion** - Prefer structural recursion over loops for data processing 29. **Free Monad** - Separate program description from interpretation 30. **Hash-Consing** - Share identical substructures to reduce memory usage

**Theoretical Foundations** 31. **Lambda Calculus** - Understand function composition and currying principles 32. **De Bruijn index** - Reference variables by position in lambda expressions 33. **Expression problem** - Add operations and types without modification 34. **Endomorphism** - Functions from a type to itself (`A => A`) 35. **Isomorphism** - Bidirectional transformations between equivalent types

**Architectural Patterns** 36. **Functional Core, Imperative Shell** - Pure core logic with effectful boundaries 37. **Smart Constructor** - Validate data construction with Iron refinements and RefinedTypeOps 38. **Make Illegal States Unrepresentable** - Use types to eliminate invalid program states 39. **Errors as values** - Handle errors explicitly with `Either`, domain ADTs, and `IO.raiseError` 40. **Parse, Don't Validate** - Transform input into precise types at system boundaries

## Project-Specific Patterns

**Newtype Pattern with Opaque Types**

```scala
// Follow the project's DeriveType pattern for zero-cost abstractions
abstract class DeriveType[A]:
  opaque type Type = A
  inline def apply(a: A): Type = a
  extension (t: Type) inline def value: A = t
  extension (t: A) inline def asType: Type = t
```

**Iron Refinement Types (Domain Constraints)**

```scala
// Use descriptive constraints with DescribedAs for better error messages
type UsernameConstraint = MinLength[1] & MaxLength[50] & Not[Blank] DescribedAs
  "should not be blank and should be between 1 and 50 characters"
type Username = String :| UsernameConstraint
object Username extends RefinedTypeOps[String, UsernameConstraint, Username]
```

**Enum-Based Error Modeling**

```scala
// Domain errors as enums extending NoStackTrace for performance
enum UserError extends NoStackTrace:
  def msg: String
  case UserNotFound(msg: String = "User not found") extends UserError
  case EmailAlreadyExists(msg: String = "Email already exists") extends UserError
```

**Tagless Final Service Pattern**

```scala
// Services abstracted over effect type with smart constructor
trait UserRepo[F[_]]:
  def findById(id: UserId): F[Option[WithId[UserId, DBUser]]]
  def create(uid: UserId, user: RegisterUserData): F[Int]

object UserRepo:
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): UserRepo[F] = new:
    // Implementation here
```

## Import Organization (Project Style)

**Wildcard Import Syntax**

```scala
// Always use * instead of _ for wildcard imports
import cats.effect.*
import cats.syntax.all.*
import io.github.iltotore.iron.{*, given}
import doobie.{*, given}
```

**Given Import Pattern**

```scala
// Import given instances for type class derivation
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.doobie.given
```

## Scala 3 Modern Syntax Preferences

**Indentation-Based Syntax**

```scala
// Use indentation for most constructs, maintain braces for short lambdas
def processUser(user: User)(using config: AppConfig): IO[Unit] =
  user.status match
    case UserStatus.Active =>
      for
        profile <- userRepo.findProfile(user.id)
        _       <- logger.info(s"Processing active user: ${user.id}")
        result  <- businessLogic.process(profile)
      yield result
    case UserStatus.Suspended =>
      logger.warn(s"Skipping suspended user: ${user.id}")
    case UserStatus.Deleted =>
      IO.raiseError(InvalidUserException(user.id))
```

**Given/Using Over Implicit**

```scala
// Prefer given and using over implicit
def authenticate[F[_]: MonadThrow](token: String)(using decoder: JwtDecoder): F[UserSession] =
  decoder.decode(token).fold(
    error => MonadThrow[F].raiseError(AuthError.InvalidToken(error)),
    session => session.pure[F]
  )
```

## Effect Composition Patterns

**Resource Management**

```scala
// Use Resource for bracketed operations with automatic cleanup
def databaseResource[F[_]: Async](config: DatabaseConfig): Resource[F, Transactor[F]] =
  for
    ce <- ExecutionContexts.fixedThreadPool[F](config.poolSize)
    xa <- HikariTransactor.newHikariTransactor[F](
      config.driver,
      config.url,
      config.user,
      config.password,
      ce
    )
  yield xa
```

**Error Recovery Patterns**

```scala
// Use recoverWith for domain-specific error handling
def getProfile(username: Username, authOpt: Option[AuthHeader]): F[GetProfileOutput] =
  val result = for
    uidOpt  <- authOpt.map(auth.authUserId).sequence
    profile <- profiles.get(username, uidOpt)
  yield GetProfileOutput(profile)

  result
    .onError(e => Logger[F].warn(e)(s"Failed to get profile for user: $username"))
    .recoverWith:
      case UserError.ProfileNotFound(msg) => NotFoundError(msg.some).raise
```

**Concurrent Operations**

```scala
// Use cats.Parallel for independent concurrent operations
def fetchUserData(userId: UserId): IO[UserData] =
  (
    userRepo.findProfile(userId),
    userRepo.findPreferences(userId),
    analyticsService.getUserStats(userId)
  ).parMapN(UserData.apply)
```

## Database Integration Patterns

**Doobie with Iron Types**

```scala
// Define Meta instances for Iron types
given Meta[Email] = Meta[String].refined[EmailConstraint]
given Meta[Username] = Meta[String].refined[UsernameConstraint]
given Meta[EncryptedPassword] = EncryptedPassword.derive

// Use transact for database operations
def findByEmail(email: Email): F[Option[WithId[UserId, DBUser]]] =
  sql"SELECT id, email, username, password, bio, image FROM users WHERE email = $email"
    .query[WithId[UserId, DBUser]]
    .option
    .transact(xa)
```

## Response Constraints

- Preserve the project's newtype pattern using `DeriveType` and opaque types
- Maintain Iron constraint definitions with `DescribedAs` for error messages
- Keep enum-based error types extending `NoStackTrace`
- Use tagless final pattern for service abstractions
- Follow the established import organization with `*` wildcards and `given` imports
- Prefer indentation-based syntax for multi-line constructs
- Always suggest Iron refinements for domain constraints
- Use for-comprehensions for monadic composition
- Apply `cats.syntax.all.*` for functional combinators
- Leverage `Resource` for safe resource management

## File Organization (Project Structure)

```
domain/
  User.scala         // Domain types, errors, and table definitions
  Article.scala      // Article domain with Iron constraints
  types/
    NewType.scala    // Opaque type abstractions
    IsUUID.scala     // UUID-based ID types
repo/
  UserRepo.scala     // Repository traits and implementations
  package.scala      // Common repository types
service/
  UserService.scala  // Business logic services
http/
  UserServiceImpl.scala // HTTP service implementations
config/
  types.scala        // Configuration types with Iron
  Config.scala       // Ciris configuration parsing
```

## Performance Optimization Patterns

**Zero-Cost Abstractions**

```scala
// Use opaque types for compile-time safety without runtime overhead
type UserId = UserId.Type
object UserId extends IdNewtype  // Compiles to UUID at runtime
```

**Stream Processing**

```scala
// Use FS2 streams for memory-efficient data processing
def processLargeDataset[F[_]: Async](source: Stream[F, RawData]): Stream[F, ProcessedData] =
  source
    .through(validateInput)
    .through(transformData)
    .through(enrichWithMetadata)
    .evalMap(persistToDatabase)
```

**Parallel Processing**

```scala
// Use cats.Parallel for concurrent independent operations
def fetchUserDetails(userIds: List[UserId]): IO[List[UserDetail]] =
  userIds.parTraverse(fetchSingleUser)
```
