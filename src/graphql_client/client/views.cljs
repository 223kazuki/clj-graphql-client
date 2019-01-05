(ns graphql-client.client.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljsjs.semantic-ui-react]
            [cljsjs.react-transition-group]
            [soda-ash.core :as sa]
            [graphql-client.client.module.router :as router]
            [graphql-client.client.module.re-graph :as re-graph]))

(defn home-panel []
  (let [droids (re-frame/subscribe [::re-graph/subscription
                                    "{ hero {id}}"])]
    (fn []
      (println @droids)
      [:div
       [sa/Segment
        [:h2 "Home"]
        [sa/Button {:on-click #(re-frame/dispatch [:re-graph.core/query
                                                   "{hero (episode: EMPIRE) {id, name}}" {}
                                                   [::re-graph/on-thing]])}
         "Query"]]])))

(defn about-panel []
  [:div [sa/Segment "About"]])

(defn none-panel []
  [:div])

(defmulti  panels identity)
(defmethod panels :home-panel [] #'home-panel)
(defmethod panels :about-panel [] #'about-panel)
(defmethod panels :none [] #'none-panel)

(def transition-group (reagent/adapt-react-class js/ReactTransitionGroup.TransitionGroup))
(def css-transition (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransition))

(defn app-container []
  (let [active-panel (re-frame/subscribe [::router/active-panel])]
    (fn []
      [:div
       [sa/Menu {:fixed "top" :inverted true}
        [sa/Container
         [sa/MenuItem {:as "span" :header true} ""]
         [sa/MenuItem {:as "a" :href "/"} "Home"]
         [sa/MenuItem {:as "a" :href "/about"} "About"]]]
       [sa/Container {:className "mainContainer" :style {:margin-top "7em"}}
        (let [panel @active-panel]
          [transition-group
           [css-transition {:key panel
                            :classNames "pageChange" :timeout 500 :className "transition"}
            [(panels panel)]]])]])))
