{
 ;; Location of the redis server for development
 :redis {:location "localhost:6379"}

 ;; HipChat auth token and room data for notifications
 :hipchat {:auth-token "abcdefghijklmnopqrstuvwxyz"
           :info-room 1122334455
           :error-room 1122334455}

 ;; just a local database - should it turn out we need it
 :database {:classname "org.postgresql.Driver"
            :subprotocol "postgresql"
            :subname "//localhost/inventory"
            :user "drbob"
            :migrator "drbob"}

 ;; this is how to authenticate with Google's Identity Svc
 :google {:token-url "https://www.googleapis.com/oauth2/v1/tokeninfo?id_token=%s"
          :client-id "357000000000-ababababababababababababababa343.apps.googleusercontent.com"
          :client-secret "abcabcabcabcabcabcabcabc"}
}

