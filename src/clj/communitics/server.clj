(ns communitics.server
  (:require [clojure.java.io :as io]
            [communitics.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET defroutes routes]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(deftemplate page
             (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(def not-nil? (complement nil?))

(defn connect-to-database [uri]
  {:pre [(not-nil? uri)]}
  (d/create-database uri)
  (d/connect uri))

(defrecord Database [uri connection]
  component/Lifecycle
  (start [this]
    (println ";; Starting database")
    (let [conn (connect-to-database uri)]
      (assoc this :connection conn)
      ;(println "connected to " conn)
      ))

  (stop [this]
    (println ";; Stopping database")
    ;; In the 'stop' method, shut down the running
    ;; component and release any external resources it has
    ;; acquired.
    (when connection
      (d/release connection))
    ;; Return the component, optionally modified. Remember that if you
    ;; dissoc one of a record's base fields, you get a plain map.
    (assoc this :connection nil)))


(defrecord GithubCrawler [serveraddress database]
  component/Lifecycle
  (start [component]
    (println ";; Starting github crawler"))
  (stop [component]
    (println ";; Stopping github crawler"))
  )


(defn find-countries [database]
  (d/q '[:find ?name
         :where [?e :db/ident ?name]]
       (d/db (:connection database))))


(defn sum [req]
  {:status 200
   :body (pr-str (find-countries (::database req)))
   :headers {"Content-Type" "application/edn"}})

(defn wrap-app-component [f database]
  "Middleware that adds component awareness"
  (fn [req]
    (f (assoc req ::database database))))


(defn make-handler [database]
  (-> (routes
        (resources "/")
        (resources "/react" {:root "react"})
        (GET "/sum" [req] sum)
        (GET "/*" [req] (page)))
      (wrap-app-component database)
      api))


(defrecord WebApp [jetty database port]
  component/Lifecycle
  (start [component]
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (println "Starting web server on port" port ".\n")
        (println "database:" database)
        (assoc component
          :jetty (run-jetty (make-handler database) {:port port :join? false})))))
  (stop [component]
    (.stop (:jetty component))))


(defn make-database [uri]
  (map->Database {:uri uri}))


(defn make-github-crawler [config-options]
  (component/using
    (map->GithubCrawler {:serveraddress (:github-address config-options)})
    [:database]))


(defn prod-system [config-options]
  (component/system-map
    :database (make-database (:datomic-uri config-options))
    :github-crawler (component/using
                      (map->GithubCrawler {:serveraddress (:github-address config-options)})
                      [:database])
    :web-app (component/using
               (map->WebApp {:port 10555}) [:database])))


(defn -main [& [port]]
  (component/start
    (prod-system
      {:github-address "http://github.com/api/v3"
       :datomic-uri "datomic:dev://localhost:4334/mbrainz-1968-1973"})))
