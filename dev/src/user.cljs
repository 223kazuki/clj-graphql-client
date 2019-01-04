(ns cljs.user
  (:require [graphql-client.core :refer [system config start stop]]
            [meta-merge.core :refer [meta-merge]]))

(enable-console-print!)
(println "dev mode")

(defn dev-conf []
  {})

(swap! config #(meta-merge % (dev-conf)))

(defn reset []
  (println "Reset.")
  (stop)
  (start))
