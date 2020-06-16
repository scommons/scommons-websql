package scommons.websql.quill

import org.scalatest.Succeeded
import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.quill.SqliteContextSpec._
import scommons.websql.WebSQL

class SqliteContextSpec extends AsyncTestSpec {

  it should "generate SQL query" in {
    //given
    val dao = new CategoryDao(TestSqliteContext)

    //when
    val result = dao.getById(123).string
    
    //then
    result shouldBe {
      "SELECT c.id, c.category_name FROM categories c WHERE c.id = ?"
    }
  }

  it should "execute quill generated SQL" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(TestSqliteContext)
    val sql = dao.getById(123).string

    //when
    val result = db.transaction { tx =>
      tx.executeSql(
        """create table categories (
          |  id              integer primary key,
          |  category_name   text not null,
          |  created_at      timestamp without time zone default current_timestamp,
          |  UNIQUE (category_name)
          |)
          |""".stripMargin
      )
      tx.executeSql(
        "insert into categories (category_name) values (?), (?)",
        Seq(
          "test category 1",
          "test category 2"
        )
      )
      tx.executeSql(sql, Seq(1))
    }

    //then
    result.map { _ =>
      Succeeded
    }
  }
}

object SqliteContextSpec {

  case class CategoryEntity(id: Int, categoryName: String)

  //noinspection TypeAnnotation
  trait CategorySchema {

    val ctx: TestSqliteContext
    import ctx._

    implicit val categoriesInsertMeta = insertMeta[CategoryEntity](
      _.id
    )
    implicit val categoriesUpdateMeta = updateMeta[CategoryEntity](
      _.id
    )

    val categories = quote(querySchema[CategoryEntity]("categories"))
  }

  class CategoryDao(val ctx: TestSqliteContext)
    extends CategorySchema {

    import ctx._

    def getById(id: Int) = {
      ctx.run(categories
        .filter(c => c.id == lift(id))
      )
    }

    def deleteAll() = {
      ctx.run(categories.delete)
    }
  }
}
