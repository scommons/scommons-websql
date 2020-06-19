package scommons.websql.quill.dao

import org.scalatest.Assertion
import scommons.nodejs.test.AsyncTestSpec

import scala.concurrent.Future

class CommonDaoSpec extends AsyncTestSpec {

  private val commonDao = new SomeTestDao

  it should "fail if there are more than 1 elements" in {
    //given
    val futureResults = Future.successful(List(1, 2))

    //when
    val result = commonDao.getSomething(futureResults)

    //then
    val q = "SomeTestDao.getSomething"
    result.failed.map { ex =>
      ex.getMessage shouldBe s"Expected only single result, but got 2 in $q"
    }
  }

  it should "not fail if there are 0 elements" in {
    //given
    val futureResults = Future.successful(List[Int]())

    //when
    val result = commonDao.getOne("queryName", futureResults)
    
    //then
    result.map { res =>
      res shouldBe None
    }
  }

  it should "return single element" in {
    //given
    val futureResults = Future.successful(List(1))

    //when
    val result = commonDao.getOne("queryName", futureResults)
    
    //then
    result.map { res =>
      res shouldBe Some(1)
    }
  }

  it should "return true if updateCount > 0" in {
    
    def check(value: Int, expected: Boolean): Future[Assertion] = {
      //when
      val result = commonDao.isUpdated(Future.successful(value))
      
      //then
      result.map { res =>
        res shouldBe expected
      }
    }
    
    //when & then
    check(-1, expected = false)
    check(0, expected = false)
    check(1, expected = true)
    check(2, expected = true)
  }
}

class SomeTestDao extends CommonDao {

  def getSomething[T](results: Future[List[T]]): Future[Option[T]] = {
    getOne("getSomething", results)
  }
}
