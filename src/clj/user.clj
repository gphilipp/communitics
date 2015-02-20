(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [communitics.server :as app]))

(def system nil)


(def official-github "https://api.github.com")
(def enterprise-github "https://sgithub.fr.world.socgen/api/v3")

(defn init []
  (alter-var-root #'system
                  (constantly
                    (app/prod-system {:github-address enterprise-github
                                      :datomic-uri "datomic:dev://localhost:4334/communitics"}))))

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
