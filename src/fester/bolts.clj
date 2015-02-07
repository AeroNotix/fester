(ns fester.bolts
  (:require [backtype.storm
             [clojure :refer [emit-bolt! defbolt ack! bolt]]]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql])
  (:import [org.cliffc.high_scale_lib NonBlockingHashMap]))


(defn write-to-cassandra [conn table ts key name value]
  (cql/insert conn table {:time ts :key key :name name :value value}))

(defn store-average-in-cass [conn table values]
  (let [total (reduce #(+ %1 (%2 3)) 0 values)
        avg   (/ total (float (count values)))
        [ts key name] (first values)]
    (write-to-cassandra conn table ts key name avg)))

(defn period-lasts? [[ts1 & _] [ts2 & _] duration]
  (> (- ts2 ts1) duration))

(defbolt fester-raw-metric-bolt ["ts" "key" "name" "value"] {:prepare true}
  [conf _ collector]
  (let [conn (cc/connect ["127.0.0.1"] {:keyspace "fester"})]
    (bolt
      (execute [{:strs [ts key name value] :as tuple}]
        (emit-bolt! collector [ts key name value])
        (write-to-cassandra conn "raw" ts key name value)
        (ack! collector tuple)))))

(defbolt fester-rollup-metric-bolt ["ts" "dt" "key" "name" "value"]
  {:prepare true
   :params [period table]}
  [conf _ collector]
  (let [nbhm (NonBlockingHashMap.)
        conn (cc/connect ["127.0.0.1"] {:keyspace "fester"})]
    (bolt
      (execute [{:strs [ts key name value] :as tuple}]
        (ack! collector tuple)
        (let [stored (.get nbhm [key name])]
          (if (not stored)
            (.put nbhm [key name] [[ts key name value]])
            (let [next (conj stored [ts key name value])]
              (if (period-lasts? (first next) (last next) 10)
                (do
                  (store-average-in-cass conn table next)
                  (.put nbhm [key name] []))
                (.put nbhm [key name] (conj stored [ts key name value])))))
          (emit-bolt! collector [ts period key name value]))))))
