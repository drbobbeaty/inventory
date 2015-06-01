(ns inventory.config
  "This is the code that loads the config.clj file from the classpath
  and makes that data available. The easiest way to use is to call the
  config function with the keys for the item you want, e.g.

    (bartender.config/config :database :user)

  will return the user for the database."
  (:require [clojure.java.io :refer [resource]]))

(def cfg (delay
          (if-let [file (resource "config.clj")]
            (read-string (slurp file))
            (throw (Exception. "Cannot find config.clj on the classpath!")))))

(defn config
  ([k & ks] (get-in (config) (cons k ks)))
  ([] @cfg))

(def redis      (partial config :redis))
(def database   (partial config :database))
(def hipchat    (partial config :hipchat))
