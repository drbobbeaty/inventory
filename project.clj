(defproject inventory "0.1.0"
  :description "Simple inventory control system where editing is allowed."
  :url "https://github.com/drbobbeaty/inventory"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :min-lein-version "2.3.4"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ;; nice utilities
                 [clj-time "0.6.0"]
                 ;; command line option processing
                 [org.clojure/tools.cli "0.2.2"]
                 ;; logging with log4j
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [log4j/log4j "1.2.17"]
                 [org.clojure/tools.logging "0.2.6"]
                 [robert/hooke "1.3.0"]
                 ;; HTTP Client
                 [clj-http "1.1.2"]
                 ;; JSON parsing library
                 [cheshire "5.3.1"]
                 ;; web server
                 [compojure "1.3.1"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring.middleware.jsonp "0.1.6"]
                 ;; all the database goodies
                 [c3p0 "0.9.1.2"]
                 [honeysql "0.5.2"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/java.jdbc "0.3.2"]]
  :main inventory.main)
