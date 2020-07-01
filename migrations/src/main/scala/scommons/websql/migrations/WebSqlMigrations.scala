package scommons.websql.migrations

import scommons.websql.{Database, Transaction}
import scommons.websql.migrations.WebSqlMigrations._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WebSqlMigrations(db: Database) {
  
  private[migrations] val logger: String => Unit = println

  def run(all: Seq[WebSqlMigration]): Future[Unit] = {
    var count = 0
    
    all.sortBy(_.version).foldLeft(prepareDb()) { (resF, curr) =>
      resF.flatMap { _ =>
        db.transaction { tx =>
          checkVersion(tx, curr, { () =>
            logger(s"DB: applying ${curr.version} ${curr.name}")
            count += 1
            
            curr.sql.split(";").map(_.trim).filter(_.nonEmpty).foreach { sql =>
              tx.executeSql(sql)
            }
          })
        }
      }
    }.map { _ =>
      if (count > 0) logger(s"DB: $count migration(s) were applied successfully")
      else logger("DB is up to date")
    }.recover {
      case ex =>
        logger(s"DB: ${ex.getMessage}")
        throw ex
    }
  }
  
  private def checkVersion(tx: Transaction, m: WebSqlMigration, applyChanges: () => Unit): Unit = {
    tx.executeSql(
      sqlStatement = s"select * from $dbTable where version = ?",
      arguments = Seq(m.version),
      success = { (_, resultSet) =>
        if (resultSet.rows.isEmpty) {
          applyChanges()
          
          tx.executeSql(
            sqlStatement = s"insert into $dbTable (version, name) values (?, ?)",
            arguments = Seq(m.version, m.name)
          )
        }
      },
      error = null
    )
  }
  
  private def prepareDb(): Future[Unit] = {
    db.transaction { tx =>
      tx.executeSql(
        s"""create table if not exists $dbTable (
           |  version  integer primary key,
           |  name     text not null
           |)
           |""".stripMargin
      )
    }
  }
}

object WebSqlMigrations {

  private val dbTable = "schema_versions"
}
