package realworld

package object validation:
  enum InvalidField:
    def error: String
    def field: String
    case InvalidEmail(error: String, field: String = "email")             extends InvalidField
    case InvalidPassword(error: String, field: String = "password")       extends InvalidField
    case InvalidUsername(error: String, field: String = "username")       extends InvalidField
    case InvalidImageUrl(error: String, field: String = "image")          extends InvalidField
    case InvalidTitle(error: String, field: String = "title")             extends InvalidField
    case InvalidDescription(error: String, field: String = "description") extends InvalidField
    case InvalidBody(error: String, field: String = "body")               extends InvalidField
    case InvalidTag(error: String, field: String = "tag")                 extends InvalidField
end validation
