(ns inventory.core
	"Main namespace for what needs to be done"
  (:require [clojure.tools.logging :refer [error info infof]]
            [inventory.database :as db]
            [inventory.logging :refer [log-execution-time!]]))

(defn pull-cars
  "Function to pull the most recent auto inventory from the database and
  return it in a structure with the model years, the manufacturers, and
  the inventory numbers. This is just something that makes it easy to look
  at the whole picture at once."
  []
  (let [rows (db/query ["select model_year, manufacturer, sum(quantity) as quantity
                           from cars
                          where as_of = (select max(as_of) from cars)
                         group by model_year, manufacturer"])
        manus (sort (distinct (map :manufacturer rows)))
        years (sort (distinct (map :model_year rows)))]
    { :manufacturers manus
      :model_years years
      :inventory (for [y years]
                   (for [m manus]
                    (or
                      (:quantity (first (filter #(and (= y (:model_year %)) (= m (:manufacturer %))) rows)))
                      0))) }))

(log-execution-time! pull-cars)

