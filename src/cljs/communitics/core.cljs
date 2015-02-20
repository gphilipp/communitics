(ns communitics.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs.reader :as reader]
            [om-sync.util :refer [edn-xhr]])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

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
          (dom/div
            #js {:className "block"}
            (dom/h1 nil (:title app))
            (dom/button
              #js {:onClick
                   (fn [_]
                     (call-server
                       (fn [res]
                         (om/transact! app :users (fn [_] res))
                         (println "Found users:" (:users @app)))
                       "/users"))}
              "Get users from datomic")
            (dom/button
              #js {:onClick
                   (fn [_]
                     (call-server
                       (fn [res]
                         (om/transact! app :import-result (fn [_] res)))
                       "/crawl"))}
              "Crawl github")
            (dom/label nil (str "imported " (pr-str (:import-count (:import-result app)))))
            #_(dom/textarea #js {:value (pr-str (:users app))}))
          (dom/div nil
                   (dom/h1 nil "Users")
                   (apply dom/ul nil
                          (map #(dom/li nil (pr-str %)) (:users app)))))
        )))
  app-state
  {:target (. js/document (getElementById "app"))})


(defn foo [v]
  (swap! app-state assoc :title v))

