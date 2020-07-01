package scommons.websql.migrations

import org.scalatest.Assertion
import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.{Database, WebSQL}

import scala.concurrent.Future
import scala.scalajs.js

class WebSqlMigrationsSpec extends AsyncTestSpec {
  
  private val migration1 = WebSqlMigration(
    version = 1,
    name = "test migration 1",
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
  private val migration2 = WebSqlMigration(
    version = 2,
    name = "test migration 2",
    sql = "insert into test_migrations (new_name) values ('test 3'), ('test 4');"
  )
  
  it should "run migrations on new database in correct order" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: applying 1 test migration 1")
    loggerMock.expects("DB: applying 2 test migration 2")
    loggerMock.expects("DB: 2 migration(s) were applied successfully")
    
    val all = Seq(
      migration2,
      migration1
    )

    //when
    val resultF = migrations.run(all)
    
    //then
    resultF.flatMap { _ =>
      assertDb(db, Seq(
        (1, "test 1"),
        (2, "test 2"),
        (3, "test 3"),
        (4, "test 4")
      ))
    }
  }
  
  it should "run migrations on existing database" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: applying 1 test migration 1")
    loggerMock.expects("DB: applying 2 test migration 2")
    loggerMock.expects("DB: 1 migration(s) were applied successfully")
    loggerMock.expects("DB: 1 migration(s) were applied successfully")
    
    val beforeF = {
      migrations.run(Seq(migration1)).flatMap { _ =>
        assertDb(db, Seq(
          (1, "test 1"),
          (2, "test 2")
        ))
      }
    }
    val all = Seq(
      migration1,
      migration2
    )

    //when
    val resultF = beforeF.flatMap { _ =>
      migrations.run(all)
    }
    
    //then
    resultF.flatMap { _ =>
      assertDb(db, Seq(
        (1, "test 1"),
        (2, "test 2"),
        (3, "test 3"),
        (4, "test 4")
      ))
    }
  }
  
  it should "skip migrations on up to date database" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: applying 1 test migration 1")
    loggerMock.expects("DB: 1 migration(s) were applied successfully")
    loggerMock.expects("DB is up to date")
    
    val beforeF = {
      migrations.run(Seq(migration1)).flatMap { _ =>
        assertDb(db, Seq(
          (1, "test 1"),
          (2, "test 2")
        ))
      }
    }
    val all = Seq(migration1)

    //when
    val resultF = beforeF.flatMap { _ =>
      migrations.run(all)
    }
    
    //then
    resultF.flatMap { _ =>
      assertDb(db, Seq(
        (1, "test 1"),
        (2, "test 2")
      ))
    }
  }

  it should "fail and rollback changes if migration error" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: applying 1 test migration 1")
    loggerMock.expects("DB: applying 2 test migration 2")
    loggerMock.expects(
      """DB: Error: SQLITE_ERROR: near ")": syntax error"""
    )

    val all = Seq(
      migration1,
      migration2.copy(
        sql = s"${migration2.sql}; insert into test_migrations (new_name) values ('test 5'), ();"
      )
    )

    //when
    val resultF = migrations.run(all)

    //then
    resultF.failed.flatMap { ex =>
      ex.getMessage shouldBe """Error: SQLITE_ERROR: near ")": syntax error"""
      
      assertDb(db, Seq(
        (1, "test 1"),
        (2, "test 2")
      ))
    }
  }

  it should "re-run migration after fixing the error" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: applying 1 test migration 1")
    loggerMock.expects("DB: applying 2 test migration 2")
    loggerMock.expects(
      """DB: Error: SQLITE_ERROR: near ")": syntax error"""
    )
    loggerMock.expects("DB: applying 2 test migration 2")
    loggerMock.expects("DB: 1 migration(s) were applied successfully")
    
    val beforeF = migrations.run(Seq(
      migration1,
      migration2.copy(
        sql = s"${migration2.sql}; insert into test_migrations (new_name) values ('test 5'), ();"
      )
    ))

    val all = Seq(
      migration1,
      migration2.copy(
        sql = s"${migration2.sql}; insert into test_migrations (new_name) values ('test 5');"
      )
    )

    val resultF = beforeF.failed.flatMap { ex =>
      //then
      ex.getMessage shouldBe """Error: SQLITE_ERROR: near ")": syntax error"""
      
      //when
      migrations.run(all)
    }

    //then
    resultF.flatMap { _ =>
      assertDb(db, Seq(
        (1, "test 1"),
        (2, "test 2"),
        (3, "test 3"),
        (4, "test 4"),
        (5, "test 5")
      ))
    }
  }

  private def assertDb(db: Database, expected: Seq[(Int, String)]): Future[Assertion] = {
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
      results shouldBe expected
    }
  }
}
