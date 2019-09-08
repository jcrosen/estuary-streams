(ns streams.core
  (:require [java-time :as jt]))

(defn -main []
  (println "Running -main at" (jt/format (jt/formatter :iso-date-time) (jt/local-date-time))))
