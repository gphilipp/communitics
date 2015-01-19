(ns communitics.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs.reader :as reader])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(enable-console-print!)

(defonce app-state (atom {:title "Communitics"
                          :sum   "no result"}))

(def ^:private meths
  {:get    "GET"
   :put    "PUT"
   :post   "POST"
   :delete "DELETE"})

(defn edn-xhr [{:keys [method url data on-complete]}]
  (let [xhr (XhrIo.)]
    (events/listen xhr goog.net.EventType.COMPLETE
                   (fn [e]
                     (on-complete (reader/read-string (.getResponseText xhr)))))
    (. xhr
       (send url (meths method) (when data (pr-str data))
             #js {"Content-Type" "application/edn"}))))

(defn get-sum [cb]
  (edn-xhr
    {:method      :get
     :url         "/sum"
     :data        {}
     :on-complete cb}))

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/div #js {}
                   (dom/h1 nil (:title app))
                   (dom/button
                     #js {:onClick (fn [_]
                                     (get-sum (fn [res]
                                                (om/transact! app :sum (fn [_] res)))))}
                     "Click me to sum"
                     )
                   (dom/textarea #js {:value (:sum app)})
                   ))))
    app-state
    {:target (. js/document (getElementById "app"))}))


(defn foo [v]
  (swap! app-state assoc :text v))

