(ns inventory.google
	"Main namespace for authentication with Google's Identity API"
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-time.coerce :refer [to-timestamp]]
            [clj-time.core :refer [now]]
            [clojure.tools.logging :refer [error warnf info infof]]
            [inventory.config :as cfg]
            [inventory.database :as db]
            [inventory.logging :refer [log-execution-time!]]
            [inventory.util :refer [parse-int]]))

(defn authorized-users
  "This is the list of authorized users. It's easy to imagine this as a
  database call where the names are in a table, and the roles are part of
  that. But in this example, we'll keep it nice and simple."
  [user]
  (let [valid (set (db/query ["select email from users"] :row-fn :email))]
    (valid user)))

(defn authorized?
  "Function to check the auth token provided to be one that was created for
  us, and that it's still valid, and from there, get the GMail address of
  the user. We will than use this and check it against the list of all
  authorized-users and if it's in there, we're OK. Log the two phases as
  it's just nice to see it develop for the user."
  [ahdr]
  (if (and (string? ahdr) (.startsWith ahdr "bearer "))
    (let [tok (.subSequence ahdr 7 (count ahdr))
          tlen (count tok)
          thead (.subSequence tok 0 8)
          ttail (.subSequence tok (- tlen 8) tlen)
          url (format (cfg/google :token-url) tok)
          resp (json/parse-string (:body (http/get url {:accept :json})) true)
          email (:email resp)]
      (if (= (or (:audience resp) "") (cfg/google :client-id))
        (do
          (infof "authenticated: %s..%s as: %s" thead ttail email)
          (when (authorized-users email)
            (infof "authorized: %s -- to edit data" email)
            true))
        (warnf "can't authenticate: %s..%s" thead ttail)))))
