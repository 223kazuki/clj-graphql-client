(ns graphql-client.handler.index
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response]
            [clojure.java.io :as io]
            [integrant.core :as ig]))

(defmethod ig/init-key :graphql-client.handler/index [_ options]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (io/resource "graphql_client/handler/index.html")]))
