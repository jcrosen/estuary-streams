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

(ns estuary-streams.stream
  (:require [clojure.core.async :as async :refer [<! >! chan go-loop sliding-buffer]]
            [estuary-streams.publish :as publish]))

(defprotocol StreamRepository
  "A generic stream repository protocol"
  (get-stream [this uuid] "Get a stream from the db by uuid")
  (remove-stream! [this uuid] "Remove a stream from the db by uuid")
  (save-stream! [this stream] "Save a stream to the db"))

(defprotocol Stream
  "A generic stream protocol"
  (add-client! [this client] "Add a client to the stream")
  (create! [this] "Create a new stream")
  (remove-client! [this client-uuid] "Remove a client from a stream by uuid")
  (stop! [this] "Stop the stream; deny new writes or connections")
  (write! [this data client-uuid] "Write data to a stream"))

(defn generate-volatile-stream-db [] (atom {}))

(deftype VolatileStreamRepository [!db]
  StreamRepository
  (get-stream [this uuid]
    (get-in @!db [:streams uuid]))
  (remove-stream! [this uuid]
    (swap! !db update-in [:streams] dissoc (get-in @!db [:streams]) uuid))
  (save-stream! [this stream]
    (let [uuid (stream :uuid)]
      (swap! !db assoc-in [:streams uuid] stream))))

(deftype CoreAsyncStream [!repository uuid]
  Stream
  (add-client! [this client]
    (when-let [stream (get-stream !repository uuid)]
      (let [pub-chan (chan (sliding-buffer 1024))
            client-uuid (client :uuid)
            pub-map {:client client
                     :start-val (publish/start! client pub-chan)
                     :go-ch (go-loop []
                                (when-let [pub-data (<! pub-chan)]
                                  (write! this pub-data client-uuid)
                                  (recur)))}]
        (save-stream! !repository
          (assoc-in stream [:clients client-uuid] pub-map)))))
  (create! [this]
    (save-stream! !repository {:uuid uuid
                               :sub-chan (chan (sliding-buffer 1024))
                               :metadata {}
                               :publishers {}
                               :subscribers {}}))
  (remove-client! [this client-uuid]
    (when-let [stream (get-stream !repository uuid)]
      (let [pub-map (get-in stream [:clients client-uuid])]
        (publish/stop! (pub-map :client) (pub-map :start-val))
        (async/close! (pub-map :go-ch))
        (save-stream! !repository
          (update-in stream [:clients]
            (dissoc (stream :clients) client-uuid))))))
  (stop! [this]
    {})
  (write! [this data client-uuid]
    (when-let [stream (get-stream !repository uuid)]
      (>! (stream :sub-chan) {:data data
                              :client-uuid client-uuid}))))
