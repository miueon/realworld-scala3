package realworld
package tests

import cats.syntax.all.*
import realworld.spec.CreateArticleData
import realworld.types.*

class ArticleDataSupport(probe: Probe):
  import probe.*

  def createArticleData() =
    (gen.strI(Title), gen.strI(Description), gen.strI(Body), gen.strI(TagName).replicateA(3))
      .mapN(CreateArticleData.apply)
