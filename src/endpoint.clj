(ns endpoint
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.content-negotiation :as conneg]
            [clojure.data.json :as json]
            [operations :as op]))

(defonce db-atom (atom {}))

(defn respond-db-read [request]
  {:status 200
   :body @db-atom})

(defn- execute! [op]
  (loop [old-val @db-atom]
    (let [op-with-db (assoc op :db old-val)
          new-val (op/execute op-with-db)
          success? (compare-and-set! db-atom old-val new-val)]
      (if success?
        new-val
        (recur @db-atom)))))

(defn execute-handler [{op :json-params}]
  (if (op/valid? op)
    {:status 200 :body (execute! op)}
    {:status 422 :body "Received data was incorrect!"}))

(def coerce-body-to-json
  {:name ::coerce-body-to-json
   :leave
   (fn [context]
     (let [response    (get context :response)
           body             (get response :body)
           updated-response (assoc response
                                   :headers {"Content-Type"  "application/json" }
                                   :body    (json/write-str body))]
       (assoc context :response updated-response)))})

(def routes
  (route/expand-routes
   #{["/api" :get [coerce-body-to-json respond-db-read] :route-name :get-db]
     ["/api" :post [(body-params/body-params) http/json-body execute-handler] :route-name :operate]}))


(def service-map
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   8890})

(defn start []
  (http/start (http/create-server service-map)))

;; For interactive development
(defonce server (atom nil))                                                             

(defn start-dev []
  (reset! server                                                                        
          (http/start (http/create-server
                       (assoc service-map
                              ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []                                                                        
  (stop-dev)
  (start-dev))

(defn -main
  [& args]
  (start))

(comment

  (start-dev)

  (restart)

  @db-atom

  (require '[io.pedestal.test :as test]
           '[clojure.pprint :as pp]
           '[clojure.repl :as repl])

  (repl/doc body-params/body-params)
  
  (:body (test/response-for (:io.pedestal.http/service-fn @server) :get "/api"))

  (:body (test/response-for (:io.pedestal.http/service-fn @server) :post "/api" :body "{\"path\": \"/a/b\", \"value\":42, \"type\":\"upsert\"} " :headers {"Content-Type" "application/json"}))

  (:body (test/response-for (:io.pedestal.http/service-fn @server) :post "/api" :body "{\"path\": \"/a/b\", \"type\":\"remove\"} " :headers {"Content-Type" "application/json"}))

  (pp/pprint (body-params/body-params
              (body-params/default-parser-map :json-options {:key-fn keyword})))
  

  (op/execute {:type "upsert"})
  (op/execute {:type "remove"})

  

  )
