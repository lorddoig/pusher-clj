(ns com.lorddoig.pusher-clj.core
  "A thin convenience wrapper around the official Pusher Java library."
  {:author "Sean Doig <sean@seandoig.com>"}
  (:require [com.lorddoig.pusher-clj.connection :as conn]
            [com.lorddoig.pusher-clj.util :as u]
            [clojure.spec.alpha :as s])
  (:import (com.pusher.client Pusher PusherOptions Authorizer)
           (com.pusher.client.connection ConnectionEventListener
                                         ConnectionStateChange
                                         ConnectionState)
           (com.pusher.client.channel Channel ChannelEventListener)))

(defprotocol IClient
  (-connect! [this])
  (-disconnect! [this])
  (-subscribe! [client channel events])
  (-unsubscribe! [client channel events])
  (-unsubscribe-all! [client channel])
  (-connection-state [client]))

(defrecord Client
  [^Pusher pusher ^ConnectionEventListener conn-listener ^ChannelEventListener ev-listener
   channels !conn-state]
  IClient
  (-connect! [this]
    (.connect pusher conn-listener (into-array ConnectionState [ConnectionState/ALL]))
    this)

  (-disconnect! [this]
    (.disconnect pusher)
    this)

  (-subscribe! [this channel events]
    (if-let [ch ^Channel (get channels channel)]
      (do (.bind ch channel ev-listener)
          this)
      (let [ch (.subscribe pusher channel ev-listener (into-array String events))]
        (update this :channels assoc channel ch))))

  (-unsubscribe! [this channel events]
    (doseq [event events]
      (.unbind ^Channel (get channels channel) event ev-listener))
    this)

  (-unsubscribe-all! [this channel]
    (.unsubscribe pusher channel)
    (update this :channels dissoc channel))

  (-connection-state [_]
    @!conn-state))

(defn client? [x]
  (satisfies? IClient x))

(defn authorizer?
  [x]
  (instance? Authorizer x))

(s/def ::payload
  (s/or :conn-state-transition
        (s/cat :event-id #(= % ::conn/state-transition)
               :transition (s/spec (s/cat :state-before ::conn/state
                                          :state-after ::conn/state)))

        :conn-error
        (s/cat :event-id #(= % ::conn/error)
               :error ::u/ex-info)

        :subscription-confirmation
        (s/cat :event-id #(= % ::subscribed)
               :channel-name ::u/nblank)

        :message
        (s/cat :event-id #(= % ::msg)
               :data (s/spec (s/cat :channel-name ::u/nblank
                                    :event-name ::u/nblank
                                    :data any?)))))

(s/def ::handler
  (s/fspec :args (s/cat :payload ::payload)))

(s/def ::encrypted? boolean?)
(s/def ::authorizer authorizer?)
(s/def ::host ::u/nblank)
(s/def ::ws-port pos-int?)
(s/def ::wss-port pos-int?)
(s/def ::cluster ::u/nblank)
(s/def ::activity-timeout pos-int?)
(s/def ::pong-timeout pos-int?)

(s/def ::options
  (s/keys :opt-un [::encrypted? ::authorizer ::host ::ws-port ::wss-port
                   ::cluster ::activity-timeout ::pong-timeout]))

(s/fdef new-client
  :args (s/cat :app-key ::u/nblank
               :f ::handler
               :opts (s/? ::options))
  :ret client?)

(defn new-client
  "Given an application key and a handler function, and optionally an options map,
  constructs and returns a new client for said application key.

  See the ::options spec or the PusherOptions java docs for available options."
  ([app-key handler] (new-client app-key handler {}))
  ([^String app-key handler opts]
   (let [!conn-state (volatile! ::conn/disconnected)
         conn-listener (proxy [ConnectionEventListener] []
                         (onConnectionStateChange [^ConnectionStateChange change]
                           (let [prev (conn/connection-state->kw (.getPreviousState change))
                                 cur (conn/connection-state->kw (.getCurrentState change))]
                             (vreset! !conn-state cur)
                             (handler [::conn/state-transition [prev cur]])))
                         (onError [^String msg ^String code ^Exception ex]
                           (handler [::conn/error (ex-info msg {:code code} ex)])))
         ev-listener (proxy [ChannelEventListener] []
                       (onSubscriptionSucceeded [^String ch]
                         (handler [::subscribed ch]))
                       (onEvent [^String ch ^String ev ^String data]
                         (handler [::msg [ch ev data]])))
         {:keys [encrypted? authorizer host ws-port
                 wss-port cluster activity-timeout pong-timeout]} opts
         popts (u/condoto (PusherOptions.)
                 encrypted? (.setEncrypted true)
                 authorizer (.setAuthorizer authorizer)
                 host (.setHost host)
                 ws-port (.setWsPort ws-port)
                 wss-port (.setWssPort wss-port)
                 cluster (.setCluster cluster)
                 activity-timeout (.setActivityTimeout activity-timeout)
                 pong-timeout (.setPongTimeout pong-timeout))
         client (Pusher. app-key popts)]
     (Client. client conn-listener ev-listener {} !conn-state))))

(s/fdef connect!
  :args (s/cat :client client?)
  :ret client?)

(defn connect!
  [client]
  (-connect! client))

(s/fdef disconnect!
  :args (s/cat :client client?)
  :ret client?)

(defn disconnect!
  [client]
  (-disconnect! client))

(s/fdef subscribe!
  :args (s/cat :client client?
               :channel ::u/nblank
               :events (s/+ ::u/nblank))
  :ret client?)

(defn subscribe!
  "Subscribe the client to the given events on the given channel.  May be called
  on clients with existing subscriptions."
  [client channel events]
  (-subscribe! client channel events))

(s/fdef unsubscribe!
  :args (s/cat :client client?
               :channel ::u/nblank
               :events (s/? (s/+ ::u/nblank)))
  :fn (fn [{{:keys [channel events]} :args, :keys [ret]}]
        (if (empty? events)
          (false? (contains? (:channels ret) channel))
          true))
  :ret client?)

(defn unsubscribe!
  "Unsubscribes the client from the given events on the given channel.  If events
  are omitted, unsubscribes the client from all events on the channel."
  ([client channel events]
   (-unsubscribe! client channel events))
  ([client channel]
   (-unsubscribe-all! client channel)))

(s/fdef connection-state
  :args (s/cat :client client?)
  :ret ::conn/state)

(defn connection-state
  "Returns the current connection state of the client, see ::conn/state for possible
  options."
  [client]
  (-connection-state client))
