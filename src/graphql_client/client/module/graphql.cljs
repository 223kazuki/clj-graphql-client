(ns graphql-client.client.module.graphql
  (:require [integrant.core :as ig]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [re-graph.core :as re-graph]
            [reagent.cookies :as cookies]
            [graphql-query.core :refer [graphql-query]]))

(defn- dissoc-in
  [m [k & ks]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

;; Initial DB
(def initial-db {})

;; Subscriptions
(defmulti reg-sub identity)
(defmethod reg-sub ::query [k]
  (re-frame/reg-sub-raw
   k (fn [app-db [_ query args path]]
       (re-frame/dispatch [:re-graph.core/query
                           (graphql-query query) args [::on-query-success path]])
       (reagent.ratom/make-reaction
        #(get-in @app-db path)
        :on-dispose #(re-frame/dispatch [::clean-db path])))))
(defmethod reg-sub ::subscription [k]
  (re-frame/reg-sub-raw
   k (fn [app-db [_ query args]]
       (let [subscription-id (keyword (pr-str query))]
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
    (let [options (-> options
                      (assoc
                       :http-parameters {:with-credentials? false
                                         :headers {"Authorization" (str "Bearer " token)}}))]
      (js/console.log (pr-str options))
      {:db (merge db initial-db)
       :dispatch [::re-graph/init options]}))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (->> db
         (filter #(not= (namespace (key %)) (namespace ::x)))
         (into {})))))
(defmethod reg-event ::on-query-success [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [db]} [path {:keys [data errors] :as payload}]]
    (if errors
      (case (get-in (first errors) [:extensions :status])
        403 {:redirect "/login"}
        {})
      {:db (assoc-in db path data)}))))
(defmethod reg-event ::clean-db [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [db]} [path]]
    {:db (dissoc-in db path)})))

;; Effects
(defmulti reg-fx identity)
(defmethod reg-fx :re-graph.internals/send-ws [k]
  (re-frame/reg-fx
   k (fn [[websocket payload]]
       (let [payload (assoc-in payload [:payload :token] (cookies/get-raw "token"))]
         (.send websocket (js/JSON.stringify (clj->js payload)))))))

;; Init
(defmethod ig/init-key :graphql-client.client.module/graphql
  [k options]
  (js/console.log (str "Initializing " k))
  (let [token (cookies/get-raw "token")
        subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))
        effects (->> reg-fx methods (map key))]
    (->> subs (map reg-sub) doall)
    (->> events (map reg-event) doall)
    (->> effects (map reg-fx) doall)
    (re-frame/dispatch-sync [::init options token])
    {:subs subs :events events :effects effects}))

;; Halt
(defmethod ig/halt-key! :graphql-client.client.module/graphql
  [k {:keys [:subs :events :effects]}]
  (js/console.log (str "Halting " k))
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall)
  (->> effects (map re-frame/clear-fx) doall))
