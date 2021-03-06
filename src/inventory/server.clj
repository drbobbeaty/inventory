(ns inventory.server
  "The routes for the web server."
  (:require [compojure
              [core :refer [defroutes GET POST]]
              [handler :as handler]
              [route :as route]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.core :refer [now]]
            [clojure.tools.logging :refer [infof warn warnf error errorf]]
            [cheshire.core :as json]
            [inventory.core :as core]
            [inventory.google :as ig]
            [inventory.util :refer [ucase git-commit project-version]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :as resp]
            [ring.util.io :refer [piped-input-stream]])
  (:import [java.io BufferedWriter OutputStreamWriter IOException]))

(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [t jg]
    (cheshire.generate/write-string jg (str t))))

(defn return-code
  "Creates a ring response for returning the given return code."
  [code]
  {:status code
   :headers {"Content-Type" "application/json; charset=UTF-8"}})

(defn return-json
  "Creates a ring response for returning the given object as JSON."
  ([ob] (return-json ob (now) 200))
  ([ob lastm] (return-json ob lastm 200))
  ([ob lastm code]
    {:status code
     :headers {"Content-Type" "application/json; charset=UTF-8"
               "Access-Control-Allow-Origin" "*"
               "Last-Modified" (str (or lastm (now)))}
     :body (piped-input-stream
             (bound-fn [out]
               (with-open [osw (OutputStreamWriter. out)
                           bw (BufferedWriter. osw)]
                 (let [error-streaming
                       (fn [e]
                         ;; Since the HTTP headers have already been sent,
                         ;; at this point it is too late to report the error
                         ;; as a 500. The best we can do is abruptly print
                         ;; an error and quit.
                         (.write bw "\n\n---INVENTORY SERVICE ERROR WHILE STREAMING JSON---\n")
                         (.write bw (str e "\n\n"))
                         (warnf "Streaming exception for JSONP: %s" (.getMessage e)))]
                   (try
                     (json/generate-stream ob bw)
                     ;; Handle "pipe closed" errors
                     (catch IOException e
                       (if (re-find #"Pipe closed" (.getMessage e))
                         (infof "Pipe Closed exception: %s" (.getMessage e))
                         (error-streaming e)))
                     (catch Throwable t
                       (error-streaming t)))))))}))

(defroutes app-routes
  "Primary routes for the webserver."
  (GET "/" []
    (resp/redirect "/index.html"))
  (GET "/info" []
    (return-json {:app "inventory service",
                  :hello? "World!",
                  :version (project-version)
                  :code (or (git-commit) "unknown commit")}))
  (GET "/heartbeat" []
    (return-code 200))
  ;; pull the inventory of cars we have right now
  (GET "/v1/cars" [:as {headers :headers}]
    (let [auth (get (or headers {}) "authorization")]
      (if (ig/authorized? auth)
        (return-json (core/pull-cars))
        (return-code 401))))
  ;; let's update the inventory of cars we should have now
  (POST "/v1/cars" [:as {headers :headers body :body}]
    (let [auth (get (or headers {}) "authorization")
          cfg (json/parse-string (slurp body) true)
          manus (:manufacturers cfg)
          years (:model_years cfg)
          tdata (:inventory cfg)]
      (if (ig/authorized? auth)
        (if (and (coll? manus) (coll? years) (coll? tdata))
          (return-json (core/update-cars! manus years tdata))
          (return-code 400))
        (return-code 401))))
  ;; Finish up with the static resources and the 404 page
  (route/resources "/")
  (route/not-found "<h1>Page not Found!</h1>"))

(defn wrap-logging
  "Ring middleware to log requests and exceptions."
  [handler]
  (fn [req]
    (infof "Handling request: %s %s from: %s" (ucase (name (:request-method req))) (:uri req) (:remote-addr req))
    (try (handler req)
         (catch Throwable t
           (error t "Server error!")
           (throw t)))))

(def app
  "The actual ring handler that is run -- this is the routes above
   wrapped in various middlewares."
  (-> app-routes
      wrap-json-with-padding
      handler/site
      wrap-params
      wrap-logging))
