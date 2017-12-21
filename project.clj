(defproject com.lorddoig/pusher-clj "0.1.0-SNAPSHOT"
  :description "A thin convenience wrapper around the official Pusher Java library."
  :url "http://github.com/lorddoig/pusher-clj"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0-RC1"]
                 [com.pusher/pusher-java-client "1.6.0"]]
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]])
