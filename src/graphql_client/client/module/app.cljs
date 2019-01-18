(ns graphql-client.client.module.app
  (:require [integrant.core :as ig]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [graphql-client.client.views :as views]))

;; Initial DB
(def initial-db {::errors nil ::loading? false})

;; Subscriptions
(defmulti reg-sub identity)
(defmethod reg-sub ::errors [k]
  (re-frame/reg-sub k #(::errors %)))
(defmethod reg-sub ::loading? [k]
  (re-frame/reg-sub k #(::loading? %)))

;; Events
(defmulti reg-event identity)
(defmethod reg-event ::init [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (merge db initial-db))))
(defmethod reg-event ::halt [k]
  (re-frame/reg-event-db
   k [re-frame/trim-v]
   (fn-traced
    [db _]
    (->> db
         (filter #(not= (namespace (key %)) (namespace ::x)))
         (into {})))))

;; Effects
(defmulti reg-fx identity)
(defmethod reg-fx :redirect [k]
  (re-frame/reg-fx
   k (fn [path]
       (set! js/location.href path))))

;; Init
(defmethod ig/init-key :graphql-client.client.module/app
  [k {:keys [:mount-point-id]}]
  (js/console.log (str "Initializing " k))
  (let [subs (->> reg-sub methods (map key))
        events (->> reg-event methods (map key))
        effects (->> reg-fx methods (map key))
        container (.getElementById js/document mount-point-id)]
    (->> subs (map reg-sub) doall)
    (->> effects (map reg-fx) doall)
    (->> events (map reg-event) doall)
    (re-frame/dispatch-sync [::init])
    (when container (reagent/render [views/app-container] container))
    {:subs subs :events events :effects effects :container container}))

;; Halt
(defmethod ig/halt-key! :graphql-client.client.module/app
  [k {:keys [:subs :events :effects :container]}]
  (js/console.log (str "Halting " k))
  (reagent/unmount-component-at-node container)
  (re-frame/dispatch-sync [::halt])
  (->> subs (map re-frame/clear-sub) doall)
  (->> events (map re-frame/clear-event) doall)
  (->> effects (map re-frame/clear-fx) doall))
