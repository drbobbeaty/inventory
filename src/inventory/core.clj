(ns inventory.core
	"Main namespace for what needs to be done"
  (:require [clj-time.coerce :refer [to-timestamp]]
            [clj-time.core :refer [now]]
            [clojure.tools.logging :refer [error info infof]]
            [inventory.database :as db]
            [inventory.logging :refer [log-execution-time!]]
            [inventory.util :refer [parse-int]]))

(defn pull-cars
  "Function to pull the most recent auto inventory from the database and
  return it in a structure with the model years, the manufacturers, and
  the inventory numbers. This is just something that makes it easy to look
  at the whole picture at once."
  []
  (let [rows (db/query ["select as_of, model_year, manufacturer, sum(quantity) as quantity
                           from cars
                          where as_of = (select max(as_of) from cars)
                         group by as_of, model_year, manufacturer"])
        manus (sort (distinct (map :manufacturer rows)))
        years (sort (distinct (map :model_year rows)))]
    { :as_of (:as_of (first rows))
      :manufacturers manus
      :model_years years
      :inventory (for [y years]
                   (for [m manus]
                    (or
                      (:quantity (first (filter #(and (= y (:model_year %)) (= m (:manufacturer %))) rows)))
                      0))) }))

(log-execution-time! pull-cars)

(defn update-cars!
  "Function to update the complete inventory based on the current values passed
  in."
  [manus years inv]
  (if (and manus years inv)
    (let [as-of (to-timestamp (now))
          raw (apply concat (for [[y cnts] (map vector years inv)]
                              (for [[m cnt] (map vector manus cnts)]
                                { :as_of as-of
                                  :model_year y
                                  :manufacturer m
                                  :quantity (parse-int cnt) }
                              )))
          resp (db/insert! :cars raw)]
      (if resp
        {:status "OK"}
        {:status "Error"}))))

(log-execution-time! update-cars!)
