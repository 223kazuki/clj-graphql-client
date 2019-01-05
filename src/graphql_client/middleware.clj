(ns graphql-client.middleware
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [integrant.core :as ig]
            [ataraxy.response :as response]
            [ring.util.codec :refer [url-encode]]))

(defn- get-access-token [code {:keys [provider-url redirect-url client-id]}]
  (let [uri (format "%s/token?grant_type=%s&client_id=%s&code=%s&redirect_uri=%s"
                    provider-url
                    "authorization_code"
                    client-id
                    code
                    (url-encode redirect-url))
        res (client/post uri {:throw-exceptions false})]
    (when (== 200 (:status res))
      (-> res
          :body
          (json/read-str :key-fn keyword)
          (select-keys [:access_token :token_type :expires_in "refresh_token"])))))

(defn- check-access-token [access-token {:keys [:provider-url]}]
  (let [res (client/get (format "%s/introspect?token=%s&token_hint=%s"
                                provider-url access-token "hint")
                        {:headers          {"Content-type" "application/x-www-form-urlencoded"}
                         :throw-exceptions false})]
    (when (== 200 (:status res))
      (let [{:keys [active client_id username scope sub aud iss exp iat] :as res}
            (json/read-str (:body res) :key-fn keyword)]
        active))))

(defn wrap-authorization [handler options]
  (fn [request]
    (let [{uri :uri
           {:keys [:code]} :params
           access_token (get-in request [:cookies "token" :value])} request]
      (if (= uri "/logout")
        (-> request
            handler
            (assoc :cookies {"token" {:value "EXPIRED"}}))
        (if-not (nil? code)
          (if-let [response (get-access-token code options)]
            (-> request
                handler
                (assoc :cookies {"token" {:value (:access_token response)}}))
            {:status 401 :body "Invalid authorization code."})
          (handler request))))))

(defmethod ig/init-key ::authorization [k {:keys [:provider-url :redirect-url :client-id]
                                           :as options}]
  (when-not (and provider-url redirect-url client-id)
    (throw (Exception. (format "Initialize error in %s." k))))
  #(wrap-authorization % options))
