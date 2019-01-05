(ns graphql-client.handler.auth
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response]
            [integrant.core :as ig]
            [ring.util.codec :refer [url-encode]]
            [taoensso.timbre :refer [info debug]]))

(defn- construct-redirect-url [options]
  (let [{:keys [provider-url redirect-url client-id response-type]
         :or {response-type "code"}} options
        state "xyz"]
    (format "%s/login?response_type=%s&client_id=%s&state=%s&redirect_uri=%s"
            provider-url response-type client-id state (url-encode redirect-url))))

(defmethod ig/init-key ::login [k {:keys [:provider-url :redirect-url :client-id] :as options}]
  (when-not (and provider-url redirect-url client-id)
    (throw (Exception. (format "Initialize error in %s." k))))
  (fn [{[_ request] :ataraxy/result}]
    [::response/found (construct-redirect-url options)]))

(defmethod ig/init-key ::logout [_ options]
  (fn [{[_] :ataraxy/result}]
    [::response/found "/login"]))

(defmethod ig/init-key ::cb [_ options]
  (fn [{[_] :ataraxy/result}]
    [::response/found "/"]))
