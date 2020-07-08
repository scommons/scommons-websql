package scommons.websql.migrations

import org.scalatest.{Assertion, Succeeded}
import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.migrations.WebSqlMigrationsSpec._
import scommons.websql.migrations.raw.WebSqlMigrationBundleItem
import scommons.websql.{Database, WebSQL}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

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
    loggerMock.expects("DB: migrating to version 1 - test migration 1")
    loggerMock.expects("DB: migrating to version 2 - test migration 2")
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
    loggerMock.expects("DB: migrating to version 1 - test migration 1")
    loggerMock.expects("DB: migrating to version 2 - test migration 2")
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
    loggerMock.expects("DB: migrating to version 1 - test migration 1")
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
    loggerMock.expects("DB: migrating to version 1 - test migration 1")
    loggerMock.expects("DB: migrating to version 2 - test migration 2")
    loggerMock.expects(
      """DB: Error: SQLITE_ERROR: near ")": syntax error"""
    )

    val all = Seq(
      migration1,
      migration2.copy(
        sql = s"${migration2.sql}; insert into test_migrations (new_name) values ('test 5'), ();"
      ),
      WebSqlMigration(
        version = 3,
        name = "test migration 3",
        sql = "insert into test_migrations (new_name) values ('test 7');"
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
    loggerMock.expects("DB: migrating to version 1 - test migration 1")
    loggerMock.expects("DB: migrating to version 2 - test migration 2")
    loggerMock.expects(
      """DB: Error: SQLITE_ERROR: near ")": syntax error"""
    )
    loggerMock.expects("DB: migrating to version 2 - test migration 2")
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

  it should "run non-transactional migration" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: migrating to version 1 - non-transactional migration")
    loggerMock.expects("DB: 1 migration(s) were applied successfully")

    val all = Seq(WebSqlMigration(
      version = 1,
      name = "non-transactional migration",
      sql =
        """-- non-transactional
          |PRAGMA foreign_keys = ON;
          |""".stripMargin
    ))

    //when
    val resultF = migrations.run(all)

    //then
    resultF.map { _ =>
      Succeeded
    }
  }

  it should "run non-transactional and transactional migrations" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: migrating to version 1 - non-transactional migration")
    loggerMock.expects("DB: migrating to version 2 - transactional migration")
    loggerMock.expects("DB: Error: SQLITE_CONSTRAINT: FOREIGN KEY constraint failed")

    val all = Seq(
      WebSqlMigration(1, "non-transactional migration",
        """
          |-- non-transactional
          |PRAGMA foreign_keys = ON;
          |
          |create table categories (
          |  id     integer primary key,
          |  name   text
          |);
          |
          |create table products (
          |  id     integer primary key,
          |  cat_id integer not null,
          |  name   text,
          |  CONSTRAINT category_fk FOREIGN KEY (cat_id) REFERENCES categories (id)
          |);
          |""".stripMargin
      ),
      WebSqlMigration(2, "transactional migration",
        """
          |insert into categories (name) values ('category 1'), ('category 2');
          |insert into products (cat_id, name) values (3, 'product 1')
          |""".stripMargin
      )
    )

    //when
    val resultF = migrations.run(all)

    //then
    resultF.failed.map { ex =>
      ex.getMessage shouldBe "Error: SQLITE_CONSTRAINT: FOREIGN KEY constraint failed"
    }
  }
  
  it should "run migrations from bundle.json" in {
    //given
    val loggerMock = mockFunction[String, Unit]
    val db = WebSQL.openDatabase(":memory:")
    val migrations = new WebSqlMigrations(db) {
      override val logger = loggerMock
    }
    loggerMock.expects("DB: migrating to version 1 - initial db structure")
    loggerMock.expects("DB: migrating to version 2 - rename db field")
    loggerMock.expects("DB: 2 migration(s) were applied successfully")

    val bundle = TestMigrationsBundle

    //when
    val resultF = migrations.runBundle(bundle)

    //then
    resultF.flatMap { _ =>
      assertDb(db, Seq(
        (1, "test 1"),
        (2, "test 2")
      ))
    }
  }

  it should "fail if cannot parse migration version and name" in {
    //given
    val itemMock = mock[WebSqlMigrationBundleItemMock]
    val loggerMock = mockFunction[String, Unit]
    val migrations = new WebSqlMigrations(null) {
      override val logger = loggerMock
    }
    val fileName = "V01_test.SQL"
    val error = s"Cannot parse migration version and name from: $fileName"

    (itemMock.file _).expects().returning(fileName)
    loggerMock.expects(s"DB: Error: $error")

    val bundle = js.Array(itemMock.asInstanceOf[WebSqlMigrationBundleItem])
      .asInstanceOf[WebSqlMigrationBundle]

    //when
    val resultF = migrations.runBundle(bundle)

    //then
    resultF.failed.map { ex =>
      ex.getMessage shouldBe error
    }
  }

  private def assertDb(db: Database, expected: Seq[(Int, String)]): Future[Assertion] = {
    var results: Seq[(Int, String)] = Nil
    db.transaction { tx =>
      tx.executeSql(
        sqlStatement = "select * from test_migrations order by id",
        success = { (_, resultSet) =>
          results = resultSet.rows.map { row =>
            (row.id.asInstanceOf[Int],
              row.new_name.asInstanceOf[String])
          }
        }
      )
    }.map { _ =>
      results shouldBe expected
    }
  }
}

object WebSqlMigrationsSpec {

  @JSExportAll
  trait WebSqlMigrationBundleItemMock {
    
    def file: String
    def content: String
  }
}
