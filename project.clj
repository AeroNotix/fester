(defproject fester "0.0.1"
  :description "Uncle Fester's savage analytics dungeon"
  :url "http://example.com/FIXME"
  :license {:name "BSD"
            :url "http://www.linfo.org/bsdlicense.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-kafka "0.2.8-0.8.1.1"]]
  :main ^:skip-aot fester.core
  :pom-location "target/"
  :profiles {:uberjar {:aot :all}}
  :target-path "target/%s"
  :uberjar-name "fester.jar"
  :plugins [[lein-ver "1.0.1"]
            [cider/cider-nrepl "0.8.0-SNAPSHOT"]])
