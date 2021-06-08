(ns operations
  (:require [clojure.string :refer [split]]
            [clojure.spec.alpha :as s]))

(s/def ::type #{"upsert" "remove"})
(s/def ::value int?)
(def path-regex #"(/[a-zA-Z-_0-9]+)+/?")
(s/def ::path (s/and string? #(re-matches path-regex %)))

(defmulti operation-type :type)
(defmethod operation-type "upsert"
  [_]
  (s/keys :req-un [::type ::path ::value]))
(defmethod operation-type "remove"
  [_]
  (s/keys :req-un [::type ::path]))

(s/def ::operation (s/multi-spec operation-type :type))

(defn valid? [op]
  (s/valid? ::operation op))

(defn parse-path [path]
  (->> (split path #"/")
       (filter not-empty)
       (map keyword )))

(defn- dissoc-in
  ;; https://github.com/clojure/core.incubator/blob/master/src/main/clojure/clojure/core/incubator.clj
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks]]
  (if ks
    (if-let [val (get m k)]
      (if (map? val)
        (let [newmap (dissoc-in val ks)]
          (if (seq newmap)
            (assoc m k newmap)
            (dissoc m k)))
        m)
      m)
    (dissoc m k)))

(defn- find-clean-ks
  [db ks]
  (loop [db db [k & ks] ks clean-ks []]
    (if-let [v (k db)]
      (let [clean-ks (conj clean-ks k)]
        (if (map? v)
          (if ks
            (recur v ks clean-ks)
            nil)
          clean-ks))
      nil)))

(defn upsert [db path value]
  (let [path-ks (parse-path path)]
    (if-let [clean-ks (find-clean-ks db path-ks)]
      (let [clean-db (dissoc-in db clean-ks)]
        (assoc-in clean-db path-ks value))
      (assoc-in db path-ks value))))


(defn delete [db path]
  (let [path-ks (parse-path path)]
    (dissoc-in db path-ks)))

(defmulti execute :type)

(defmethod execute "upsert"
  [{:keys [db path value]}]
  (upsert db path value))

(defmethod execute "remove"
  [{:keys [db path]}]
  (delete db path))


(comment

  (require '[clojure.pprint :as pp])
  
  (find-clean-ks {:a 10} [:a :b])



  )
