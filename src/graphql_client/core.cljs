(ns graphql-client.core
  (:require [integrant.core :as ig]
            [graphql-client.client.config :as config]))

(defonce system (atom nil))

(def config (atom (config/system-conf)))

(defn start []
  (reset! system (ig/init @config)))

(defn stop []
  (when @system
    (ig/halt! @system)
    (reset! system nil)))

(defn ^:export init []
  (start))
