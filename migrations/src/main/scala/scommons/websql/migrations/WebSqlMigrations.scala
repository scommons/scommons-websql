package scommons.websql.migrations

import scommons.websql.migrations.WebSqlMigrations._
import scommons.websql.raw.WebSQLInternalQuery
import scommons.websql.{Database, Transaction}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

class WebSqlMigrations(db: Database) {
  
  private[migrations] val logger: String => Unit = println

  def run(all: Seq[WebSqlMigration]): Future[Unit] = {
    var count = 0
    for {
      currVersions <- readCurrentVersions()
      _ <- all
        .filter(m => !currVersions.contains(m.version))
        .sortBy(_.version)
        .foldLeft(Future.successful(())) { (resF, m) =>
          resF.flatMap { _ =>
            val (nonTransactional, transactional) = m.sql.split(";")
              .map(_.trim)
              .filter(_.nonEmpty)
              .partition(_.contains("non-transactional"))
            
            for {
              _ <- runNonTransactional(nonTransactional)
              applied <- runTransactional(m, transactional)
            } yield {
              if (applied) {
                count += 1
              }
            }
          }
        }.recover { case ex =>
          logger(s"DB: ${ex.getMessage}")
          throw ex
        }
    } yield {
      if (count > 0) logger(s"DB: $count migration(s) were applied successfully")
      else logger("DB is up to date")
    }
  }

  private def runNonTransactional(statements: Seq[String]): Future[_] = {
    if (statements.nonEmpty) {
      db.exec(statements.map { statement =>
        new WebSQLInternalQuery {
          override val sql = statement
          override val args = js.Array[js.Any]()
        }
      }, readOnly = false)
    }
    else Future.successful(())
  }

  private def runTransactional(m: WebSqlMigration, statements: Seq[String]): Future[Boolean] = {
    var applied = false
    
    db.transaction { tx =>
      checkVersion(tx, m, { () =>
        logger(s"DB: applying ${m.version} ${m.name}")
        
        statements.foreach { statement =>
          tx.executeSql(statement)
        }
        
        applied = true
      })
    }.map(_ => applied)
  }
  
  private def checkVersion(tx: Transaction, m: WebSqlMigration, applyChanges: () => Unit): Unit = {
    tx.executeSql(
      sqlStatement = s"select version from $dbTable where version = ?",
      arguments = Seq(m.version),
      success = { (_, resultSet) =>
        if (resultSet.rows.isEmpty) {
          applyChanges()
          
          tx.executeSql(
            sqlStatement = s"insert into $dbTable (version, name) values (?, ?)",
            arguments = Seq(m.version, m.name)
          )
        }
      }
    )
  }
  
  private def readCurrentVersions(): Future[Set[Int]] = {
    var results = Set.empty[Int]
    
    db.transaction { tx =>
      tx.executeSql(
        s"""create table if not exists $dbTable (
           |  version  integer primary key,
           |  name     text not null
           |)
           |""".stripMargin
      )
      
      tx.executeSql(
        sqlStatement = s"select version from $dbTable",
        success = { (_, resultSet) =>
          results = resultSet.rows.map { row =>
            row.version.asInstanceOf[Int]
          }.toSet
        }
      )
    }.map(_ => results)
  }
}

object WebSqlMigrations {

  private val dbTable = "schema_versions"
}
