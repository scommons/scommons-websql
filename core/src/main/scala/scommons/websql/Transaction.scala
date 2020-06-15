package scommons.websql

import scommons.websql.raw._

import scala.scalajs.js

class Transaction(underlying: WebSQLTransaction) {

  def executeSql(sqlStatement: String): Unit = {
    executeSql(sqlStatement, Nil)
  }
  
  def executeSql(sqlStatement: String, arguments: Seq[js.Any]): Unit = {
    executeSql(sqlStatement, arguments, null, null)
  }
  
  def executeSql(sqlStatement: String,
                 arguments: Seq[js.Any],
                 success: (Transaction, ResultSet) => Unit,
                 error: (Transaction, js.Error) => Boolean): Unit = {

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
