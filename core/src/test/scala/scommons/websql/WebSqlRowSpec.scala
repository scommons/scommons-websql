package scommons.websql

import scommons.nodejs.test.TestSpec

import scala.scalajs.js.Dynamic.literal

class WebSqlRowSpec extends TestSpec {

  private val sql = "test sql"
  
  it should "fail if wrong type" in {
    //given
    val row = WebSqlRow(sql, literal("_1" -> 123))
    
    //when
    val ex = the[IllegalStateException] thrownBy {
      row[Long](0)
    }
    
    //then
    ex.getMessage shouldBe "Expected 'long' type, but got '123'\n\tat column: '_1', sql: test sql"
  }
  
  it should "return value if correct type" in {
    //given
    val row = WebSqlRow(sql, literal("_1" -> 123))
    
    //when
    val result = row[Int](0)
    
    //then
    result shouldBe 123
  }
}
