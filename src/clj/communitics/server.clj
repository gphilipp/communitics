(ns communitics.server
  (:require [compojure.core :refer [GET defroutes routes]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]
            [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [ring.middleware.cors :refer [wrap-cors]]
            [communitics.github :as github]
            [communitics.datomic-util :as dutil]
            [clojure.java.io :as io]
            [clojure.core.async :refer [go <! >!]]))

(def not-nil? (complement nil?))


(defrecord Database [uri connection]
  component/Lifecycle
  (start [this]
    (d/create-database uri)
    (let [conn   (d/connect uri)
          _      (println ";; Connected to database, installing schema..." uri)
          result (d/transact conn
                             (-> (dutil/read-all (io/resource "schema/schema.edn"))
                                 (first)
                                 (get-in [:github :schema])))
          _      (println "Schema installed successfully " result)]
      (assoc this :connection conn)))

  (stop [this]
    (println ";; Disconnect from database")
    (when connection
      (d/release connection))
    (assoc this :connection nil)))


(defrecord GithubCrawler [serveraddress database max-request-count]
  component/Lifecycle
  (start [component]
    (println ";; Starting github crawler")
    component)
  (stop [component]
    (println ";; Stopping github crawler")
    component))


(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})


(defn users [req]
  (generate-response (github/list-users (::database req))))

(defn clear-database [req]
  (generate-response (github/clear-database (::database req))))

(defn crawl [req]
  (go (github/import-github-tuples-into-db (::database req) (::github-crawler req) "users" :user/login))
  (generate-response "Started crawling..."))

(defn wrap-app-component [f database github-crawler]
  "Middleware that adds component awareness"
  (fn [req]
    (f (assoc req ::database database ::github-crawler github-crawler))))

(defn make-handler [database github-crawler]
  (-> (routes
        (resources "/")
        (GET "/users" [] users)
        (GET "/clear-database" [] clear-database)
        (GET "/crawl" [] crawl)
        (GET "/*" [] (resources "index.html")))
      (wrap-app-component database github-crawler)
      (wrap-cors :access-control-allow-origin #"http://localhost:3449"
                 :access-control-allow-methods [:get :put :post :delete])
      api))


(defrecord WebApp [jetty database github-crawler port]
  component/Lifecycle
  (start [component]
    (do
      (let [port (Integer. (or port 10555))]
        (println ";; Starting web server on port" port)
        (assoc component
          :jetty (run-jetty (make-handler database github-crawler) {:port port :join? false})))))
  (stop [component]
    (when-let [jetty (:jetty component)]
      (.stop jetty))))


(defn make-database [config-options]
  (map->Database {:uri (:datomic-uri config-options)}))


(defn make-github-crawler [config-options]
  (component/using
    (map->GithubCrawler {:serveraddress (:github-address config-options)
                         :max-request-count (:github-max-requests config-options)})
    [:database]))


(defn prod-system [config-options]
  (component/system-map
    :database (make-database config-options)
    :github-crawler (make-github-crawler config-options)
    :web-app (component/using (map->WebApp {:port 10555}) [:database :github-crawler])))

(defn -main [& [port]]
  (component/start
    (prod-system
      {:github-address "http://github.com/api/v3"
       :datomic-uri "datomic:dev://localhost:4334/communitics"})))
