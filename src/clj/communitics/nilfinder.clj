(ns communitics.nilfinder
  (:import (java.util List Map Set)))


(def app
  "Intenal Helper"
  (fnil conj []))

(defprotocol PathSeq
  (path-seq* [form path] "Helper for path-seq"))

(extend-protocol PathSeq
  List
  (path-seq*
    [form path]
    (->> (map-indexed
           (fn [idx item]
             (path-seq* item (app path idx)))
           form)
         (mapcat identity)))

  Map
  (path-seq*
    [form path]
    (->> (map
           (fn [[k v]]
             (path-seq* v (app path k)))
           form)
         (mapcat identity)))

  Set
  (path-seq*
    [form path]
    (->> (map
           (fn [v]
             (path-seq* v (app path v)))
           form)
         (mapcat identity)))
  Object
  (path-seq* [form path] [[form path]])
  nil
  (path-seq* [_ path] [[nil path]]))

(defn path-seq
  "Returns a sequence of paths into a form, and the elements found at
  those paths. Each item in the sequence is a map with :path
  and :form keys. Paths are built based on collection type: lists
  by position, maps by key, and sets by value, e.g.

  (path-seq [:a [:b :c] {:d :e} #{:f}])

  ({:path [0], :form :a}
  {:path [1 0], :form :b}
  {:path [1 1], :form :c}
  {:path [2 :d], :form :e}
  {:path [3 :f], :form :f})
  "
  [form]
  (map
    #(let [[form path] %]
      {:path path :form form})
    (path-seq* form nil)))

(comment
  (path-seq [:a [:b :c] {:d :e} #{:f}])
  ;; finding nils hiding in data structures:
  (->> (path-seq [:a [:b nil] {:d :e} #{:f}])
       (filter (comp nil? :form)))
  ;; finding a nil hiding in a Datomic transaction
  (->> (path-seq {:db/id 100
                  :friends [{:firstName "John"}
                            {:firstName nil}]})
       (filter (comp nil? :form)))
    )
