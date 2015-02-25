(defproject communitics "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2850" :scope "provided"]
                 [figwheel "0.2.5-SNAPSHOT"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.stuartsierra/component "0.2.2"]
                 [ring "1.3.1"]
                 [ring-cors "0.1.6"]
                 [compojure "1.2.0"]
                 [org.omcljs/om "0.8.8"]
                 [om-sync "0.1.1"]
                 [com.cemerick/piggieback "0.1.5"]
                 [weasel "0.6.0"]
                 [clj-http "1.0.1"]
                 [com.datomic/datomic-pro "0.9.5130" :exclusions [joda-time]]
                 [base64-clj "0.1.1"]]

  :jvm-opts ^:replace ["-Xmx512m" "-server"]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-figwheel "0.2.5-SNAPSHOT"]
            [lein-kibit "0.0.8"]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src/cljs" "dev_src"]
                        :compiler {:output-to "resources/public/js/compiled/communitics.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :optimizations :none
                                   :main communitics.dev
                                   :asset-path "js/compiled/out"
                                   :source-map true
                                   :source-map-timestamp true
                                   :cache-analysis true}}
                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/communitics.js"
                                   :main communitics.core
                                   :optimizations :advanced
                                   :pretty-print false}}]}

  :figwheel {
             :http-server-root "public"                     ;; default and assumes "resources"
             :server-port 3449                              ;; default
             :css-dirs ["resources/public/css"]             ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             }
  )
