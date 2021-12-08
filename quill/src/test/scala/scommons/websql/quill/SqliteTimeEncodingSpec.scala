package scommons.websql.quill

import scommons.nodejs.test.TestSpec
import scommons.websql.quill.showcase.domain.ShowcaseDBContext
import scommons.websql.{Database, WebSqlRow}

import java.time.LocalDate
import scala.scalajs.js

class SqliteTimeEncodingSpec extends TestSpec {

  private lazy val ctx = new ShowcaseDBContext(mock[Database])
  
  import ctx._

  private def encode[T](value: T)(implicit e: Encoder[T]): js.Any = {
    e.apply(0, value, Nil).head
  }

  private def decode[T](index: Int, row: ResultRow)(implicit d: Decoder[T]): T = {
    d.apply(index, row)
  }

  it should "encode LocalDate" in {
    //given
    val d = {
      val now = new js.Date()
      new js.Date(now.getFullYear(), now.getMonth(), now.getDate())
    }

    //when
    val result = encode[LocalDate](LocalDate.of(d.getFullYear, d.getMonth + 1, d.getDate))

    //then
    result.asInstanceOf[Double] shouldBe d.getTime
  }

  it should "decode LocalDate from double" in {
    //given
    val d = new js.Date()
    
    //when & then
    decode[LocalDate](0, WebSqlRow(js.Dynamic.literal("_1" -> d.getTime))) shouldBe {
      LocalDate.of(d.getFullYear, d.getMonth + 1, d.getDate)
    }
  }
  
  it should "decode LocalDate from string" in {
    //when & then
    decode[LocalDate](0, WebSqlRow(js.Dynamic.literal("_1" -> "2020-06-21"))) shouldBe {
      LocalDate.of(2020, 6, 21)
    }
  }
}
