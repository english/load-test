# load-test

Exposes one function, `blast!`, that allows you to repeatedly make requests against an
endpoint and receive the responses on a given channel. Requests are performed for a given
duration and rate.

e.g. Make POST 5 requests per second to `https://example.com` for 10 seconds

```clj
(let [request {:headers {:Content-Type "text/plain"}
               :url "https://exmaple.com"
               :method "POST"
               :body "hello"}
      response-channel (async/chan 1)]

  (async/go-loop
        []
        (when-some [{:keys [response-time status method url body]} (async/<! response-channel)]
          (println "Got a response!")
          (println "response time: " response-time)
          (println "status: " status)
          (println "method: " method)
          (println "url: " url)
          (println "body: " body)
          (recur)))

  (blast! request response-channel :duration 10 :rate 5))
```
