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
                          :users ["no result"]
                          :status ""}))

(def serveraddress "http://localhost:10555")

(defn call-server [cb command]
  (edn-xhr
    {:method :get
     :url (str serveraddress command)
     :data {}
     :on-complete cb}))


(defn user-view [user]
  (om/component
    (dom/li nil
      (dom/div nil
        (dom/a #js {:href (:user/url user)} (:user/login user))
        (dom/label nil (str "," (:user/repos_url user)))))))


(defn status-view [status]
  (om/component
    (dom/p #js {:className "status"} status)))

(defn update-status [app text]
  (om/transact! app :status (fn [_] text)))

(defn users-view [users]
  (om/component
    (dom/div nil
      (dom/section nil "Github")
      (dom/h3 nil (count users) " users")
      (apply dom/ul nil
             (om/build-all user-view users)))))

(om/root
  (fn [app owner]
    (om/component
      (dom/div #js {:className "l-box"}
        (dom/header nil
                    (dom/h1 nil (:title app))
                    (dom/p nil "Analytics for your communities!"))
        (dom/button
          #js {:className "pure-button pure-button-primary my-button"
               :onClick
               (fn [_]
                 (call-server

                   (fn [res]
                     (update-status app "Searching users")
                     (om/transact! app :users (fn [_] res))
                     (update-status app (str "Found " (count (:users @app)) " users")))
                   "/users"))}
          (dom/i #js {:className "fa fa-list-ul"}) " List users")
        (dom/button
          #js {:className "pure-button pure-button-primary my-button"
               :onClick
               (fn [_]
                 (call-server
                   (fn [res]
                     (update-status app "Start crawling github for users")
                     (println "Start crawling github for users")
                     (om/transact! app :crawl-result (fn [_] res))
                     (let [craw-result (:crawl-result @app)]
                       (update-status app (str "Crawled GitHub"
                                               (if-let [message (:message craw-result)]
                                                 message (:import-count craw-result))))))
                   "/crawl"))}
          (dom/i #js {:className "fa fa-camera-retro"}) " Crawl GitHub")
        (dom/button
          #js {:className "pure-button pure-button-primary my-button"
               :onClick
               (fn [_]
                 (call-server
                   (fn [res]
                     (println "Triggered database clearing")
                     (om/transact! app :clear-database-result (fn [_] res))
                     (println "Clear database: " (:clear-database-result @app) " datom deleted"))
                   "/clear-database"))}
          (dom/i #js {:className "fa fa-trash-o"}) " Clear database")
        (dom/div nil
          (om/build status-view (:status app)))
        (om/build users-view (:users app)))))
  app-state
  {:target (. js/document (getElementById "app"))})


(defn foo [v]
  (swap! app-state assoc :title v))

