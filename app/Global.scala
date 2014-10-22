import play.api.mvc.WithFilters
import play.api._
import io.ino.play.ConcurrentRequestsLimiter

// object Global extends GlobalSettings
object Global extends WithFilters(ConcurrentRequestsLimiter)