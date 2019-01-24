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
                         {:query/data [:rikishis {:first 20 :after :$after}
                                       [[:pageInfo [:hasNextPage :endCursor]]
                                        [:edges [[:node [:id :shikona :banduke
                                                         [:sumobeya [:name]]]]]]]]}]}
        path [::rikishis]
        rikishis (re-frame/subscribe [::graphql/sub-query query {}
                                      path ])]
    (fn []
      (when-let [rikishis @rikishis]
        (let [favorites (->> (:favoriteRikishis rikishis)
                             (map :id)
                             set)
              {:keys [edges pageInfo]} (:rikishis rikishis)
              {:keys [hasNextPage endCursor]} pageInfo]
          [sa/Table
           [sa/TableHeader
            [sa/TableRow
             [sa/TableHeaderCell "四股名"]
             [sa/TableHeaderCell "番付"]
             [sa/TableHeaderCell "所属"]
             [sa/TableHeaderCell]]]
           [sa/Visibility {:as "tbody"
                           :on-update (fn [_ ctx]
                                        (let [{:keys [percentagePassed offScreen bottomPassed onScreen width topPassed fits
                                                      pixelsPassed passing topVisible direction height bottomVisible] :as calc}
                                              (js->clj (aget ctx "calculations")
                                                       :keywordize-keys true)]
                                          (when (and bottomVisible hasNextPage)
                                            (re-frame/dispatch [::graphql/fetch-more
                                                                query path :rikishis])
                                            (js/console.log "fetch more!"))))}
            (for [{{:keys [id shikona banduke sumobeya]} :node} edges]
              [sa/TableRow {:key id}
               [sa/TableCell [:a {:href (str "/rikishis/" id)} shikona]]
               [sa/TableCell banduke]
               [sa/TableCell (:name sumobeya)]
               [sa/TableCell
                (if (favorites id)
                  [sa/Rating {:icon "star" :rating 1 :max-rating 1
                              :on-click
                              #(re-frame/dispatch [::graphql/mutate
                                                   {:operation {:operation/type :mutation
                                                                :operation/name "unfavRikishi"}
                                                    :queries [{:query/data [:unfavRikishi {:rikishiId id}
                                                                            [:id]]
                                                               :query/alias :favoriteRikishis}]}
                                                   {}
                                                   path])}]
                  [sa/Rating {:icon "star" :rating 0 :max-rating 1
                              :on-click
                              #(re-frame/dispatch [::graphql/mutate
                                                   {:operation {:operation/type :mutation
                                                                :operation/name "favRikishi"}
                                                    :queries [{:query/data [:favRikishi {:rikishiId id}
                                                                            [:id]]
                                                               :query/alias :favoriteRikishis}]}
                                                   {}
                                                   path])}])]])]])))))

(defn home-panel []
  (let [websocket-ready? (re-frame/subscribe [::graphql/websocket-ready?])]
    (fn []
      [sa/Segment
       [:h2 "力士一覧"]
       (when @websocket-ready?
         [_home-panel])])))

(defn _rikishi-panel [{:keys [rikishi-id]}]
  (let [query {:operation {:operation/type :query
                           :operation/name :rikishiQuery}
               :variables [{:variable/name :$id
                            :variable/type :Int!}]
               :queries [{:query/data [:rikishi {:id :$id}
                                       [:id :shikona :banduke :syusshinchi
                                        [:sumobeya [:name [:rikishis [:id :shikona :banduke]]]]]]}]}
        rikishi-id (js/parseInt rikishi-id)
        path [::rikishi]
        rikishi (re-frame/subscribe [::graphql/sub-query query {:id rikishi-id} path])]
    (when-let [{:keys [shikona banduke syusshinchi sumobeya]} (:rikishi @rikishi)]
      (let [{:keys [name rikishis]} sumobeya]
        [:<>
         [:h2 [:a {:href "/"} "力士一覧"] " > " shikona]
         [sa/Table
          [sa/TableBody
           [sa/TableRow
            [sa/TableCell {:width 4}  "番付"]
            [sa/TableCell banduke]]
           [sa/TableRow
            [sa/TableCell "部屋"]
            [sa/TableCell name]]
           [sa/TableRow
            [sa/TableCell "同部屋力士"]
            [sa/TableCell (->> (for [{:keys [id shikona banduke]} rikishis]
                                 (when (not= rikishi-id id)
                                   (str banduke " " shikona)))
                               (filter some?)
                               (clojure.string/join ", "))]]]]]))))

(defn rikishi-panel []
  (let [websocket-ready? (re-frame/subscribe [::graphql/websocket-ready?])
        route-params (re-frame/subscribe [::router/route-params])]
    (fn []
      [sa/Segment
       (when (and @websocket-ready? @route-params)
         [_rikishi-panel @route-params])])))

(defn about-panel []
  [:div [sa/Segment "About"]])

(defn none-panel []
  [:div])

(defmulti  panels identity)
(defmethod panels :home-panel [] #'home-panel)
(defmethod panels :about-panel [] #'about-panel)
(defmethod panels :rikishi-panel [] #'rikishi-panel)
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
