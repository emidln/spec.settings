(defproject spec.settings "0.1.0-SNAPSHOT"
  :description "Settings validated clojure.spec"
  :url "https://github.com/emidln/spec.settings"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/spec.alpha "0.2.168"]
                 [metosin/spec-tools "0.7.1"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.10.0-alpha3"]
                                  [orchestra "2017.11.12-1"]]}})
