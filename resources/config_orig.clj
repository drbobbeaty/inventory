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
}

