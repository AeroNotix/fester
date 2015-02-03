(ns fester.topology
  "https://github.com/nathanmarz/storm/wiki/Clojure-DSL"
  (:require [backtype.storm.clojure :refer [topology spout-spec bolt-spec]]
            [backtype.storm.config :refer :all]
            [fester.bolts :refer [fester-raw-metric-bolt]]
            [fester.spouts :refer [fester-spout]])
  (:import [backtype.storm LocalCluster LocalDRPC]))


(defn storm-topology []
  (topology
    {"fester-spout"
     (spout-spec fester-spout)}
    {"fester-raw-metric-bolt"
     (bolt-spec
       {"fester-spout" ["key" "name"]}
       fester-raw-metric-bolt :p 2)}))

(defn run! [& {debug "debug" workers "workers" :or {debug "true" workers "2"}}]
  (doto (LocalCluster.)
    (.submitTopology "topology-1"
      {TOPOLOGY-DEBUG (Boolean/parseBoolean debug)
       TOPOLOGY-WORKERS (Integer/parseInt workers)}
      (storm-topology))))4