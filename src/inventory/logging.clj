(ns inventory.logging
  "Logging utilities."
  (:require [clojure.tools.logging :refer [log]]
            [robert.hooke :refer [add-hook]]))

(defn now [] (System/currentTimeMillis))

(defn execution-time-logging-hook
  "Given a config map, returns a hook function that logs execution time."
  [{:keys [level func-name msg msg-fn ns] :or {level :info}}]
  (let [labeler (fn [msg]
                  (str func-name (if msg (str " [" msg "]"))))
        logf (fn [s & args]
               (log ns level nil (apply format s args)))]
    (fn [func & args]
      (let [start (now)]
        (try
          (let [ret (apply func args)
                time-taken (- (now) start)
                label (labeler
                       (cond msg msg
                             msg-fn (try (apply msg-fn ret args)
                                         (catch Throwable t (str "msg-fn error! " t)))
                             :else nil))]
            (logf "Finished %s in %dms." label time-taken)
            ret)
          (catch Throwable t
            (let [time-taken (- (now) start)]
              (logf "Error in %s after %dms (%s)." (labeler nil) time-taken (.getMessage t)))
            (throw t)))))))

(defmacro log-execution-time!
  "A macro for adding execution time logging to a named
  function. Simply call at the top level with the name of the function
  you want to wrap. As a second argument you may provide an options
  map with possible values:

    {
     :level  ;; defaults to :info
     :msg    ;; some string that is printed with the log messages
     :msg-fn ;; a function that will be called with the return value
             ;; and the arguments, and should return a message for
             ;; inclusion in the log
    }"
  ([var-name] `(log-execution-time! ~var-name {}))
  ([var-name opts]
     `(add-hook (var ~var-name)
                ::execution-time
                (execution-time-logging-hook
                 (assoc ~opts
                   :func-name '~var-name
                   ;; pass in the namespace so the log messages
                   ;; can have the appropriate namespace instead
                   ;; of deal-performance.logging
                   :ns ~*ns*)))))
