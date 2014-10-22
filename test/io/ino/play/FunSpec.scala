package io.ino.play

import akka.util.Timeout
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.{PlaySpec, OneServerPerSuite}
import play.api.mvc.Action
import play.api.mvc.Results.Ok
import play.api.test.FakeApplication
import play.api.test.Helpers._
import scala.concurrent.duration._
import play.api.libs.concurrent.Promise
import java.util.concurrent.atomic.AtomicInteger
import akka.util.Timeout.durationToTimeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FunSpec extends PlaySpec with OneServerPerSuite with EitherValues with BeforeAndAfterEach {

  private val activeRequests = new AtomicInteger(0)
  private var maxActiveRequests: Int = 0

  override def beforeEach(): Unit = {
    activeRequests.set(0)
    maxActiveRequests = 0
  }

  implicit override lazy val app: FakeApplication =
    FakeApplication(
      additionalConfiguration = Map(
          "maxProcessedRequests" -> 10,
          "maxQueuedRequests" -> 10),
      withRoutes = {
        case ("GET", "/resource1") => Action.async { request =>
          activeRequests.synchronized {
            maxActiveRequests = Math.max(maxActiveRequests, activeRequests.incrementAndGet)
          }

          Promise.timeout({
            val active = activeRequests.decrementAndGet
            val i = request.queryString("i").head
            Ok(s"$i:$active:$maxActiveRequests")
          }, 100 millis)
        }
      }
    )

  private implicit val defaultAwaitTimeout: Timeout = 10.seconds

  "RequestQueueFilter" should {

    "limit concurrent requests processed" in {
      val responseFutures = (1 to 20).map(i => wsUrl("/resource1").withQueryString("i" -> i.toString).get())
      
      val responses = await(Future.sequence(responseFutures))
      // println("Got responses:\n" + responses.map(_.body).mkString("\n"))
      
      maxActiveRequests mustBe 10
    }

    "send 429 if queued requests exceed limit" in {
      val responseFutures = (1 to 40).map(i => wsUrl("/resource1").withQueryString("i" -> i.toString).get())
      
      val responses = await(Future.sequence(responseFutures))
      // 10 concurent + 10 queued requests are allowed, so we expect < 20 rejected requests
      responses.filter(_.status == 429).size must (be > 0 and be < 20)
    }

  }

}
