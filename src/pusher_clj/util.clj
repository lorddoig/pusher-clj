(ns pusher-clj.util
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang IExceptionInfo)))

(defmacro condoto
  "Like cond-> but with doto instead of ->"
  [expr & clauses]
  (assert (even? (count clauses)))
  (let [ret (gensym)
        op-f (fn [[texpr op]]
                `(when ~texpr (doto ~ret ~op)))]
    `(let [~ret ~expr]
       ~@(map op-f (partition 2 clauses))
       ~ret)))

(s/fdef nblank?
  :args (s/cat :s string?)
  :ret boolean?)

(defn nblank?
  [s]
  (not (str/blank? s)))

(s/def ::nblank (s/and string? nblank?))

(defn ex-info?
  [x]
  (instance? IExceptionInfo x))

(s/def ::ex-info ex-info?)
