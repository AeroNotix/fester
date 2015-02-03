(defproject fester "0.0.1"
  :description "Uncle Fester's savage analytics dungeon"
  :url "http://example.com/FIXME"
  :license {:name "BSD"
            :url "http://www.linfo.org/bsdlicense.html"}
  :dependencies [[clj-kafka "0.2.8-0.8.1.1"]
                 [clojurewerkz/cassaforte "2.0.0"]
                 [storm "0.8.1"]
                 [org.clojure/clojure "1.4.0"]]
  :aot [fester.TopologySubmitter]
  :profiles {:dev {:dependencies [[storm "0.8.1"]]}}
  :plugins [[lein-ver "1.0.1"]])
