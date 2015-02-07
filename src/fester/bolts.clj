(ns fester.bolts
  (:require [backtype.storm
             [clojure :refer [emit-bolt! defbolt ack! bolt]]]
            [clojurewerkz.cassaforte.client :as cc]
            [fester.aggregators :refer [avg]]
            [clojurewerkz.cassaforte.cql :as cql])
  (:import [org.cliffc.high_scale_lib NonBlockingHashMap]))


(def aggregator-map
  {:avg avg})

(defn write-to-cassandra [conn table ts key value]
  (cql/insert conn table {:time ts :key key :value value}))

(defn write-rollup-to-cassandra [conn ts rollup key value]
  (cql/insert conn "rollups"
    {:time ts :rollup rollup :key key :value value}))

(defn extract-value [[_ _ value]]
  value)

(defn period-lasts? [points duration]
  (let [ts1 (first (first points))
        ts2 (first (last (next points)))]
    (when (and ts1 ts2)
      (>= (- ts2 ts1) duration))))

(defbolt fester-raw-metric-bolt ["ts" "key" "value"] {:prepare true}
  [conf _ collector]
  (let [conn (cc/connect ["127.0.0.1"] {:keyspace "fester"})]
    (bolt
      (execute [{:strs [ts key value] :as tuple}]
        (emit-bolt! collector [ts key value])
        (write-to-cassandra conn "raw" ts key value)
        (ack! collector tuple)))))

(defbolt fester-rollup-metric-bolt ["ts" "key" "value"]
  {:prepare true
   :params [period aggregator-type]}
  [conf _ collector]
  (let [nbhm (NonBlockingHashMap.)
        conn (cc/connect ["127.0.0.1"] {:keyspace "fester"})
        aggregator (aggregator-type aggregator-map)]
    (bolt
      (execute [{:strs [ts key value] :as tuple}]
        (ack! collector tuple)
        (let [stored (.get nbhm key)]
          (if (not stored)
            (.put nbhm key [[ts key value]])
            (let [next (conj stored [ts key value])]
              (if (period-lasts? next period)
                (let [agg (aggregator (mapv extract-value next))
                      first-ts (first (first next))]
                  (write-rollup-to-cassandra conn first-ts period key agg)
                  (emit-bolt! collector [first-ts key agg])
                  (.put nbhm key []))
                (.put nbhm key next)))))))))
