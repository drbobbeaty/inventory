(ns inventory.main
  "Main (simple) client for the CryptoQuip code. This doesn't do a lot (yet),
  but it's here to launch the different schemes of attaching the problem."
  (:require [clojure.tools.cli :refer [cli]]
            [clojure.tools.logging :refer [error info infof]]
            [inventory.server :refer [app]]
            [ring.adapter.jetty :as jt])
  (:gen-class))

(defn- error-msg
  "Prints a format string to stderr and logs it."
  [fmt & args]
  (let [s (apply format fmt args)]
    (.println System/err (str "CLI error: " s))
    (error s)))

(defn wrap-error-handling
  [func]
  (try (func)
       (catch Throwable t
         (.println System/err (str "Error in main: " t))
         (error t "Error in main")
         (throw t))))

(defmacro with-error-handling
  [& body]
  `(wrap-error-handling (fn [] ~@body)))

(defn handle-args
  "Function to parse the arguments to the main entry point of this project and
  do what it's asking. By the time we return, it's all done and over."
  [args]
  (let [[params [attack]] (cli args
             ["-p" "--port" "Listen on this port" :default 8080 :parse-fn #(Integer. %)]
             ["-v" "--verbose" :flag true])]
    (cond
      ; (= "block" attack)
      ;   (do
      ;     (infof "solution: %s" (blk/solve cypher clue blk/words)))
      (= "web" attack)
        (jt/run-jetty app { :port (:port params) })
      :else
        (do
          (info "Welcome to Inventory!")
          (println "Welcome to Inventory!")))))

(defn -main
  "Function to kick off everything and clean up afterwards"
  [& args]
  (with-error-handling (handle-args args)))
