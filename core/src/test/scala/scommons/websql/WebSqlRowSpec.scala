package scommons.websql

import scommons.nodejs.test.TestSpec

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

class WebSqlRowSpec extends TestSpec {

  private val sql = "test sql"
  
  it should "fail if wrong type" in {
    //given
    val row = WebSqlRow(sql, literal("_1" -> 123))
    row.index shouldBe 0
    
    //when
    val ex = the[IllegalStateException] thrownBy {
      row[Long](0)
    }
    
    //then
    ex.getMessage shouldBe "Expected 'long' type, but got '123'\n\tat column: '_1', sql: test sql"

    //when & then
    row.index shouldBe 1
  }
  
  it should "return value if correct type" in {
    //given
    val row = WebSqlRow(sql, literal("_1" -> 123))
    row.index shouldBe 0
    
    //when & then
    row[Int](0) shouldBe 123
    
    //when & then
    row.index shouldBe 1
  }

  it should "return true if defined value when isDefinedAt" in {
    //given
    val row = WebSqlRow(sql, literal("_1" -> 123))
    row.index shouldBe 0
    
    //when & then
    row.isDefinedAt(0) shouldBe true
    
    //when & then
    row.index shouldBe 0
  }

  it should "return false if null value when isDefinedAt" in {
    //given
    val row = WebSqlRow(sql, literal("_1" -> null))
    row.index shouldBe 0
    
    //when & then
    row.isDefinedAt(0) shouldBe false
    
    //when & then
    row.index shouldBe 0
  }

  it should "return false if undefined value when isDefinedAt" in {
    //given
    val row = WebSqlRow(sql, literal("_1" -> js.undefined))
    row.index shouldBe 0
    
    //when & then
    row.isDefinedAt(0) shouldBe false
    
    //when & then
    row.index shouldBe 0
  }

  it should "increment next index when skipIndices" in {
    //given
    val row = WebSqlRow(sql, literal())
    row.index shouldBe 0
    
    //when & then
    row.skipIndices(1)
    row.index shouldBe 1
    
    //when & then
    row.skipIndices(2)
    row.index shouldBe 3
  }
}
