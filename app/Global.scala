import play.api.mvc.WithFilters
import io.ino.play.ConcurrentRequestsLimiter

object Global extends WithFilters(ConcurrentRequestsLimiter)