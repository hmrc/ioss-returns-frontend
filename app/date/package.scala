import java.time.LocalDate

package object date {
  implicit class LocalDateOps(val localDate: LocalDate) extends Ordered[LocalDate] {
    def compare(other: LocalDate): Int = localDate.compareTo(other)
  }
}