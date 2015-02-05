(defproject fester "0.0.1"
  :description "Uncle Fester's savage analytics dungeon"
  :url "http://example.com/FIXME"
  :license {:name "BSD"
            :url "http://www.linfo.org/bsdlicense.html"}
  :dependencies [[clj-kafka "0.2.8-0.8.1.1"]
                 [com.boundary/high-scale-lib "1.0.6"]
                 [clojurewerkz/cassaforte "2.0.0"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [storm "0.9.0.1"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.slf4j/slf4j-log4j12 "1.6.6"]]
  :aot [fester.TopologySubmitter]
  :profiles {:dev {:dependencies [[storm "0.8.1"]]}}
  :plugins [[lein-ver "1.0.1"]
            [lein-protobuf "0.1.1"]])
