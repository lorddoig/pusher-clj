(ns com.lorddoig.pusher-clj.connection
  {:author "Sean Doig <sean@seandoig.com>"}
  (:require [clojure.spec.alpha :as s])
  (:import (com.pusher.client.connection ConnectionState)))

(def connection-state->kw
  {ConnectionState/ALL ::all
   ConnectionState/CONNECTED ::connected
   ConnectionState/CONNECTING ::connecting
   ConnectionState/DISCONNECTED ::disconnected
   ConnectionState/DISCONNECTING ::disconnecting
   ConnectionState/RECONNECTING ::reconnecting})

(s/def ::state
  (->> connection-state->kw
       vals
       set
       (remove #(= % ::all))
       set))
