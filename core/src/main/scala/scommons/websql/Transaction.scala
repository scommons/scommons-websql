package scommons.websql

import scommons.websql.raw._

import scala.scalajs.js

class Transaction(underlying: WebSQLTransaction) {

  def executeSql(sqlStatement: String,
                 arguments: Seq[js.Any] = Nil,
                 success: (Transaction, ResultSet) => Unit = null,
                 error: (Transaction, js.Error) => Boolean = null): Unit = {

    val successFn: js.Function2[WebSQLTransaction, WebSQLResultSet, Unit] =
      if (success == null) null
      else { (_, resultSet) =>
        success(this, new ResultSet(resultSet))
      }

    val errorFn: js.Function2[WebSQLTransaction, js.Error, Boolean] =
      if (error == null) null
      else { (_, err) =>
        error(this, err)
      }
    
    underlying.executeSql(sqlStatement, js.Array(arguments: _*), successFn, errorFn)
  }
}
