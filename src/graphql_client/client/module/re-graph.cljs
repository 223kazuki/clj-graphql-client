(ns graphql-client.client.module.re-graph
  (:require [integrant.core :as ig]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [re-graph.core :as re-graph]
            [reagent.cookies :as cookies]))

;; Initial DB
(def initial-db {})

;; Subscriptions
(defmulti reg-sub identity)
(defmethod reg-sub ::subscription [k]
  (re-frame/reg-sub-raw
   k (fn [app-db [_ query args]]
       (let [subscription-id (keyword (pr-str query))]
         #_ (re-frame/dispatch [::re-graph/query query args
                                [::on-thing subscription-id]])
         (re-frame/dispatch [::re-graph/subscribe
                             subscription-id query args
                             [::on-thing subscription-id]])
         (reagent.ratom/make-reaction
          #(get-in @app-db [subscription-id])
          :on-dispose #(re-frame/dispatch [::re-graph/unsubscribe subscription-id]))))))

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [:db]} [options token]]
    (if token
      (let [options (assoc options
                           :http-parameters {:with-credentials? true
                                             :oauth-token token})]
        {:db (merge db initial-db)
         :dispatch [::re-graph/init options]})
      {:redirect "/login"}))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (->> db
         (filter #(not= (namespace (key %)) (namespace ::x)))
         (into {})))))
(defmethod reg-event ::on-thing [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db [{:keys [data errors] :as payload} subscription-id]]
    (println subscription-id payload)
    db)))

;; Init
(defmethod ig/init-key :graphql-client.client.module/re-graph
  [k options]
  (js/console.log (str "Initializing " k))
  (let [token (cookies/get-raw "token")
        subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))]
    (->> subs (map reg-sub) doall)
    (->> events (map reg-event) doall)
    (re-frame/dispatch-sync [::init options token])
    {:subs subs :events events}))

;; Halt
(defmethod ig/halt-key! :graphql-client.client.module/re-graph
  [k {:keys [:subs :events]}]
  (js/console.log (str "Halting " k))
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall))
