package scommons.websql.quill

import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.{NamingStrategy, SqliteDialect}

object WebSqlDialect extends SqliteDialect {

  private abstract class TokenizerImpl[T] extends Tokenizer[T] {
    var isTopQuery = true
  }
  
  override def defaultTokenizer(implicit naming: NamingStrategy): Tokenizer[Ast] =
    new TokenizerImpl[Ast] {
      private val stableTokenizer = astTokenizer(this, naming)

      def token(v: Ast): Token = stableTokenizer.token(v)
    }

  override implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast],
                                          strategy: NamingStrategy): Tokenizer[SqlQuery] = Tokenizer[SqlQuery] {
    case q: FlattenSqlQuery =>
      val curr = astTokenizer.asInstanceOf[TokenizerImpl[Ast]]
      val query = {
        if (curr.isTopQuery) {
          q.copy(select = q.select.zipWithIndex.map { case (s, index) =>
            s.copy(alias = Some(s"_$index"))
          })(q.quat)
        }
        else q
      }
      curr.isTopQuery = false
      new FlattenSqlQueryTokenizerHelper(query).apply
    case value => super.sqlQueryTokenizer.token(value)
  }
}
