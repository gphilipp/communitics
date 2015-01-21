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
            [datomic.api :as d]))

(deftemplate page
             (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))


(def uri "datomic:dev://localhost:4334/mbrainz-1968-1973")
(d/create-database uri)
(def conn (d/connect uri))

(defn find-countries []
  (d/q '[:find ?name
         :where [?e :db/ident ?name]]
       (d/db conn)))


(defn sum [req]
  {:status  200
   :body (pr-str (find-countries))
   :headers {"Content-Type" "application/edn"}})

(defroutes routes
           (resources "/")
           (resources "/react" {:root "react"})
           (GET "/sum" req sum)
           (GET "/*" req (page)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (api #'routes))
    (api routes)))

(defn run [& [port]]
  (defonce ^:private server
           (do
             (if is-dev? (start-figwheel))
             (let [port (Integer. (or port (env :port) 10555))]
               (print "Starting web server on port" port ".\n")
               (run-jetty http-handler {:port  port
                                        :join? false}))))
  server)

(defn -main [& [port]]
  (run port))
