package io.ino.play

import play.api.Logger
import play.api.mvc._
import play.api.mvc.Results.TooManyRequest
import scala.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue
import play.api.Play
import play.api.Play.current
import scala.concurrent.Promise
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object ConcurrentRequestsLimiter extends Filter {
  
  private type FilterFun = RequestHeader => Future[Result]
  
  private val active = new AtomicInteger(0)
  private val queue = new ConcurrentLinkedQueue[(Promise[Result], FilterFun, RequestHeader)]()
  
  private lazy val maxProcessedRequests = current.configuration.getInt("maxProcessedRequests").getOrElse(100)
  private lazy val maxQueuedRequests = current.configuration.getInt("maxQueuedRequests").getOrElse(1000)
  
  def apply(next: FilterFun)(request: RequestHeader): Future[Result] = {
    Logger.debug(s"Comparing active=${active.get} with maxProcessedRequests=$maxProcessedRequests")
    if(active.get >= maxProcessedRequests) {
      if(queue.size() > maxQueuedRequests) {
        Logger.debug("Rejecting request with status 429 because of too many queued requests.")
        Future.successful(TooManyRequest)
      } else {
        Logger.debug(s"Delaying request (${request.queryString}) because there are already more than $maxProcessedRequests requests processed.")
        val p = Promise[Result]()
        queue.offer((p, next, request))
        p.future
      }
    } else {
      processRequest(next, request)
    }
  }

  /**
   * Process the given request header with the provided next filter function.
   * The active counter is updated at start/end accordingly.
   *
   * When the request processing is completed, a possibly queued request will be taken from the queue
   * and will be processed as well.
   */
  private def processRequest(next: FilterFun, request: RequestHeader): Future[Result] = {
    val a = active.incrementAndGet()
    
    Logger.debug(s"Processing request (${request.queryString}) with incremented active=$a")
    val result = next(request)
    
    result.onComplete { _ =>
      
      active.decrementAndGet()
    
      Option(queue.poll()).foreach { case (p, next, request) =>
        process(p, next, request)
      }
    }
    
    result
  }

  private def process(p: Promise[Result], next: FilterFun, request: RequestHeader): Unit = {
    Logger.debug("Completing a queued request...")
    val res = processRequest(next, request)
    p.completeWith(res)
  }

}