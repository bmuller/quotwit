(defproject quotwit "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main quotwit.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-oauth "1.5.2"]
                 [compojure "1.3.4"]
                 [lib-noir "0.9.9"]
                 [ring/ring-defaults "0.1.5"]
                 [twitter-api "0.7.8"]] 
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler quotwit.web/app})
