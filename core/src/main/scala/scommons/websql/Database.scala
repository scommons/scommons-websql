package scommons.websql

import scommons.websql.raw.{WebSQLInternalQuery, WebSQLInternalResult}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

class Database(db: raw.WebSQLDatabase) {

  def transaction(callback: Transaction => Unit): Future[Unit] = {
    val p = Promise[Unit]()
    
    db.transaction({ tx =>
      callback(new Transaction(tx))
    }, { error =>
      p.failure(js.JavaScriptException(error))
    }, { () =>
      p.success(())
    })
    
    p.future
  }

  def exec(queries: Seq[WebSQLInternalQuery], readOnly: Boolean): Future[Seq[WebSQLInternalResult]] = {
    val p = Promise[Seq[WebSQLInternalResult]]()

    db._db.exec(js.Array(queries: _*), readOnly, { (error, results) =>
      if (!js.isUndefined(error) && error != null) {
        p.failure(js.JavaScriptException(error))
      }
      else {
        p.success(results.toSeq)
      }
    })
    
    p.future
  }
}
