(ns load-test.core
  (:require [clojure.core.async :as async]
            [org.httpkit.client :as http]))

(defn- enqueue-requests! [channel request rate]
  (let [sleep-time (long (/ 1000 rate))]
    (async/go
      (while (async/>! channel request)
        (async/<! (async/timeout sleep-time))))))

(defn- enqueue-requests-for-duration-at-rate! [request-channel request duration rate]
  (async/go
    (async/<! (async/timeout (* duration 1000)))
    (async/close! request-channel))
  (enqueue-requests! request-channel request rate))

(defn- send-requests! [request-channel response-channel]
  (async/go-loop
    []
    (when-some [request (async/<! request-channel)]
      (let [start-time (System/currentTimeMillis)]
        (http/request request (fn [response]
                                (async/put! response-channel
                                            (assoc response
                                                   :time (System/currentTimeMillis)
                                                   :response-time (- (System/currentTimeMillis)
                                                                     start-time))))))
      (recur))))

(defn blast!
  "Make <request> for <duration> seconds at a rate of <rate> requests per second
  and put response times to <out-channel>"
  [request out-channel & {:keys [duration rate] :or {duration 10 rate 5}}]
  (let [request-channel (async/chan 10000) ;; how big?
        response-channel (async/chan 10000 (comp (map #(assoc % :url (get-in % [:opts :url])))
                                                 (map #(assoc % :method (get-in % [:opts :method])))
                                                 (map #(select-keys % [:response-time :status :method :url :body]))))]
    (enqueue-requests-for-duration-at-rate! request-channel request duration rate)
    (async/pipe response-channel out-channel)
    (send-requests! request-channel response-channel)))
