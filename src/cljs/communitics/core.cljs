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
                          :sum   "no result"}))

(defn get-sum [cb]
  (edn-xhr
    {:method :get
     :url    "http://localhost:10555/sum"
     :data   {}
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
                     (get-sum (fn [res]
                                (om/transact! app :sum (fn [_] res))
                                (println "(:sum app)=" (:sum @app)))))}
              "See countries ftw")
            (dom/textarea #js {:value (pr-str (:sum app))}))
          (dom/div nil
                   (dom/h1 nil "Countries")
                   (apply dom/ul nil
                          (map #(dom/li nil (pr-str %)) (:sum app)))))
        )))
  app-state
  {:target (. js/document (getElementById "app"))})


(defn foo [v]
  (swap! app-state assoc :title v))

