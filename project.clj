(defproject big-prize "0.1.0-SNAPSHOT"
  :description "A game for a few groups."
  :url "https://github.com/forgetaboutit/big-prize"

  :jvm-opts ^:replace ["-Xmx512m" "-server"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [secretary "0.4.0"]
                 [om "0.3.0"]
                 [cljs-ajax "0.2.3"]
                 [compojure "1.1.6"]
                 [com.cemerick/piggieback "0.1.2"]]

  :plugins [[lein-cljsbuild "1.0.1"]
            [lein-ring "0.8.8"]]

  :ring {:handler prize.core/handler}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :injections [(require
                 '[cljs.repl.browser :as brepl]
                 '[cemerick.piggieback :as pb])
               (defn browser-repl []
                 (pb/cljs-repl :repl-env (brepl/repl-env :port 9000)))]

  :source-paths ["src/clj" "src/cljs"]

  :cljsbuild {
    :builds [{
              :source-paths ["src/cljs"]
              :compiler {
                         :output-to "resources/public/js/big-prize.js"
                         :output-dir "resources/public/js"
                         :optimizations :none
                         :pretty-print true
                         :source-map true}}]})
