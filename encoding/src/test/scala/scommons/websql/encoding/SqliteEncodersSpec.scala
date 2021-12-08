package scommons.websql.encoding

import scommons.nodejs.test.TestSpec
import scommons.websql.Database

import java.util.{Date, UUID}
import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array

class SqliteEncodersSpec extends TestSpec {

  private lazy val ctx = new TestSqliteContext(mock[Database])
  
  import ctx._

  private def encode[T](value: T)(implicit e: Encoder[T]): js.Any = {
    e.apply(0, value, Nil).head
  }
  
  it should "encode Some" in {
    //when & then
    encode[Option[String]](Some("test")) shouldBe ("test": js.Any)
  }
  
  it should "encode None" in {
    //when & then
    encode[Option[String]](None) shouldBe (null: js.Any)
  }

  it should "encode String" in {
    //when & then
    encode[String]("test") shouldBe ("test": js.Any)
  }

  it should "encode Double" in {
    //when & then
    encode[Double](Double.MinValue) shouldBe (Double.MinValue: js.Any)
    encode[Double](Double.MaxValue) shouldBe (Double.MaxValue: js.Any)
  }

  it should "encode Boolean" in {
    //when & then
    encode[Boolean](false) shouldBe (0: js.Any)
    encode[Boolean](true) shouldBe (1: js.Any)
  }

  it should "encode BigDecimal" in {
    //when & then
    encode[BigDecimal](BigDecimal(12.345)) shouldBe (12.345d: js.Any)
  }

  it should "encode Byte" in {
    //when & then
    encode[Byte](Byte.MinValue) shouldBe (Byte.MinValue: js.Any)
    encode[Byte](Byte.MaxValue) shouldBe (Byte.MaxValue: js.Any)
  }

  it should "encode Short" in {
    //when & then
    encode[Short](Short.MinValue) shouldBe (Short.MinValue: js.Any)
    encode[Short](Short.MaxValue) shouldBe (Short.MaxValue: js.Any)
  }

  it should "encode Int" in {
    //when & then
    encode[Int](Int.MinValue) shouldBe (Int.MinValue: js.Any)
    encode[Int](Int.MaxValue) shouldBe (Int.MaxValue: js.Any)
  }

  it should "encode Long" in {
    //when & then
    encode[Long](Long.MinValue) shouldBe (Long.MinValue: js.Any)
    encode[Long](Long.MaxValue) shouldBe (Long.MaxValue: js.Any)
  }

  it should "encode Float" in {
    //when & then
    encode[Float](Float.MinValue) shouldBe (Float.MinValue: js.Any)
    encode[Float](Float.MaxValue) shouldBe (Float.MaxValue: js.Any)
  }

  it should "encode UUID" in {
    //given
    val value = UUID.randomUUID()

    //when & then
    encode[UUID](value) shouldBe (value.toString: js.Any)
  }

  it should "encode Seq[Byte]" in {
    //given
    val data = Seq[Byte](1, 2, 3)
    
    //when
    val result = encode[Seq[Byte]](data)
    
    //then
    result.asInstanceOf[Int8Array].toArray shouldBe data
  }

  it should "encode Array[Byte]" in {
    //given
    val data = Array[Byte](1, 2, 3)
    
    //when
    val result = encode[Array[Byte]](data)
    
    //then
    result.asInstanceOf[Int8Array].toArray shouldBe data
  }

  it should "encode Date" in {
    //given
    val time = new Date().getTime

    //when
    val result = encode[Date](new Date(time))
    
    //then
    result.asInstanceOf[Double] shouldBe time.toDouble
  }
}
