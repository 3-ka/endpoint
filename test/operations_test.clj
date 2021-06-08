(ns operations-test
  (:require [operations :as op])
  (:use [clojure.test]))

(deftest path-parse
  (is (= [:a] (op/parse-path "/a")))
  (is (= [:a] (op/parse-path "/a/")))

  (is (= [:a :b :c] (op/parse-path "/a/b/c/")))
  (is (= [:a :b :c] (op/parse-path "/a/b/c")))

  ;;
  )

(deftest upsert
  ;; {}
  (is (= {:a 5} (op/upsert {} "/a" 5)))
  (is (= {:a {:b {:c 5}}} (op/upsert {} "/a/b/c" 5)))
  
  ;; {:a 5}
  (is (= {:a 10} (op/upsert {:a 5} "/a" 10)))
  (is (= {:a {:b 5}} (op/upsert {:a 5} "/a/b" 5)))
  (is (= {:a {:b {:c 5}}} (op/upsert {:a 5} "/a/b/c" 5)))
  
  ;; {:a {:b {:c 10}}}
  (is (= {:a 5} (op/upsert {:a {:b {:c 10} }} "/a" 5)))
  (is (= {:a {:b 5}} (op/upsert {:a {:b {:c 10} }} "/a/b" 5)))
  (is (= {:a {:b {:c 5}}} (op/upsert {:a {:b {:c 10} }} "/a/b/c" 5)))
  
  ;; {:a {:b 10} {:c 20}} + /a/b 5 ->   {:a {:b 5} {:c 20}}
  (is (= {:a 5 :c 20} (op/upsert {:a {:b 10} :c 20} "/a" 5)))
  (is (= {:a {:b 5} :c 20} (op/upsert {:a {:b 10} :c 20} "/a/b" 5)))
  (is (= {:a {:b {:c 5}} :c 20} (op/upsert {:a {:b 10} :c 20} "/a/b/c" 5)))
  
  ;;
  )

(deftest delete
  ;; {} 
  (is (= {} (op/delete {} "/a")))
  (is (= {} (op/delete {} "/a/b/c")))
  
  ;; {:a 1}
  (is (= {} (op/delete {:a 1} "/a")))
  (is (= {:a 1} (op/delete {:a 1} "/b")))
  (is (= {:a 1} (op/delete {:a 1} "/a/b/c")))
  (is (= {:a 1} (op/delete {:a 1} "/b/c/d")))
  
  ;; {:a 1 :b 10}
  (is (= {:a 1} (op/delete {:a 1 :b 10} "/b")))
  (is (= {:a 1 :b 10} (op/delete {:a 1 :b 10} "/b/c")))
  (is (= {:a 1 :b 10} (op/delete {:a 1 :b 10} "/c")))
  
  ;; {:a {:b 10} :c 20}
  (is (= {:c 20} (op/delete {:a {:b 10} :c 20} "/a")))
  (is (= {:c 20} (op/delete {:a {:b 10} :c 20} "/a/b")))
  (is (=  {:a {:b 10} :c 20} (op/delete {:a {:b 10} :c 20} "/a/b/c")))
  (is (=  {:a {:b 10}} (op/delete {:a {:b 10} :c 20} "/c")))
  (is (=  {:a {:b 10} :c 20} (op/delete {:a {:b 10} :c 20} "/c/b")))
  
  ;; {:a {:b {:c 20}}}
  (is (= {} (op/delete {:a {:b {:c 20}}} "/a")))
  (is (= {} (op/delete {:a {:b {:c 20}}} "/a/b")))
  (is (= {} (op/delete {:a {:b {:c 20}}} "/a/b/c")))
  (is (= {:a {:b {:c 20}}} (op/delete {:a {:b {:c 20}}} "/a/b/c/d")))
  )


(defptest operation-validation
  ;;valid upsert
  (is (true? (op/valid? {:type "upsert" :path "/a/b" :value 23})))
  ;;valid remove
  (is (true? (op/valid? {:type "remove" :path "/a/b"})))
  
  ;;invalid type operation
  (is (false? (op/valid? {:type "update" :path "/aa" :value 34})))

  ;;valid path
  (is (true? (op/valid? {:type "upsert" :path "/a/" :value 34})))
  (is (true? (op/valid? {:type "upsert" :path "/aa/vv/c" :value 34})))
  (is (true? (op/valid? {:type "upsert" :path "/aa/vv/c/" :value 34})))
  
  ;;invalid path
  (is (false? (op/valid? {:type "upsert" :path "/aa////" :value 34})))
  (is (false? (op/valid? {:type "upsert" :path "/" :value 34})))
  (is (false? (op/valid? {:type "upsert" :path "////" :value 34})))
  (is (false? (op/valid? {:type "upsert" :path "/asdsad!!#/" :value 34})))
  
  ;;invalid value
  (is (false? (op/valid? {:type "upsert" :path "/asdsad!!#/" :value "asdasd"})))
  
  ;;
  )
