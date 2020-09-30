package scommons.websql.quill

import scommons.nodejs.test.TestSpec
import io.getquill._
import showcase.domain.{CategoryEntity, ProductEntity}

class WebSqlDialectSpec extends TestSpec {

  private val ctx = new SqlMirrorContext(WebSqlDialect, Literal)
  import ctx._
  
  it should "add aliases for top query when single column" in {
    //when
    val sql = ctx.run(
      infix"""SELECT t.i FROM TestEntity t""".as[Query[Int]]
    ).string
    
    //then
    sql shouldBe """SELECT x AS _0 FROM (SELECT t.i FROM TestEntity t) AS x"""
  }
  
  it should "add aliases for top query when multiple columns" in {
    //when
    val sql = ctx.run(
      infix"""SELECT t.x, t.y FROM TestEntity t""".as[Query[(Int, Int)]]
    ).string
    
    //then
    sql shouldBe """SELECT x._1 AS _0, x._2 AS _1 FROM (SELECT t.x, t.y FROM TestEntity t) AS x"""
  }
  
  it should "add aliases for top query when join" in {
    //when
    val sql = ctx.run {
      for {
        p <- query[ProductEntity].sortBy(_.id)
        c <- query[CategoryEntity].join(c => p.categoryId.contains(c.id))
      } yield (p, c)
    }.string
    
    //then
    sql shouldBe {
      "SELECT x1.id AS _0, x1.name AS _1, x1.categoryId AS _2, c.id AS _3, c.categoryName AS _4 FROM" +
        " (SELECT x1.id, x1.name, x1.categoryId FROM ProductEntity x1 ORDER BY x1.id ASC" +
        " /* NULLS FIRST omitted (not supported by sqlite) */) AS x1" +
        " INNER JOIN CategoryEntity c ON x1.categoryId = c.id"
    }
  }
}
