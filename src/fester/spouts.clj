(ns fester.spouts
  (:require [backtype.storm [clojure :refer [defspout spout emit-spout!]]]
            [clj-kafka.consumer.zk :as zfkc]
            [clj-kafka.core :as kafka]
            [clj-kafka.producer :as producer]
            [clj-kafka.zk :as zk]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent ArrayBlockingQueue]
           [java.nio ByteBuffer]
           [java.io ByteArrayInputStream]))


;; TODO: Put into parametrized spouts
(def topic "uncle_fester")
(def queue-size 1024)

;; TODO: Put into config files
(def consumer-config
  {"zookeeper.connect" "localhost:2181"
   "group.id" "clj-kafka.consumer"
   "auto.offset.reset" "smallest"
   "auto.commit.enable" "false"})

;; TODO: Put into config files
(def producer-config
 {"metadata.broker.list" "localhost:9092"
  "serializer.class" "kafka.serializer.DefaultEncoder"
  "partitioner.class" "kafka.producer.DefaultPartitioner"})

;; TODO: start dynamically
(def p
  (producer/producer producer-config))

(defn send-message [where what]
  (producer/send-message p
    (producer/message where what)))

(defn start-consumer-thread [topic queue-size]
  (let [running? (atom true)
        abq      (ArrayBlockingQueue. queue-size)]
    (future
      (let [c (zfkc/consumer consumer-config)]
        (try
          (let [stream-map (.createMessageStreams c {topic (int 1)})
                [stream & _] (get stream-map topic)
                msg-seq (iterator-seq (.iterator stream))]
            (doseq [msg msg-seq :while @running?]
              (.put abq (kafka/to-clojure msg))
              (.commitOffsets c)))
          (log/info "Consumer for" topic "stopping")
          (catch Exception e
            (log/error "Consumer for" topic "encountered" e))
          (finally
            (.shutdown c)))))
    {:queue abq :running? running?}))

(defn parse-message {:post [#(= (count %) 3)]}
  [entry]
  (when-let [buf (:value entry)]
    (let [[ts key name value :as all] (.split (String. buf) "\\s+")]
      (when (= (count all) 4)
        [(Long. ts) key name (Double. value)]))))

(defspout fester-spout ["ts" "key" "name" "value"]
  [conf context collector]
  (let [{:keys [queue running?]}
        ;; TODO: use parametrized spouts for topic/queue-size
        (start-consumer-thread topic queue-size)]
    (spout
      (nextTuple []
        (when-let [entry (parse-message (.poll queue))]
          (emit-spout! collector entry))))))

(defspout fake-data-spout ["ts" "key" "name" "value"]
  [conf context collector]
  (let [x (atom 0)]
    (spout
      (nextTuple []
        (let [ct (System/currentTimeMillis)]
          (send-message topic (.getBytes (str ct " FOO KEY " @x)))
          (swap! x inc)
          (Thread/sleep 1000))))))
