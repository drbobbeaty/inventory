(ns inventory.database
  "Namespace for accessing the database for all the persistent data we will
  be gathering and serving up."
  (:require [inventory.config :as cfg]
            [inventory.logging :refer [log-execution-time!]]
            [cheshire.core :as json]
            [clj-time.coerce :refer [to-timestamp from-date]]
            [clojure.java.jdbc :refer [IResultSetReadColumn result-set-read-column] :as sql]
            [clojure.string :as cs]
            [clojure.tools.logging :refer [error errorf warnf infof info]]
            [honeysql.core :as hsql]
            [honeysql.helpers :refer [limit]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource
           java.sql.SQLException
           java.lang.Throwable))

;;
;; Some support code customizing clojure.java.jdbc to handle
;; database arrays.
;;

;; Extending this protocol causes java.jdbc to automatically convert
;; different types as we read them in, and is necessary to support
;; reading arrays into a vector.
(extend-protocol IResultSetReadColumn
  java.sql.Array
  (result-set-read-column [sqlarray _ _]
    ((fn read-array [^java.sql.Array arr]
       (let [rs (.getResultSet arr)]
         (loop [rows []]
           (if (.next rs)
             (let [elem (.getObject rs 2)]
               (recur (conj rows (if (instance? java.sql.Array elem)
                                   (-> elem (read-array))
                                   elem))))
             rows))))
     sqlarray))

  java.sql.Timestamp
  (result-set-read-column [ts _ _]
    (from-date ts))

  java.sql.Date
  (result-set-read-column [ts _ _]
    (from-date ts)))

;;
;; General functions that will be used in conversion from, or to, the types
;; necessary for saving in the database.
;;

(defn format-as-uuid
  "Function to format a UUID as a hex string into the typical representation of
  a UUID with the '-' at the proper locations in the string. This function checks
  to make sure it's the right length coming in and then just chops it up and
  rebuilds it in the proper form."
  [id]
  (if (and (string? id) (= (count id) 32))
    (cs/lower-case (str (subs id 0 8) "-" (subs id 8 12) "-" (subs id 12 16) "-" (subs id 16 20) "-" (subs id 20)))
    id))

(defn- ->sql-array*
  "Create a java.sql.Array from the given collection. Uses the second argument as
  the return value of the getBaseTypeName function for the data type."
  [coll basetype]
  (reify
    Object
    (toString [self] (str \{ (cs/join \, (map #(or % "NULL") coll)) \}))
    (getBaseTypeName [self] basetype)
    java.sql.Array))

(defn ->sql-array
  "create a java.sql.Array from the given collection. Assumes the base type of
  the collection is the same as the _first_ element in the collection. If the
  collection is hetergeneous, then this is going to break in a very bad way."
  [coll & [type-hint]]
  (if-not (empty? coll)
    (let [f (first (remove nil? coll))
          th (or type-hint
               (cond
                 (nil? f)     :varchar
                 (string? f)  :varchar
                 (integer? f) :integer
                 (float? f)   :double))]
      (cond
        (= :varchar th) (->sql-array* (map #(if % (str \" % \") "NULL") coll) "varchar")
        (= :integer th) (->sql-array* coll "int4")
        (= :double th)  (->sql-array* coll "float8")))))

;; Create a pooled connection from the config parameters in project
(defonce pooled-connection
  (delay
    (let [{:keys [classname subprotocol subname user password min-pool-size max-pool-size]} (cfg/database)]
      {:datasource (doto (ComboPooledDataSource.)
                     (.setDriverClass classname)
                     (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
                     (.setUser user)
                     (.setPassword password)
                     ;; yay magic numbers!
                     (.setMaxIdleTimeExcessConnections (* 30 60))
                     (.setMaxIdleTime (* 3 60 60))
                     (.setMinPoolSize (or min-pool-size 3))
                     (.setMaxPoolSize (or max-pool-size 15)))})))

;; Somewhat faking java.jdbc's original *connection* behavior so that
;; we don't have to pass one around.
(def ^:dynamic *connection* nil)

(defn connection []
  []
  (or *connection* @pooled-connection))

(defmacro transaction
  [& body]
  `(sql/with-db-transaction [con# (connection)]
     (binding [*connection* con#]
       ~@body)))

(defn query
  "A wrapper around clojure.java.jdbc's clunky query macro.

   Argument can be a SQL string, a parameterized vector as in the
   sql/with-query-results macro, or (recommended) a honeysql query
   object. Any extra arguments are passed through directly to
   java.jdbc/query, so processing optimizations (like stream
   processing) can be obtained that way."
  [expr & query-opts]
  (let [query-arg (cond (string? expr) [expr]
                        (map? expr) (hsql/format expr)
                        :else expr)
        ;; I'm assuming it's okay to use the concurrency options here, but
        ;; I can't say I understand them well enough to be sure.
        query-arg-with-opts (apply vector
                                   {:concurrency :read-only
                                    :result-type :forward-only}
                                   query-arg)]
    (try
      (apply sql/query (connection) query-arg-with-opts query-opts)
      (catch SQLException se
        (warnf "SQLException thrown on: %s :: %s" query-arg-with-opts (.getMessage se)))
      (catch Throwable t
        (warnf "Exception thrown on: %s :: %s" query-arg-with-opts (.getMessage t))))))

(defn do-commands
  "Function to execute several SQL commands and not retain any of the output.
  This is typically done when you need to migrate or update the database with
  DDL commands, and you want them all to be done within one transaction."
  [& cmds]
  (if-not (empty? cmds)
    (try
      (apply sql/db-do-commands (connection) true cmds)
      (catch SQLException se
        (warnf "SQLException thrown on: %s :: %s" cmds (.getMessage se)))
      (catch Throwable t
        (warnf "Exception thrown on: %s :: %s" cmds (.getMessage t))))))

(defn insert!
  "Function to execute a SQL insert command for the provided rows into the
  provided table name - given as a keyword. This is the simple way to get
  data into the database - trapping for any errors, and logging them."
  [tbl rows]
  (if-not (empty? rows)
    (try
      (apply sql/insert! (connection) tbl rows)
      (catch SQLException se
        (warnf "SQLException thrown on: %s :: %s" rows (.getMessage se)))
      (catch Throwable t
        (warnf "Exception thrown on: %s :: %s" rows (.getMessage t))))))

(defn delete!
  "Function to execute a SQL delete command for the provided where clause
  from the provided table name - given as a keyword. This is the simple way
  to remove data from the database - trapping for any errors, and logging them."
  [tbl clause]
  (if-not (empty? clause)
    (try
      (sql/delete! (connection) tbl clause)
      (catch SQLException se
        (warnf "SQLException thrown on: %s :: %s" clause (.getMessage se)))
      (catch Throwable t
        (warnf "Exception thrown on: %s :: %s" clause (.getMessage t))))))