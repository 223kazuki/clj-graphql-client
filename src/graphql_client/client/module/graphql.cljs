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
(defmethod reg-sub ::websocket-ready? [k]
  (re-frame/reg-sub k #(get-in % [:re-graph :re-graph.internals/default :websocket :ready?])))
(defmethod reg-sub ::query [k]
  (re-frame/reg-sub-raw
   k (fn [app-db [_ query args path]]
       (re-frame/dispatch [::re-graph/query
                           (graphql-query query) args [::on-query-success path]])
       (reagent.ratom/make-reaction
        #(get-in @app-db path)
        :on-dispose #(re-frame/dispatch [::clean-db path])))))
(defmethod reg-sub ::subscription [k]
  (re-frame/reg-sub-raw
   k (fn [app-db [_ query args path]]
       (let [subscription-id (keyword (pr-str query))]
         (re-frame/dispatch [::re-graph/subscribe
                             subscription-id (graphql-query query) args
                             [::on-thing path]])
         (reagent.ratom/make-reaction
          #(get-in @app-db path)
          :on-dispose #(re-frame/dispatch [::re-graph/unsubscribe subscription-id]))))))

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [:db]} [options token]]
    (let [options (cond-> options
                    (:ws-url options) (update-in [:ws-url] str "?token=" token)
                    true (assoc
                          :http-parameters {:with-credentials? false
                                            :headers {"Authorization" (str "Bearer " token)}}))]
      {:db (merge db initial-db)
       :dispatch [::re-graph/init options]}))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-fx
   k [re-frame/trim-v]
   (fn-traced
    [{:keys [:db]} _]
    {:db (->> db
              (filter #(not= (namespace (key %)) (namespace ::x)))
              (into {}))
     :dispatch [::re-graph/destroy]})))
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
(defmethod reg-event ::on-thing [k]
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

(defn- on-ws-message [instance-name]
  (fn [m]
    (let [data (js/JSON.parse (aget m "data"))]
      (condp = (aget data "type")
        "data"
        (re-frame/dispatch [:re-graph.internals/on-ws-data instance-name (aget data "id") (js->clj (aget data "payload") :keywordize-keys true)])

        "complete"
        (re-frame/dispatch [:re-graph.internals/on-ws-complete instance-name (aget data "id")])

        "error"
        (js/console.warn (str "GraphQL error for " instance-name " - " (aget data "id") ": " (aget data "payload" "message")))

        (js/console.debug "Ignoring graphql-ws event " instance-name " - " (aget data "type"))))))

(defn- on-open [instance-name ws]
  (fn [e]
    (re-frame/dispatch [:re-graph.internals/on-ws-open instance-name ws])))

(defn- on-close [instance-name]
  (fn [e]
    (re-frame/dispatch [:re-graph.internals/on-ws-close instance-name])))

(defn- on-error [instance-name]
  (fn [e]
    (js/console.warn "GraphQL websocket error" instance-name e)
    (set! js/location.href "/login")))

;; Effects
(defmulti reg-fx identity)
(defmethod reg-fx :re-graph.internals/connect-ws [k]
  (re-frame/reg-fx
   k (fn [[instance-name ws-url]]
       (let [ws (js/WebSocket. ws-url "graphql-ws")]
         (aset ws "onmessage" (on-ws-message instance-name))
         (aset ws "onopen" (on-open instance-name ws))
         (aset ws "onclose" (on-close instance-name))
         (aset ws "onerror" (on-error instance-name))))))

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
