; Copyright 2016 Jeremy Crosen

; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at

;     http://www.apache.org/licenses/LICENSE-2.0

; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns estuary-streams.publish
  (:require [clojure.core.async :as async :refer [<! >! go-loop]]))

(defprotocol Publisher
  "A generic stateful publisher protocol with a simple start & stop interface"
  (help [this] "Return a vector describing usage as [param-symbol usage-text value]")
  (start! [this chan] "Start the publisher with a channel on which to write")
  (stop! [this start-val] "Stop the publisher with the return value of start!"))

; core.async publisher
(defrecord AsyncPublisher [source-ch started?]
  Publisher
  (help [this] [['source-ch "async channel from which to read via <!" source-ch]
                ['started? "atom storing running state" started?]])
  (start! [this write-ch]
    (when-not @started?
      (swap! started? true)
      (go-loop []
        (when-let [data (<! source-ch)]
          (>! write-ch data)
          (recur)))))
  (stop! [this start-val]
    (when @started?
      (async/close! source-ch))))

(defn make-async-publisher [source-ch]
    (AsyncPublisher. source-ch (atom false)))

; current date/time publisher
(defrecord TimePublisher [interval ]
  Publisher
  (help [this] [['interval "Time in miliseconds to wait between sending time events" interval]])
  (start! [this write-ch]
    (go-loop []
      (when (>! write-ch (str (java.util.Date.)))
        (Thread/sleep interval)
        (recur))))
  (stop! [this start-val]
    (async/close! start-val)))
