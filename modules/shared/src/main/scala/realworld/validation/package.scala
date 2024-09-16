package realworld
import cats.data.*

package object validation:
  trait InvalidField:
    def error: String
    def field: String
end validation
