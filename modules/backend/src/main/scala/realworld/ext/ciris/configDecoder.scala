package realworld.ext.ciris

import _root_.ciris.ConfigDecoder

object Decoder:
  type Id[A] = ConfigDecoder[String, A]
