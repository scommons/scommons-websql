package scommons.websql.io

import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.io.SqliteEncodingSpec._
import scommons.websql.io.dao.CommonDao
import scommons.websql.io.showcase.domain.ShowcaseDBContext
import scommons.websql.{Transaction, WebSQL}

import java.util.{Date, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

class SqliteEncodingSpec extends AsyncTestSpec {
  
  private val entity = TestEntity(
    id = 1,
    encSome = Some(123),
    encNone = None,
    encString = "test name",
    encFalse = false,
    encTrue = true,
    encDouble = Double.MaxValue,
    encBigDecimal = BigDecimal(12.3456789),
    encByte = Byte.MaxValue,
    encShort = Short.MaxValue,
    encLong = Long.MaxValue,
    encFloat = Float.MaxValue,
    encUuid = UUID.randomUUID(),
    encByteArray = Seq[Byte](1, 2, 3),
    encDate = new Date()
  )

  //see:
  //  https://www.sqlite.org/datatype3.html
  //  https://www.sqlite.org/lang_datefunc.html
  //
  private def prepareDb(tx: Transaction): Unit = {
    tx.executeSql(
      """CREATE TABLE test_encodings (
        |  id              integer PRIMARY KEY,
        |  enc_some        int,
        |  enc_none        int,
        |  enc_false       boolean NOT NULL,
        |  enc_true        boolean NOT NULL,
        |  enc_string      text NOT NULL,
        |  enc_double      double NOT NULL,
        |  enc_big_decimal double NOT NULL,
        |  enc_byte        integer NOT NULL,
        |  enc_short       integer NOT NULL,
        |  enc_long        integer NOT NULL,
        |  enc_float       float NOT NULL,
        |  enc_uuid        text NOT NULL,
        |  enc_byte_array  blob NOT NULL,
        |  enc_date        datetime NOT NULL,
        |  created_at      timestamp NOT NULL DEFAULT (strftime('%s','now') * 1000)
        |)
        |""".stripMargin
    )
  }

  it should "encode and decode all supported DB types" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new TestDao(new ShowcaseDBContext(db))
    val beforeCreate = {
      val now = new js.Date()
      val utcMillis = js.Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate(),
        now.getUTCHours(), now.getUTCMinutes(), now.getUTCSeconds())
      utcMillis.toLong
    }

    //when
    val resultF = dao.ctx.db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      for {
        _ <- dao.insert(entity)
        res <- dao.getById(entity.id)
      } yield res
    }
    
    //then
    resultF.map { result =>
      val res = inside(result) {
        case Some(res) => res
      }
      res.createdAt.getTime should be >= beforeCreate
      res shouldBe entity.copy(createdAt = res.createdAt)
    }
  }
}

object SqliteEncodingSpec {

  case class TestEntity(id: Int,
                        encSome: Option[Int],
                        encNone: Option[Int],
                        encString: String,
                        encFalse: Boolean,
                        encTrue: Boolean,
                        encDouble: Double,
                        encBigDecimal: BigDecimal,
                        encByte: Byte,
                        encShort: Short,
                        encLong: Long,
                        encFloat: Float,
                        encUuid: UUID,
                        encByteArray: Seq[Byte],
                        encDate: Date,
                        createdAt: Date = new Date)

  class TestDao(val ctx: ShowcaseDBContext)(implicit ec: ExecutionContext) extends CommonDao {

    import ctx._

    def getById(id: Int): Future[Option[TestEntity]] = {
      getOne("getById", ctx.performIO(ctx.runQuery(
        sql =
          """SELECT
            |  id,
            |  enc_some,
            |  enc_none,
            |  enc_string,
            |  enc_false,
            |  enc_true,
            |  enc_double,
            |  enc_big_decimal,
            |  enc_byte,
            |  enc_short,
            |  enc_long,
            |  enc_float,
            |  enc_uuid,
            |  enc_byte_array,
            |  enc_date,
            |  created_at
            |FROM test_encodings
            |WHERE
            |  id = ?
            |""".stripMargin,
        args = id,
        extractor = TestEntity.tupled
      )))
    }

    def insert(entity: TestEntity): Future[Unit] = {
      ctx.performIO(ctx.runAction(
        sql =
          """INSERT INTO test_encodings (
            |  id,
            |  enc_some,
            |  enc_none,
            |  enc_string,
            |  enc_false,
            |  enc_true,
            |  enc_double,
            |  enc_big_decimal,
            |  enc_byte,
            |  enc_short,
            |  enc_long,
            |  enc_float,
            |  enc_uuid,
            |  enc_byte_array,
            |  enc_date
            |) VALUES (
            |  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            |)
            |""".stripMargin,
        args = (
          entity.id,
          entity.encSome,
          entity.encNone,
          entity.encString,
          entity.encFalse,
          entity.encTrue,
          entity.encDouble,
          entity.encBigDecimal,
          entity.encByte,
          entity.encShort,
          entity.encLong,
          entity.encFloat,
          entity.encUuid,
          entity.encByteArray,
          entity.encDate
        )
      )).map(_ => ())
    }
  }
}
