(ns graphql-client.client.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljsjs.semantic-ui-react]
            [cljsjs.react-transition-group]
            [soda-ash.core :as sa]
            [graphql-client.client.module.router :as router]
            [graphql-client.client.module.graphql :as graphql]))

#_(defn _home-panel []
    (let [rikishi (re-frame/subscribe [::graphql/query
                                       {:queries [[:rikishi {:id 1} [:id :shikona]]
                                                  [:sumobeya {:id 2} [:name]]]} {}
                                       [::rikishi]])
          torikumis (re-frame/subscribe [::graphql/subscription
                                         {:queries [[:torikumis {:num 3} [:id :kimarite]]]} {}
                                         [::torikumis]])]
      (fn []
        [:<>
         [:p (str @rikishi)]
         [:p (str @torikumis)]])))

(defn _home-panel []
  (let [query {:operation {:operation/type :query
                           :operation/name :rikishisQuery}
               :variables [{:variable/name :$after
                            :variable/type :String}]
               :queries [{:query/data [:favoriteRikishis [:id]]}
                         {:query/data [:rikishis {:first 10 :after :$after}
                                       [[:pageInfo [:hasNextPage :endCursor]]
                                        [:edges [[:node [:id :shikona :banduke
                                                         [:sumobeya [:name]]]]]]]]}]}
        rikishis (re-frame/subscribe [::graphql/query query {}
                                      [::rikishis]])]
    (fn []
      (when-let [{:keys [edges pageInfo]} (:rikishis @rikishis)]
        (let [{:keys [hasNextPage endCursor]} pageInfo]
          (println endCursor)
          [sa/Table
           [sa/TableHeader
            [sa/TableRow
             [sa/TableHeaderCell "四股名"]
             [sa/TableHeaderCell "番付"]
             [sa/TableHeaderCell "部屋"]]]
           [sa/Visibility {:as "tbody"
                           :on-update (fn [_ ctx]
                                        (let [{:keys [percentagePassed offScreen bottomPassed onScreen width topPassed fits
                                                      pixelsPassed passing topVisible direction height bottomVisible] :as calc}
                                              (js->clj (aget ctx "calculations")
                                                       :keywordize-keys true)]
                                          (when (and bottomVisible hasNextPage)
                                            (js/console.log "fetch more!"))))}
            (for [{{:keys [id shikona banduke sumobeya]} :node} edges]
              [sa/TableRow {:key id}
               [sa/TableCell shikona]
               [sa/TableCell banduke]
               [sa/TableCell (:name sumobeya)]])]])))))

(defn home-panel []
  (let [websocket-ready? (re-frame/subscribe [::graphql/websocket-ready?])]
    (fn []
      [sa/Segment
       [:h2 "Home"]
       (when @websocket-ready?
         [_home-panel])])))

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
