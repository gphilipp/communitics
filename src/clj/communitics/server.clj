(ns communitics.server
  (:require [compojure.core :refer [GET defroutes routes]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]
            [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [ring.middleware.cors :refer [wrap-cors]]
            [communitics.github :as github]))

(def not-nil? (complement nil?))

(defn connect-to-database [uri]
  {:pre [(not-nil? uri)]}
  (d/create-database uri)
  (d/connect uri))

(defrecord Database [uri connection]
  component/Lifecycle
  (start [this]
    (let [conn (connect-to-database uri)]
      (when conn
        (println ";; Connected to database " uri)
        (assoc this :connection conn))
      ))

  (stop [this]
    (println ";; Disconnect from database")
    (when connection
      (d/release connection))
    (assoc this :connection nil)))


(defrecord GithubCrawler [serveraddress database]
  component/Lifecycle
  (start [component]
    (println ";; Starting github crawler")
    component)
  (stop [component]
    (println ";; Stopping github crawler")
    component))


(defn find-users [database]
  (d/q '[:find ?name
         :where [_ :user/url ?name]]
       (d/db (:connection database))))

(defn users [req]
  {:status 200
   :body (pr-str (find-users (::database req)))
   :headers {"Content-Type" "application/edn"}})

(defn crawl [req]
  {:status 200
   :body (pr-str (github/import-data-into-db (::database req) (::github-crawler req)))
   :headers {"Content-Type" "application/edn"}})


(defn wrap-app-component [f database github-crawler]
  "Middleware that adds component awareness"
  (fn [req]
    (f (assoc req ::database database ::github-crawler github-crawler))))


(defn make-handler [database github-crawler]
  (-> (routes
        (resources "/")
        (GET "/users" [] users)
        (GET "/crawl" [] crawl)
        (GET "/*" [] (resources "index.html")))
      (wrap-app-component database github-crawler)
      (wrap-cors :access-control-allow-origin [#"http://localhost.*"]
                 :access-control-allow-methods [:get :put :post :delete])
      api))


(defrecord WebApp [jetty database github-crawler port ]
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


(defn make-database [uri]
  (map->Database {:uri uri}))


(defn make-github-crawler [config-options]
  (component/using
    (map->GithubCrawler {:serveraddress (:github-address config-options)})
    [:database]))


(defn prod-system [config-options]
  (component/system-map
    :database (make-database (:datomic-uri config-options))
    :github-crawler (make-github-crawler config-options)
    :web-app (component/using (map->WebApp {:port 10555}) [:database :github-crawler])))

(defn -main [& [port]]
  (component/start
    (prod-system
      {:github-address "http://github.com/api/v3"
       :datomic-uri "datomic:dev://localhost:4334/communitics"})))
