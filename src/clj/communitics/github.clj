(ns communitics.github
  (:require [datomic.api :as d]
            [communitics.datomic-util :as dutil]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]))


(def serveradress "http://github.com/api")
(def uri "datomic:dev://localhost:4334/communitics")

(defn init-schema [database]
  (d/transact (:connection database)
              (-> (dutil/read-all "schema/schema.edn")
                  (ffirst)
                  (get-in [:github-enterprise :txes]))))


(defn find-user [database]
  (let [dbval (:connection database)]
    (d/q '[:find ?login
           :where [_ :user/url ?login]] dbval)))

(defn map-vals
  "Given a map and a function, returns the map resulting from applying
  the function to each value."
  [m f]
  (zipmap (keys m) (map f (vals m))))


(defn fetch-github-data!
  ([serveradress resource-name]
    "Fetches data from a multi-page github resource like
     users or repositories"
    (loop [url (str serveradress "/" resource-name)
           result []]
      (let [response (client/get url {:as :json})
            next-url (get-in response [:links :next :href])
            data (:body response)
            result (if (map? data)
                     (conj result data)
                     (into result data))]
        (if next-url
          (recur next-url result)
          result))))
  ([resource-name]
    (fetch-github-data! serveradress resource-name)))


(defn github-keyword->datomic-attr [keyword]
  (keyword "user" (name keyword)))

(github-keyword->datomic-attr :html_url)

(def user-attributes {:html_url :user/html_url
                      :login :user/login
                      :repos_url :user/repos_url})

(defn create-users-import-tx [users]
  (map (fn [user] [:db/add (d/tempid :db.part/user) :user/url user])
       (map :html_url filter (#(= "User" (:type %)) users))
       ))

(defn create-users-import-tx [users]
  (map (fn [user] [:db/add (d/tempid :db.part/user) :user/url user])
       (map :html_url (filter #(= "User" (:type %)) users))
       ))



(create-users-import-tx users)



(def all-users (fetch-github-data! "users"))
(def users (take 2 all-users))

(defn import-github-data-into-db [database]
  (d/transact (:connection database)
              (create-users-import-tx (fetch-github-data! "users"))))

(defn group-by-and-count [attr coll]
  (map-vals (group-by attr coll) count))

(fetch-github-data! "users/bob")

(def fetch-user-stats #(group-by-and-count :type (fetch-github-data! "users")))
(def fetch-repo-stats #(group-by-and-count (comp :type :owner) (fetch-github-data! "repositories")))

(fetch-user-stats)


(def fetch-user-stats-threaded
  #(->> "/users"
        (str serveradress)
        (fetch-github-data!)
        (group-by-and-count :type)))

(def fetch-repo-stats
  #(group-by-and-count (comp :type :owner) (fetch-github-data! (str serveradress "/repositories"))))


(defn count-repo [username]
  (-> (client/get (str serveradress "/users/" username "/repos") {:as :json})
      :body
      count))




