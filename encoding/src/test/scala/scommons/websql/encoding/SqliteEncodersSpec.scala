package scommons.websql.encoding

import scommons.nodejs.test.TestSpec
import scommons.websql.Database

import java.util.{Date, UUID}
import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array

class SqliteEncodersSpec extends TestSpec {

  private lazy val ctx = new TestSqliteContext(mock[Database])
  
  import ctx._

  private def encode[T](value: T)(implicit e: Encoder[T]): List[js.Any] = {
    e.apply(0, value, Nil)
  }
  
  it should "encode Some" in {
    //when & then
    encode[Option[String]](Some("test")) shouldBe List[js.Any]("test")
  }
  
  it should "encode None" in {
    //when & then
    encode[Option[String]](None) shouldBe List[js.Any](null)
  }

  it should "encode String" in {
    //when & then
    encode[String]("test") shouldBe List[js.Any]("test")
  }

  it should "encode Double" in {
    //when & then
    encode[Double](Double.MinValue) shouldBe List[js.Any](Double.MinValue)
    encode[Double](Double.MaxValue) shouldBe List[js.Any](Double.MaxValue)
  }

  it should "encode Boolean" in {
    //when & then
    encode[Boolean](false) shouldBe List[js.Any](0)
    encode[Boolean](true) shouldBe List[js.Any](1)
  }

  it should "encode BigDecimal" in {
    //when & then
    encode[BigDecimal](BigDecimal(12.345)) shouldBe List[js.Any](12.345d)
  }

  it should "encode Byte" in {
    //when & then
    encode[Byte](Byte.MinValue) shouldBe List[js.Any](Byte.MinValue)
    encode[Byte](Byte.MaxValue) shouldBe List[js.Any](Byte.MaxValue)
  }

  it should "encode Short" in {
    //when & then
    encode[Short](Short.MinValue) shouldBe List[js.Any](Short.MinValue)
    encode[Short](Short.MaxValue) shouldBe List[js.Any](Short.MaxValue)
  }

  it should "encode Int" in {
    //when & then
    encode[Int](Int.MinValue) shouldBe List[js.Any](Int.MinValue)
    encode[Int](Int.MaxValue) shouldBe List[js.Any](Int.MaxValue)
  }

  it should "encode Long" in {
    //when & then
    encode[Long](Long.MinValue) shouldBe List[js.Any](Long.MinValue.toDouble)
    encode[Long](Long.MaxValue) shouldBe List[js.Any](Long.MaxValue.toDouble)
  }

  it should "encode Float" in {
    //when & then
    encode[Float](Float.MinValue) shouldBe List[js.Any](Float.MinValue)
    encode[Float](Float.MaxValue) shouldBe List[js.Any](Float.MaxValue)
  }

  it should "encode UUID" in {
    //given
    val value = UUID.randomUUID()

    //when & then
    encode[UUID](value) shouldBe List[js.Any](value.toString)
  }

  it should "encode Seq[Byte]" in {
    //given
    val data = Seq[Byte](1, 2, 3)
    
    //when
    val result = inside(encode[Seq[Byte]](data)) {
      case List(res) => res
    }
    
    //then
    result.asInstanceOf[Int8Array].toArray shouldBe data
  }

  it should "encode Array[Byte]" in {
    //given
    val data = Array[Byte](1, 2, 3)
    
    //when
    val result = inside(encode[Array[Byte]](data)) {
      case List(res) => res
    }
    
    //then
    result.asInstanceOf[Int8Array].toArray shouldBe data
  }

  it should "encode Date" in {
    //given
    val time = new Date().getTime

    //when
    val result = inside(encode[Date](new Date(time))) {
      case List(res) => res
    }
    
    //then
    result.asInstanceOf[Double] shouldBe time.toDouble
  }

  it should "encode Seq[T]" in {
    //when & then
    encode[Seq[Int]](Seq(1, 2, 3)) shouldBe List[js.Any](1, 2, 3)
  }
}
