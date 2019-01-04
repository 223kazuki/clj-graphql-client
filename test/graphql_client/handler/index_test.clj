(ns graphql-client.handler.index-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ring.mock.request :as mock]
            [graphql-client.handler.index :as index]))

(deftest smoke-test
  (testing "index page exists"
    (let [handler  (ig/init-key :graphql-client.handler/index {})
          response (handler (mock/request :get "/"))]
      (is (= :ataraxy.response/ok (first response)) "response ok"))))
