(ns cljs.user
  (:require [graphql-client.core :refer [system config start stop]]
            [meta-merge.core :refer [meta-merge]]))

(enable-console-print!)
(println "dev mode")

(defn dev-conf []
  {:graphql-client.client.module/graphql
   {;;:ws-url   "ws://localhost:8080/graphql-ws"
    :ws-url nil
    :http-url "http://localhost:8080/graphql"}})

(swap! config #(meta-merge % (dev-conf)))

(defn reset []
  (println "Reset.")
  (stop)
  (start))
