(ns graphql-client.client.config
  (:require [integrant.core :as ig]
            [graphql-client.client.module.app]
            [graphql-client.client.module.router]))

(defn system-conf []
  {:graphql-client.client.module/router
   {:routes ["/" {""      :home
                  "about" :about}]}
   :graphql-client.client.module/app
   {:mount-point-id "app"
    :router (ig/ref :graphql-client.client.module/router)}})
