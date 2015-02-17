(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [communitics.server :as app]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly
                    (app/prod-system {:github-address "http://github.com/api/v3"
                                      :datomic-uri "datomic:dev://localhost:4334/mbrainz-1968-1973"}))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
