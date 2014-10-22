# Play! Framework 2 Concurrent Requests Limiter

This Play! 2 app shows how to limit the concurrently processed requests in an async/non-blocking application.
Requests exceeding this limit are queued and processed as soon as other requests are finished.

This may be useful in such a situation:

* Your play app is working async/non-blocking (e.g. by only using the WS library and/or asynchronous drivers)
* The app is receiving very many requests in a very short time
* Processing all requests concurrently produces so much data that the jvm complains with an OutOfMemoryError gc overhead limit exceeded
* A successful (expected) response is preferred over a rejected request if there are too many concurrent requests, even if it takes some "more" time until the response is available.

The solution (`io.ino.play.ConcurrentRequestsLimiter`) works with a limit of concurrently processed requests (configurable via
`maxProcessedRequests`, default 100), so that incoming requests, that would exceed this limit, are queued until other requests are completed.
Upon the completion of a request, the oldest queued request will then be processed.
Additionally, it's possible to limit the number of queued requests (configurable via `maxQueuedRequests`, default 1000).
Incoming requests that would exceed this limit are then in fact rejected with HTTP status 429 ("Too many requests").

## License

The license is Apache 2.0, see LICENSE.txt.