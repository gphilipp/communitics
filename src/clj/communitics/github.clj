(ns communitics.github
  (:require [datomic.api :as d]
            [communitics.datomic-util :as dutil]
            [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [communitics.nilfinder :as nilfinder]))


(defn http-get [url]
  (client/get url {:as :json :digest-auth ["login" "pwd"]}))


(defn list-users [database]
  (flatten (d/q '[:find (pull ?e [*])
                  :where
                  [?e :user/login]]
                (d/db (:connection database)))))


(defn find-entity-keys [database]
  (into {} (d/q '[:find ?login ?u
                  :where
                  [?u :user/login ?login]]
                (d/db (:connection database)))))


(defn map-vals
  "Given a map and a function, returns the map resulting from applying
  the function to each value."
  [m f]
  (zipmap (keys m) (map f (vals m))))


(defn fetch-github-data! [github-crawler resource-name]
  "Fetches data from a multi-page github resource like
   users or repositories"
  (loop [url           (str (:serveraddress github-crawler) "/" resource-name)
         result        []
         requests-left (or (:max-request-count github-crawler) Integer/MAX_VALUE)]
    (println "Fetching" url)
    (let [response (http-get url)
          next-url (get-in response [:links :next :href])
          data     (:body response)
          result   (if (map? data)
                     (conj result data)
                     (into result data))]
      (if (and next-url (pos? requests-left))
        (recur next-url result (dec requests-left))
        result))))


(defn prefix-keys [m prefix]
  (let [keys     (keys m)
        prefixed (map #(->> % name (str prefix) keyword) keys)]
    (zipmap keys prefixed)))


(defn github->datomic [github-data]
  (let [sample (first github-data)]
    (map #(set/rename-keys % (prefix-keys sample "user/"))
         github-data)))


(defn remove-entries-with-nil-value [m]
  (into {} (map
             (fn [[k v]]
               (when v [k v])) m)))


(defn create-txes [github-data database db-entity-key]
  (let [key->entity (find-entity-keys database)]
    (->> (github->datomic github-data)
         (map remove-entries-with-nil-value)
         (map #(assoc % :db/id
                        (or (key->entity (db-entity-key %)) (d/tempid :db.part/user)))))))


(defn import-github-tuples-into-db
  "Imports data from apis like \"users\" \"repos\"... "
  [database github-crawler api db-entity-key]
  (let [github-tuples (fetch-github-data! github-crawler api)
        _             (println "Found" (count github-tuples) api github-tuples)
        txes          (create-txes github-tuples database db-entity-key)]
    (let [nilforms (->> (nilfinder/path-seq txes) (filter (comp nil? :form)))]
      (if (empty? nilforms)
        (do (println "Importing github data into datomic ")
            @(d/transact (:connection database) txes)
            {:import-count (count txes)})
        (let [message (str "Found nils in these transactions " (vec txes) ": " (vec nilforms))]
          (do (println message)
              {:message message}))))))


(defn clear-database [database]
  (let [conn     (:connection database)
        entities (d/q '[:find [?e ...]
                        :where [?e :user/login]]
                      (d/db conn))
        result   (d/transact conn (map (fn [e] {:db/id (d/tempid :db.part/user) :db/excise e})
                                  entities))]
    (println "Cleared database")
    (dec (count (:tx-data @result)))))