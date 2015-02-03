(ns fester.bolts
  (:require [backtype.storm
             [clojure :refer [emit-bolt! defbolt ack! bolt]]]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]))


(defn write-to-cassandra [conn table key name value]
  (cql/insert conn table {:key key :name name :value value}))

(defbolt fester-raw-metric-bolt ["device-state"] {:prepare true}
  [conf _ collector]
  (let [conn (cc/connect ["127.0.0.1"] {:keyspace "fester"})]
    (bolt
      (execute [{:strs [key name value] :as tuple}]
        (write-to-cassandra conn "raw" key name value)
        (emit-bolt! collector [tuple])
        (ack! collector tuple)))))
