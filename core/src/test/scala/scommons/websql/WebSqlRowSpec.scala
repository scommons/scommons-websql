package scommons.websql

import scommons.nodejs.test.TestSpec

import scala.scalajs.js

class WebSqlRowSpec extends TestSpec {

  it should "fail if wrong type" in {
    //given
    val row = WebSqlRow(js.Dynamic.literal("_1" -> 123))
    
    //when
    val ex = the[IllegalStateException] thrownBy {
      row[Long](0)
    }
    
    //then
    ex.getMessage shouldBe "Invalid column type. Expected 'long', but got '123'"
  }
  
  it should "return value if correct type" in {
    //given
    val row = WebSqlRow(js.Dynamic.literal("_1" -> 123))
    
    //when
    val result = row[Int](0)
    
    //then
    result shouldBe 123
  }
}
