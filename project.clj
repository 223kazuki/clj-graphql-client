(defproject graphql-client "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [duct/core "0.7.0"]
                 [duct/module.logging "0.4.0"]
                 [duct/module.web "0.7.0"]
                 [duct/module.ataraxy "0.3.0"]
                 [duct/module.cljs "0.4.0"]

                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.9.1"]

                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [reagent-utils "0.3.2"]
                 [kibu/pushy "0.3.8"]
                 [bidi "2.1.5"]
                 [soda-ash "0.83.0" :exclusions [[cljsjs/react]]]
                 [cljsjs/react-transition-group "2.4.0-0"]
                 [re-graph "0.1.7"]
                 [district0x/graphql-query "1.0.5"]]
  :plugins [[duct/lein-duct "0.11.0"]]
  :main ^:skip-aot graphql-client.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.3.1"]
                                   [eftest "0.5.4"]
                                   [kerodon "0.9.0"]
                                   [alembic "0.3.2"]
                                   [meta-merge "1.0.0"]
                                   [day8.re-frame/re-frame-10x "0.3.6"]
                                   [day8.re-frame/tracing "0.5.1"]]}})
