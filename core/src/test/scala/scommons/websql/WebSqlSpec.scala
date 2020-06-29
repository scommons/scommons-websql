package scommons.websql

import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.raw.WebSQLInternalQuery

import scala.scalajs.js

class WebSqlSpec extends AsyncTestSpec {
  
  it should "return auth error if readOnly=true when exec" in {
    //given
    val db = WebSQL.openDatabase(":memory:")

    //when
    val resultF = db.exec(
      Seq(new WebSQLInternalQuery {
        override val sql =
          """create table categories (
            |  id     integer primary key,
            |  name   text
            |)
            |""".stripMargin
        override val args = js.Array[js.Any]()
      }),
      readOnly = true
    )
    
    //then
    resultF.flatMap { results =>
      results.size shouldBe 1
      inside(results.head) { case result =>
        result.error.getOrElse(null).message shouldBe {
          "could not prepare statement (23 not authorized)"
        }
      }
    }
  }
  
  it should "return SQL error when exec" in {
    //given
    val db = WebSQL.openDatabase(":memory:")

    //when
    val resultF = db.exec(
      Seq(new WebSQLInternalQuery {
        override val sql =
          """
            |insert into categories (name) values (?)
          """.stripMargin
        override val args = js.Array[js.Any]("category")
      }),
      readOnly = false
    )
    
    //then
    resultF.flatMap { results =>
      results.size shouldBe 1
      inside(results.head) { case result =>
        result.error.getOrElse(null).message shouldBe {
          "SQLITE_ERROR: no such table: categories"
        }
      }
    }
  }
  
  it should "execute SQLs using exec method" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val beforeF = db.transaction { tx =>
      tx.executeSql(
        """create table categories (
          |  id     integer primary key,
          |  name   text
          |)
          |""".stripMargin
      )
    }

    //when
    val resultF = beforeF.flatMap { _ =>
      db.exec(Seq(
        new WebSQLInternalQuery {
          override val sql = "insert into categories (name) values (?), (?)"
          override val args = js.Array[js.Any]("category 1", "category 2")
        },
        new WebSQLInternalQuery {
          override val sql = "select * from categories where id = ?"
          override val args = js.Array[js.Any](1)
        }
      ), readOnly = false)
    }
    
    //then
    resultF.flatMap { results =>
      results.size shouldBe 2
      inside(results.head) { case result =>
        result.error.getOrElse(null) shouldBe null
        result.insertId.getOrElse(0.0) shouldBe 2
        result.rowsAffected.getOrElse(0.0) shouldBe 2
        result.rows.getOrElse(js.Array[js.Object]()).toSeq shouldBe Nil
      }
      inside(results(1)) { case result =>
        result.error.getOrElse(null) shouldBe null
        result.insertId.getOrElse(0.0) shouldBe 0.0
        result.rowsAffected.getOrElse(0.0) shouldBe 0.0
        result.rows.getOrElse(js.Array[js.Object]()).toSeq.map { row =>
          val r = row.asInstanceOf[js.Dynamic]
          (r.id.asInstanceOf[Int], r.name.asInstanceOf[String])
        } shouldBe Seq(
          (1, "category 1")
        )
      }
      
      var categories: Seq[(Int, String)] = Nil
      db.transaction { tx =>
        tx.executeSql(
          sqlStatement = "select * from categories order by id",
          arguments = Nil,
          success = { (_, resultSet) =>
            categories = resultSet.rows.map { row =>
              val r = row.asInstanceOf[js.Dynamic]
              (r.id.asInstanceOf[Int], r.name.asInstanceOf[String])
            }
          },
          error = null
        )
      }.map { _ =>
        categories shouldBe Seq(
          (1, "category 1"),
          (2, "category 2")
        )
      }
    }
  }
  
  it should "execute PRAGMA foreign_keys=ON using exec method" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val beforeF = db.transaction { tx =>
      tx.executeSql(
        """create table categories (
          |  id     integer primary key,
          |  name   text
          |)
          |""".stripMargin
      )
      tx.executeSql("insert into categories (name) values ('category 1'), ('category 2')")
    }

    //when
    val resultF = beforeF.flatMap { _ =>
      db.exec(Seq(new WebSQLInternalQuery {
        override val sql = "PRAGMA foreign_keys = ON"
        override val args = js.Array[js.Any]()
      }), readOnly = false)
    }
    
    //then
    resultF.flatMap { results =>
      results.size shouldBe 1
      inside(results.head) { case result =>
        result.error.getOrElse(null) shouldBe null
        result.insertId.getOrElse(0.0) shouldBe 2
        result.rowsAffected.getOrElse(0.0) shouldBe 2
        result.rows.getOrElse(js.Array[js.Object]()).toSeq shouldBe Nil
      }
      
      db.transaction { tx =>
        tx.executeSql(
          """create table products (
            |  id     integer primary key,
            |  cat_id integer not null,
            |  name   text,
            |  CONSTRAINT category_fk FOREIGN KEY (cat_id) REFERENCES categories (id)
            |)
            |""".stripMargin
        )
        tx.executeSql("insert into products (cat_id, name) values (3, 'product 1')")
      }.failed.map { ex =>
        ex.getMessage shouldBe "Error: SQLITE_CONSTRAINT: FOREIGN KEY constraint failed"
      }
    }
  }
}
