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

(defn period-lasts? [{:keys [min-ts max-ts] :as last} duration]
  (when (and min-ts max-ts)
    (>= (- max-ts min-ts) duration)))

(defbolt fester-raw-metric-bolt ["ts" "key" "value"] {:prepare true}
  [conf _ collector]
  (let [conn (cc/connect ["127.0.0.1"] {:keyspace "fester"})]
    (bolt
      (execute [{:strs [ts key value] :as tuple}]
        (emit-bolt! collector [ts key value])
        (write-to-cassandra conn "raw" ts key value)
        (ack! collector tuple)))))

(defn store-initial [hm [ts key value] & {:keys [max-written] :or
                                          {max-written 0}}]
  (.put hm key {:min-ts ts :max-ts ts :max-written max-written
                :stored [[ts key value]]}))

(defn update-bounds [{:keys [min-ts max-ts] :as last} next-ts]
  (cond
    (< next-ts min-ts) (assoc last :min-ts next-ts)
    (> next-ts max-ts) (assoc last :max-ts next-ts)
    :else last))

(defbolt fester-rollup-metric-bolt ["ts" "key" "value"]
  {:prepare true
   :params [period aggregator-type]}
  [conf _ collector]
  (let [nbhm (NonBlockingHashMap.)
        conn (cc/connect ["127.0.0.1"] {:keyspace "fester"})
        aggregator (aggregator-type aggregator-map)]
    (bolt
      (execute [{:strs [ts key value] :as tuple}]
        (let [{:keys [max-written stored] :as last} (.get nbhm key)]
          (if (not stored)
            (store-initial nbhm [ts key value])
            (when (> ts max-written)
              (let [next (-> last
                           (update-in [:stored] conj [ts key value])
                           (update-bounds ts))]
                (if (period-lasts? next period)
                  (let [agg (aggregator (mapv extract-value (:stored next)))
                        first-ts (:min-ts next)]
                    (write-rollup-to-cassandra conn first-ts period key agg)
                    (emit-bolt! collector [first-ts key agg] :anchor tuple)
                    (store-initial nbhm [ts key value]) :max-written first-ts)
                  (.put nbhm key next))))))
        (ack! collector tuple)))))
