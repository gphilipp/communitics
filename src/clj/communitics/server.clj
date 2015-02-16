(ns communitics.server
  (:require [clojure.java.io :as io]
            [communitics.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET defroutes]]
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
      (assoc this :connection conn)))

  (stop [this]
    (println ";; Stopping database")
    ;; In the 'stop' method, shut down the running
    ;; component and release any external resources it has
    ;; acquired.
    (d/release connection)
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


(defrecord WebServer [port]
  component/Lifecycle
  (start [this]
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (print "Starting web server on port" port ".\n")
        (assoc this :http-server (run-jetty http-handler {:port port
                                                          :join? false}))))
    )
  (stop [this]
    (.stop (:http-server this)))
  )

(defn new-database [uri]
  (Database. uri nil))

(defn new-github-crawler [config-options]
  (let [gc (GithubCrawler. nil nil)]
    (assoc gc :serveraddress (:github-address config-options))))

(defn prod-system [config-options]
  (let [{:keys [datomic-url]} config-options]
    (component/system-map
      :database (new-database datomic-url)
      :app (component/using
             (new-github-crawler config-options)
             [:database]))))

(def system (prod-system {:github-address "http://github.com/api/v3"
                         :datomic-url "datomic:dev://localhost:4334/mbrainz-1968-1973"}))

(defn run [& [port]]
  (defonce ^:private server
           (do
             (if is-dev? (start-figwheel))
             (let [port (Integer. (or port (env :port) 10555))]
               (print "Starting web server on port" port ".\n")
               (run-jetty http-handler {:port port
                                        :join? false}))))
  server)

(defn sum [req]
  {:status 200
   :body (pr-str (find-countries (:database system)))
   :headers {"Content-Type" "application/edn"}})



(def http-handler [database]
  (if is-dev?
    (reload/wrap-reload (api #'routes))
    (api (routes
           (resources "/")
           (resources "/react" {:root "react"})
           (GET "/sum" req sum database)
           (GET "/*" req (page))))))




(defn -main [& [port]]
  (run port))
