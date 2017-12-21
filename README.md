# pusher-clj

[![Clojars Project](https://img.shields.io/clojars/v/com.lorddoig/pusher-clj.svg)](https://clojars.org/com.lorddoig/pusher-clj)

A thin convenience wrapper around the official pusher Java library.

It currently only supports public channels; private channels, presence subscriptions,
and authorization are still TODO.  Contributions welcome.

## Usage

Requires Clojure 1.9.0+ as it uses `spec` for data validation.

The client is constructed with a pusher application key and a single callback
that will handle all messages; this gives flexibility for you to use `core.async`
or whatever other mechanism you like to process messages.  In a nutshell:

```clojure
(let [client (-> (new-client "xxxxxxxxxxxxxxxxx" (fn callback [[msg-type data :as payload]]
                                                     (println payload)))
                   (connect!))]
    ;; callback receives and prints:
    [:pusher-clj.connection/state-transition
     [:pusher-clj.connection/disconnected :pusher-clj.connection/connecting]]
    [:pusher-clj.connection/state-transition
     [:pusher-clj.connection/connecting :pusher-clj.connection/connected]]
    
    
    (subscribe! client "my_channel" ["event1" "event2"])

    ;; callback receives and prints:
    [:pusher-clj.core/subscribed "my_channel"]
    [:pusher-clj.core/msg ["my_channel" "event2" "some-data"]]
    [:pusher-clj.core/msg ["my_channel" "event1" "some-data"]]
    [:pusher-clj.core/msg ["my_channel" "event2" "some-data"]]


    (unsubscribe! client "my_channel" ["event2"])
    
    ;; messages for event2 stop being received:
    [:pusher-clj.core/msg ["my_channel" "event1" "some-data"]]
    [:pusher-clj.core/msg ["my_channel" "event1" "some-data"]]
    
    (disconnect! client)
    
    ;; callback receives and prints the following, after which no more messages
    ;; are received until...
    [:pusher-clj.connection/state-transition
     [:pusher-clj.connection/connected :pusher-clj.connection/disconnecting]]
    [:pusher-clj.connection/state-transition
     [:pusher-clj.connection/disconnecting :pusher-clj.connection/disconnected]]
    
    (connect! client)
    
    ;;...the client is reconnected using `connect!`; subscriptions are preserved:
    [:pusher-clj.connection/state-transition
     [:pusher-clj.connection/disconnected :pusher-clj.connection/connecting]]
    [:pusher-clj.connection/state-transition
     [:pusher-clj.connection/connecting :pusher-clj.connection/connected]]
    [:pusher-clj.core/msg ["my_channel" "event1" "some-data"]]
    [:pusher-clj.core/msg ["my_channel" "event1" "some-data"]]
    )
```

There are various specs defining the shape of the data this library passes around.
You can use them with `s/conform` and friends, but are especially useful as documentation.
Currently, specs are colocated with the functions/data structures with which they
are used, see [core.clj](src/com/lorddoig/pusher_clj/core.clj) for examples.

## Contributing

Make a PR.

## License

Copyright © 2017 Sean Doig

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


