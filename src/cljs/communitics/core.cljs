(ns communitics.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs.reader :as reader]
            [om-sync.util :refer [edn-xhr]]
            [weasel.repl :as repl])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(when-not (repl/alive?)
  (repl/connect "ws://localhost:9001"))


(enable-console-print!)

(defonce app-state (atom {:title "Communitics"
                          :users ["no result"]}))

(def serveraddress "http://localhost:10555")

(defn call-server [cb command]
  (edn-xhr
    {:method :get
     :url (str serveraddress command)
     :data {}
     :on-complete cb}))


(om/root
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (dom/div
          nil
          (dom/div nil
                   (dom/h1 nil (:title app))
                   (dom/button
                     #js {:className "pure-button pure-button-primary"
                          :onClick
                          (fn [_]
                            (call-server
                              (fn [res]
                                (om/transact! app :users (fn [_] res))
                                (println "Found users:" (count (:users @app))))
                              "/users"))}
                     "List users")
                   (dom/button
                     #js {:className "pure-button pure-button-primary"
                          :onClick
                          (fn [_]
                            (call-server
                              (fn [res]
                                (om/transact! app :crawl-result (fn [_] res))
                                (println "Craw github:" (:crawl-result @app)))
                              "/crawl"))}
                     "Crawl Github")
                   (dom/button
                     #js {:className "pure-button pure-button-primary"
                          :onClick
                          (fn [_]
                            (call-server
                              (fn [res]
                                (println "Triggered database clearing")
                                (om/transact! app :clear-database-result (fn [_] res))
                                (println "Clear database: " (:clear-database-result @app) " datom deleted"))
                              "/clear-database"))}
                     "Clear database")
                   (dom/label nil (str "Crawled and imported " (pr-str (:import-count (:crawl-result app))) " users."))
                   #_(dom/textarea #js {:value (pr-str (:users app))}))
          (dom/div nil
                   (dom/h1 nil "Github")
                   (dom/h3 nil (count (:users app)) " users")
                   (apply dom/ul nil
                          (map #(dom/li nil (pr-str %)) (:users app)))))
        )))
  app-state
  {:target (. js/document (getElementById "app"))})


(defn foo [v]
  (swap! app-state assoc :title v))

