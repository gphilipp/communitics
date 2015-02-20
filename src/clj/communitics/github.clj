(ns communitics.github
  (:require [datomic.api :as d]
            [communitics.datomic-util :as dutil]
            [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.set :as set]))


;(defn http-get [url] (client/get url ({:as :json})))
(defn http-get [url]
  (client/get url {:as :json :digest-auth ["login" "pwd"]}))


(defn list-users [database]
  (flatten (d/q '[:find (pull ?e [*])
                  :where
                  [?e :user/login]]
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
        prefixed (map #(->> % name (str "user/") keyword) keys)]
    (zipmap keys prefixed)))


(defn github->datomic [github-data]
  (map #(set/rename-keys % (-> github-data (first) (prefix-keys "users/")))
       github-data))


(defn create-txes [github-data]
  (map #(assoc % :db/id (d/tempid :db.part/user))
       (github->datomic github-data)))


(defn import-data-into-db [database github-crawler]
  (let [users (fetch-github-data! github-crawler "users")
        txes  (create-txes users)
        _     (println "Importing github data into datomic ")
        result @(d/transact (:connection database) txes)]
    (println result)
    {:import-count (count txes)}))


(defn group-by-and-count [attr coll]
  (map-vals (group-by attr coll) count))


(defn count-repo [github-crawler username]
  (-> (client/get (str (:serveraddress github-crawler) "/users/" username "/repos") {:as :json})
      :body
      count))

(comment

  (def fetch (partial fetch-github-data! (:github-crawler user/system)))
  (def all-users (fetch "users"
                        {:digest-auth ["gilles.philippart" "Virgo1110"]}))
  (def all-users (fetch))
  (def users (take 2 all-users))

  (fetch "users/gphilip")

  (def fetch-user-stats #(group-by-and-count :type (fetch "users")))
  (def fetch-repo-stats #(group-by-and-count (comp :type :owner) (fetch "repositories")))

  (fetch-user-stats)


  (def fetch-user-stats-threaded
    #(->> "/users"
          (str serveradress)
          (fetch)
          (group-by-and-count :type)))

  (def fetch-repo-stats
    #(group-by-and-count (comp :type :owner) (fetch (str serveradress "/repositories")))))




