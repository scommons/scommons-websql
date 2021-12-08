package scommons.websql.quill

import io.getquill.NamingStrategy
import scommons.websql.Database
import scommons.websql.encoding.{SqliteDecoders, SqliteEncoders}

import java.time.LocalDate
import scala.scalajs.js

class SqliteContext[T <: NamingStrategy](naming: T, db: Database)
  extends WebSqlContext(WebSqlDialect, naming, db)
    with WebSqlQuillEncoding
    with SqliteEncoders
    with SqliteDecoders {

  implicit val localDateEncoder: Encoder[LocalDate] =
    WebSqlEncoder { (_: Index, value: LocalDate, row: PrepareRow) =>
      val millis: js.Any =
        new js.Date(value.getYear, value.getMonthValue - 1, value.getDayOfMonth).getTime()
      row :+ millis
    }

  implicit val jsDateToLocalDate: MappedEncoding[js.Date, LocalDate] = mappedEncoding[js.Date, LocalDate](d =>
    LocalDate.of(d.getFullYear, d.getMonth + 1, d.getDate)
  )
  implicit val localDateDecoder: Decoder[LocalDate] = mappedDecoder[js.Date, LocalDate]  
}
