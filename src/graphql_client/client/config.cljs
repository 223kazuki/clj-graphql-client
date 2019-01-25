(ns graphql-client.client.config
  (:require [integrant.core :as ig]
            [graphql-client.client.module.app]
            [graphql-client.client.module.router]
            [graphql-client.client.module.graphql]))

(defn system-conf []
  {:graphql-client.client.module/router
   {:routes ["/" {""      :home
                  ["rikishis/" :rikishi-id] :rikishi
                  "torikumi" :torikumi}]}

   :graphql-client.client.module/graphql
   {:ws-url                  "wss://foo.io/graphql-ws"
    :http-url                "http://bar.io/graphql"
    :ws-reconnect-timeout    2000
    :resume-subscriptions?   true
    :connection-init-payload {}}

   :graphql-client.client.module/app
   {:mount-point-id "app"
    :graphql (ig/ref :graphql-client.client.module/graphql)
    :router (ig/ref :graphql-client.client.module/router)}})
