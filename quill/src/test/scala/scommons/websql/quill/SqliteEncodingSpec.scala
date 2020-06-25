package scommons.websql.quill

import java.time.LocalDate
import java.util.{Date, UUID}

import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.quill.SqliteEncodingSpec._
import scommons.websql.quill.dao.CommonDao
import scommons.websql.{Transaction, WebSQL}
import showcase.domain.ShowcaseDBContext

import scala.concurrent.Future
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
    encByteArray = Array[Byte](1, 2, 3),
    encDate = new Date(),
    encLocalDate = LocalDate.now()
  )

  //see:
  //  https://www.sqlite.org/datatype3.html
  //  https://www.sqlite.org/lang_datefunc.html
  //
  private def prepareDb(tx: Transaction): Unit = {
    tx.executeSql(
      """create table test_encodings (
        |  id              integer primary key,
        |  enc_some        int,
        |  enc_none        int,
        |  enc_false       boolean not null,
        |  enc_true        boolean not null,
        |  enc_string      text not null,
        |  enc_double      double not null,
        |  enc_big_decimal double not null,
        |  enc_byte        integer not null,
        |  enc_short       integer not null,
        |  enc_long        integer not null,
        |  enc_float       float not null,
        |  enc_uuid        text not null,
        |  enc_byte_array  blob not null,
        |  enc_date        datetime not null,
        |  enc_local_date  date not null,
        |  created_at      timestamp not null default (strftime('%s','now') * 1000)
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
      val Some(res) = result
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
                        encLocalDate: LocalDate,
                        createdAt: Date = new Date)

  //noinspection TypeAnnotation
  trait TestSchema {

    val ctx: ShowcaseDBContext
    import ctx._

    implicit val testEncodingsInsertMeta = insertMeta[TestEntity](
      _.createdAt
    )
    val testEncodings = quote(querySchema[TestEntity]("test_encodings"))
  }

  class TestDao(val ctx: ShowcaseDBContext) extends CommonDao
    with TestSchema {
    
    import ctx._

    def getById(id: Int): Future[Option[TestEntity]] = {
      getOne("getById", ctx.performIO(ctx.run(testEncodings
        .filter(c => c.id == lift(id))
      )))
    }

    def insert(entity: TestEntity): Future[Unit] = {
      ctx.performIO(ctx.run(testEncodings
        .insert(lift(entity))
      ).map(_ => ()))
    }
  }
}
