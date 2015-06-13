(ns inventory.util
  "Lots of nice little utility functions for the project."
  (:require [clojure.string :as cs]
            [clojure.tools.logging :refer [error info infof]]))

(defn ucase
  "Function to convert the string argument to it's upper-case equivalent - but
  do so in a clean manner. This means that if it's a `nil`, you get back a `nil`,
  and if it's a number, you get that number back. This was left out of the spec
  in the original, and it makes for code like this to clean things up. If you
  pass in a collection, this function will call itself on all the values in that
  collection so you can upper-case a collection without worrying about the other
  types of values."
  [s]
  (cond
    (string? s) (cs/upper-case s)
    (coll? s)   (map ucase s)
    :else       s))

(defn lcase
  "Function to convert the string argument to it's lower-case equivalent - but
  do so in a clean manner. This means that if it's a `nil`, you get back a `nil`,
  and if it's a number, you get that number back. This was left out of the spec
  in the original, and it makes for code like this to clean things up. If you
  pass in a collection, this function will call itself on all the values in that
  collection so you can lower-case a collection without worrying about the other
  types of values."
  [s]
  (cond
    (string? s) (cs/lower-case s)
    (coll? s)   (map lcase s)
    :else       s))

(defn parse-int
  "Parses a string into an int, expecting \"Inf\" for infinity. A nil is parsed
  as 0 - similar to ruby's `to_i` method."
  [x]
  (cond
    (nil? x) 0
    (or (= "NA" x) (= "Inf" x) (= "Infinity" x)) Integer/MAX_VALUE
    (or (= "-Inf" x) (= "-Infinity" x)) Integer/MIN_VALUE
    (string? x) (if (empty? x) 0 (try
                                   (Integer/parseInt (cs/trim x))
                                   (catch java.lang.NumberFormatException nfe
                                     (infof "Unable to parse '%s' into an integer!" x)
                                     0)))
    (coll? x) (map parse-int x)
    (decimal? x) (int (if (pos? x) (min x Integer/MAX_VALUE) (max x Integer/MIN_VALUE)))
    (float? x) (int (if (pos? x) (min x Integer/MAX_VALUE) (max x Integer/MIN_VALUE)))
    :else x))

(defn git-commit
  "Tries to determine the currently deployed commit by looking for it
  in the name of the jar. Returns nil if it cannot be determined."
  []
  (-> clojure.lang.RT
      .getProtectionDomain
      .getCodeSource
      .getLocation
      .getPath
      java.io.File.
      .getName
      (->> (re-find #"-([a-f0-9]{5,})\.jar"))
      second))

(defn project-version
  "Function to look at the 'project.clj' file and pick out the version of the
  project for use *within* the code itself. This makes it easy to define - or
  report - the version of this project without having to maintain it in
  multiple places."
  []
  (-> "project.clj" slurp read-string (nth 2)))
