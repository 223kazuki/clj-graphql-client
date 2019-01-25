(ns graphql-client.client.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljsjs.semantic-ui-react]
            [cljsjs.react-transition-group]
            [soda-ash.core :as sa]
            [graphql-client.client.module.router :as router]
            [graphql-client.client.module.graphql :as graphql]))

(def kimarite-map
  {"TSUKIDASHI" "突き出し"
   "TSUKITAOSHI" "突き倒し"
   "OSHIDASHI" "押し出し"
   "OSHITAOSHI" "押し倒し"
   "YORIKIRI" "寄り切り"
   "YORITAOSHI" "寄り倒し"
   "ABISETAOSHI" "浴びせ倒し"
   "UWATENAGE" "上手投げ"
   "SHITATENAGE" "下手投げ"
   "KOTENAGE" "小手投げ"
   "SUKUINAGE" "掬い投げ"
   "UWATEDASHINAGE" "上手出し投げ"
   "SHITATEDASHINAGE" "下手出し投げ"
   "KOSHINAGE" "腰投げ"
   "KUBINAGE" "首投げ"
   "IPPONZEOI" "一本背負い"
   "NICHONAGE" "二丁投げ"
   "YAGURANAGE" "櫓投げ"
   "KAKENAGE" "掛け投げ"
   "TSUKAMINAGE" "つかみ投げ"
   "UCHIGAKE" "内掛け"
   "SOTOGAKE" "外掛け"
   "CHONGAKE" "ちょん掛け"
   "KIRIKAESHI" "切り返し"
   "KAWAZUGAKE" "河津掛け"
   "KEKAESHI" "蹴返し"
   "KETAGURI" "蹴手繰り"
   "MITOKOROZEME" "三所攻め"
   "WATASHIKOMI" "渡し込み"
   "NIMAIGERI" "二枚蹴り"
   "KOMATASUKUI" "小股掬い"
   "SOTOKOMATA" "外小股"
   "OMATA" "大股"
   "TSUMATORI" "褄取り"
   "KOZUMATORI" "小褄取り"
   "ASHITORI" "足取り"
   "SUSOTORI" "裾取り"
   "SUSOHARAI" "裾払い"
   "IZORI" "居反り"
   "SHUMOKUZORI" "撞木反り"
   "KAKEZORI" "掛け反り"
   "TASUKIZORI" "たすき反り"
   "SOTOTASUKIZORI" "外たすき反り"
   "TSUTAEZORI" "伝え反り"
   "TSUKIOTOSHI" "突き落とし"
   "MAKIOTOSHI" "巻き落とし"
   "TOTTARI" "とったり"
   "SAKATOTTARI" "逆とったり"
   "KATASUKASHI" "肩透かし"
   "SOTOMUSO" "外無双"
   "UCHIMUSO" "内無双"
   "ZUBUNERI" "頭捻り"
   "UWATEHINERI" "上手捻り"
   "SHITATEHINERI" "下手捻り"
   "AMIUCHI" "網打ち"
   "SABAORI" "鯖折り"
   "HARIMANAGE" "波離間投げ"
   "OSAKATE" "大逆手"
   "KAINAHINERI" "腕捻り"
   "GASSHOHINERI" "合掌捻り"
   "TOKKURINAGE" "徳利投げ"
   "KUBIHINERI" "首捻り"
   "KOTEHINERI" "小手捻り"
   "HIKIOTOSHI" "引き落とし"
   "HIKKAKE" "引っ掛け"
   "HATAKIKOMI" "叩き込み"
   "SOKUBIOTOSHI" "素首落とし"
   "TSURIDASHI" "吊り出し"
   "OKURITSURIDASHI" "送り吊り出し"
   "TSURIOTOSHI" "吊り落とし"
   "OKURITSURIOTOSHI" "送り吊り落とし"
   "OKURIDASHI" "送り出し"
   "OKURITAOSHI" "送り倒し"
   "OKURINAGE" "送り投げ"
   "OKURIGAKE" "送り掛け"
   "OKURIHIKIOTOSHI" "送り引き落とし"
   "WARIDASHI" "割り出し"
   "UTCHARI" "うっちゃり"
   "KIMEDASHI" "極め出し"
   "KIMETAOSHI" "極め倒し"
   "USHIROMOTARE" "後ろもたれ"
   "YOBIMODOSHI" "呼び戻し"
   "ISAMIASHI" "勇み足"
   "KOSHIKUDAKE" "腰砕け"
   "TSUKITE" "つき手"
   "TSUKIHIZA" "つきひざ"
   "FUMIDASHI" "踏み出し"})

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

(defn _torikumi-panel []
  (let [query {:operation {:operation/type :subscription
                           :operation/name :torikumis}
               :variables [{:variable/name :$num
                            :variable/type :Int!}]
               :queries [{:query/data [:torikumis {:num :$num}
                                       [:id :kimarite [:higashi [:id :shikona]]
                                        [:nishi [:id :shikona]] [:shiroboshi [:id]]]]}]}
        path [::torikumis]
        torikumis (re-frame/subscribe [::graphql/sub-subscription query {:num 10}
                                       path])]
    (fn []
      (when-let [{:keys [torikumis]} @torikumis]
        [sa/Table
         [sa/TableHeader
          [sa/TableRow
           [sa/TableHeaderCell {:text-align "center" :width 5} "東"]
           [sa/TableHeaderCell {:text-align "center" :width 5} "西"]
           [sa/TableHeaderCell "決まり手"]]]
         [sa/TableBody
          (for [{:keys [id higashi nishi shiroboshi kimarite] :as torikumi} torikumis]
            (do
              (println torikumi)
              [sa/TableRow {:key id}
               [sa/TableCell {:text-align "center"
                              :style (when (== (:id higashi)
                                               (:id shiroboshi))
                                       {:background-color "red"})}
                (:shikona higashi)]
               [sa/TableCell {:text-align "center"
                              :style (when (== (:id nishi)
                                               (:id shiroboshi))
                                       {:background-color "red"})}
                (:shikona nishi)]
               [sa/TableCell (kimarite-map kimarite)]]))]]))))

(defn torikumi-panel []
  (let [websocket-ready? (re-frame/subscribe [::graphql/websocket-ready?])]
    (fn []
      [sa/Segment
       [:h2 "取り組み速報"]
       (when @websocket-ready?
         [_torikumi-panel])])))

(defn none-panel []
  [:div])

(defmulti  panels identity)
(defmethod panels :home-panel [] #'home-panel)
(defmethod panels :torikumi-panel [] #'torikumi-panel)
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
         [sa/MenuItem {:as "a" :href "/"} "力士一覧"]
         [sa/MenuItem {:as "a" :href "/torikumi"} "取組速報"]]]
       [sa/Container {:className "mainContainer" :style {:margin-top "7em"}}
        (let [panel @active-panel]
          [transition-group
           [css-transition {:key panel
                            :classNames "pageChange" :timeout 500 :className "transition"}
            [(panels panel)]]])]])))
