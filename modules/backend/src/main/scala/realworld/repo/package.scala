package realworld

package object repo:
  def affectedToOption(n: Int): Option[Unit] =
    if n > 0 then Some(())
    else None
