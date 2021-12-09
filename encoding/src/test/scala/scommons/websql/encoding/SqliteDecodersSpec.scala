package scommons.websql.encoding

import scommons.nodejs.test.TestSpec
import scommons.websql.{Database, WebSqlRow}

import java.util.{Date, UUID}
import scala.scalajs.js
import scala.scalajs.js.typedarray._

class SqliteDecodersSpec extends TestSpec {

  private lazy val ctx = new TestSqliteContext(mock[Database])
  
  import ctx._

  private def decode[T](index: Int, row: ResultRow)(implicit d: Decoder[T]): T = {
    d.apply(index, row)
  }
  
  it should "decode Some" in {
    //when & then
    decode[Option[String]](0, WebSqlRow(js.Dynamic.literal("_1" -> "test"))) shouldBe Some("test")
  }
  
  it should "decode None from js.undefined" in {
    //when & then
    decode[Option[String]](0, WebSqlRow(js.Dynamic.literal("_1" -> ()))) shouldBe None
  }
  
  it should "decode None from null" in {
    //when & then
    decode[Option[String]](0, WebSqlRow(js.Dynamic.literal("_1" -> null))) shouldBe None
  }
  
  it should "decode String" in {
    //when & then
    decode[String](0, WebSqlRow(js.Dynamic.literal("_1" -> "test"))) shouldBe "test"
  }
  
  it should "decode Double" in {
    //when & then
    decode[Double](0, WebSqlRow(js.Dynamic.literal("_1" -> Double.MinValue))) shouldBe Double.MinValue
    decode[Double](0, WebSqlRow(js.Dynamic.literal("_1" -> Double.MaxValue))) shouldBe Double.MaxValue
  }
  
  it should "decode Boolean" in {
    //when & then
    decode[Boolean](0, WebSqlRow(js.Dynamic.literal("_1" -> 0))) shouldBe false
    decode[Boolean](0, WebSqlRow(js.Dynamic.literal("_1" -> 1))) shouldBe true
  }
  
  it should "decode BigDecimal" in {
    //when & then
    decode[BigDecimal](0, WebSqlRow(js.Dynamic.literal("_1" -> 12.345))) shouldBe BigDecimal(12.345)
  }
  
  it should "decode Byte" in {
    //when & then
    decode[Byte](0, WebSqlRow(js.Dynamic.literal("_1" -> Byte.MinValue))) shouldBe Byte.MinValue
    decode[Byte](0, WebSqlRow(js.Dynamic.literal("_1" -> Byte.MaxValue))) shouldBe Byte.MaxValue
  }
  
  it should "decode Short" in {
    //when & then
    decode[Short](0, WebSqlRow(js.Dynamic.literal("_1" -> Short.MinValue))) shouldBe Short.MinValue
    decode[Short](0, WebSqlRow(js.Dynamic.literal("_1" -> Short.MaxValue))) shouldBe Short.MaxValue
  }
  
  it should "decode Int" in {
    //when & then
    decode[Int](0, WebSqlRow(js.Dynamic.literal("_1" -> Int.MinValue))) shouldBe Int.MinValue
    decode[Int](0, WebSqlRow(js.Dynamic.literal("_1" -> Int.MaxValue))) shouldBe Int.MaxValue
  }
  
  it should "decode Long" in {
    //when & then
    decode[Long](0, WebSqlRow(js.Dynamic.literal("_1" -> Long.MinValue.toDouble))) shouldBe Long.MinValue
    decode[Long](0, WebSqlRow(js.Dynamic.literal("_1" -> Long.MaxValue.toDouble))) shouldBe Long.MaxValue
  }
  
  it should "decode Float" in {
    //when & then
    decode[Float](0, WebSqlRow(js.Dynamic.literal("_1" -> Float.MinValue))) shouldBe Float.MinValue
    decode[Float](0, WebSqlRow(js.Dynamic.literal("_1" -> Float.MaxValue))) shouldBe Float.MaxValue
  }

  it should "decode UUID" in {
    //given
    val value = UUID.randomUUID()

    //when & then
    decode[UUID](0, WebSqlRow(js.Dynamic.literal("_1" -> value.toString))) shouldBe value
  }
  
  it should "decode Seq[Byte]" in {
    //given
    val data = Seq[Byte](1, 2, 3)
    
    //when & then
    decode[Seq[Byte]](0, WebSqlRow(js.Dynamic.literal("_1" -> data.toArray.toTypedArray))) shouldBe data
  }
  
  it should "decode Array[Byte]" in {
    //given
    val data = Array[Byte](1, 2, 3)
    
    //when & then
    decode[Array[Byte]](0, WebSqlRow(js.Dynamic.literal("_1" -> data.toTypedArray))) shouldBe data
  }
  
  it should "decode Date from double" in {
    //given
    val date = new js.Date()
    
    //when & then
    decode[Date](0, WebSqlRow(js.Dynamic.literal("_1" -> date.getTime))) shouldBe {
      new Date(date.getTime.toLong)
    }
  }
  
  it should "decode Date from string" in {
    //given
    val date = new js.Date()
    
    //when & then
    decode[Date](0, WebSqlRow(js.Dynamic.literal("_1" -> date.toISOString()))) shouldBe {
      new Date(date.getTime().toLong)
    }
  }
}
