(defproject big-prize "0.1.0-SNAPSHOT"
  :description "A game for a few groups."
  :url "https://github.com/forgetaboutit/big-prize"

  :jvm-opts ^:replace ["-Xmx512m" "-server"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [secretary "0.4.0"]
                 [om "0.3.0"]]

  :plugins [[lein-cljsbuild "1.0.1"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                         :output-to "big-prize.js"
                         :output-dir "out"
                         :optimizations :none
                         :source-map true}}]})
