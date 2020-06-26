package scommons.websql.migrations

import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.WebSQL

import scala.scalajs.js

class WebSqlMigrationsSpec extends AsyncTestSpec {
  
  it should "run migrations within transactions" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db)
    val all = Seq(
      WebSqlMigration(
        version = 1,
        name = "test",
        sql =
          """-- comment 1
            |-- comment 2
            |create table test_migrations (
            |  id              integer primary key, -- inline comment
            |  original_name   text
            |);
            |
            |/*
            | * multi-line comment
            | */
            |
            |alter table test_migrations rename column original_name to new_name;
            |
            |insert into test_migrations (new_name) values ('test 1'), ('test 2');
            |
            |""".stripMargin
      )
    )

    //when
    val resultF = migrations.run(all)
    
    //then
    resultF.flatMap { _ =>
      var results: Seq[(Int, String)] = Nil
      db.transaction { tx =>
        tx.executeSql(
          sqlStatement = "select * from test_migrations order by id",
          arguments = Nil,
          success = { (_, resultSet) =>
            results = resultSet.rows.map { row =>
              val r = row.asInstanceOf[js.Dynamic]
              (r.id.asInstanceOf[Int], r.new_name.asInstanceOf[String])
            }
          },
          error = null
        )
      }.map { _ =>
        results shouldBe Seq(
          (1, "test 1"),
          (2, "test 2")
        )
      }
    }
  }
}
