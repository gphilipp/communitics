(ns communitics.datomic-util
  (:import (datomic Util))
  (:require [clojure.java.io :as io]
            [datomic.api :as d]))


(defn read-all
  "Read all forms in f, where f is any resource that can
  be opened by io/reader"
  [f]
  (Util/readAll (io/reader f)))

(defn transact-all
  "Load and run all transactions from f, where f is any
  resource that can be opened by io/reader."
  [conn f]
  (doseq [txd (read-all f)]
    (d/transact conn txd))
  :done)

(defn has-attribute?
  "Does database have an attribute named attr-name?"
  [db attr-name]
  (-> (d/entity db attr-name)
      :db.install/_attribute
      boolean))

(defn has-schema?
  "Does database have a schema named schema-name installed?
  Uses schema-attr (an attribute of transactions!) to track
  which schema names are installed."
  [db schema-attr schema-name]
  (and (has-attribute? db schema-attr)
       (-> (d/q '[:find ?e
                  :in $ ?sa ?sn
                  :where [?e ?sa ?sn]]
                db schema-attr schema-name)
           seq boolean)))


(defn- ensure-schema-attribute
  "Ensure that schema-attr, a keyword-valued attribute used
  as a value on transactions to track named schemas, is
  installed in database."
  [conn schema-attr]
  (when-not (has-attribute? (d/db conn) schema-attr)
    (d/transact conn [{:db/id                 #db/id[:db.part/db]
                       :db/ident              schema-attr
                       :db/valueType          :db.type/keyword
                       :db/cardinality        :db.cardinality/one
                       :db/doc                "Name of schema installed by this transaction"
                       :db/index              true
                       :db.install/_attribute :db.part/db}])))

(defn ensure-schemas
  "Ensure that schemas are installed.
  schema-attr a keyword valued attribute of a transaction,
  naming the schema
  schema-map a map from schema names to schema installation
  maps. A schema installation map contains two
  keys: :txes is the data to install, and :requires
  is a list of other schema names that must also
  be installed
  schema-names the names of schemas to install"
  [conn schema-attr schema-map & schema-names]
  (ensure-schema-attribute conn schema-attr)
  (doseq [schema-name schema-names]
    (when-not (has-schema? (d/db conn) schema-attr schema-name)
      (let [{:keys [requires txes]} (get schema-map schema-name)]
        (apply ensure-schemas conn schema-attr schema-map requires)
        (if txes
          (doseq [tx txes]
            ;; hrm, could mark the last tx specially
            (d/transact conn (cons {:db/id      #db/id [:db.part/tx]
                                    schema-attr schema-name}
                                   tx)))
          (throw (ex-info (str "No data provided for schema" schema-name)
                          {:schema/missing schema-name})))))))
