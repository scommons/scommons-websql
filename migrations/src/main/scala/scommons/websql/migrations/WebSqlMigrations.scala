package scommons.websql.migrations

import scommons.websql.Database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WebSqlMigrations(db: Database) {

  def run(all: Seq[WebSqlMigration]): Future[Unit] = {
    all.foldLeft(Future.successful(())) { (resF, curr) =>
      resF.flatMap { _ =>
        db.transaction { tx =>
          curr.sql.split(";").map(_.trim).filter(_.nonEmpty).foreach { sql =>
            tx.executeSql(sql)
          }
        }
      }
    }
  }
}
