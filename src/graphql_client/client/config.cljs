(ns graphql-client.client.config
  (:require [integrant.core :as ig]
            [graphql-client.client.module.app]
            [graphql-client.client.module.router]
            [graphql-client.client.module.re-graph]))

(defn system-conf []
  {:graphql-client.client.module/router
   {:routes ["/" {""      :home
                  "about" :about}]}

   :graphql-client.client.module/re-graph
   {:ws-url                  "wss://foo.io/graphql-ws"
    :http-url                "http://bar.io/graphql"
    :ws-reconnect-timeout    2000
    :resume-subscriptions?   true
    :connection-init-payload {}}

   :graphql-client.client.module/app
   {:mount-point-id "app"
    :re-graph (ig/ref :graphql-client.client.module/re-graph)
    :router (ig/ref :graphql-client.client.module/router)}})
