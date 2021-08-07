(ns viz-demo-1.intro
  (:require [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [aerial.hanami.core :as hmi]
            [clojure.java.io :as io]
            [cheshire.core]
            [tablecloth.api :as table]
            [notespace.api :as notespace]
            [notespace.kinds :as kind]
            [scicloj.viz.api :as viz]
            [tech.v3.dataset :as tmd]
            [viz-demo-1.data.developer-survey :as developer-survey]))

(comment
  (notespace/init-with-browser)
  (notespace/listen))


(def dataset1
  (table/dataset {:x [1 2 8 -7]
                  :y [-5 1 4 5]
                  :z ["A" "A" "A" "B"]}))

dataset1

(-> dataset1
    (table/rows :as-maps))


(hc/xform ht/point-chart
          :DATA (-> dataset1
                    (table/rows :as-maps))
          :COLOR "z"
          :SIZE 400)

^kind/vega
(hc/xform ht/point-chart
          :DATA (-> dataset1
                    (table/rows :as-maps))
          :COLOR "z"
          :SIZE 400)

^kind/vega
(hc/xform ht/point-chart
          :DATA (-> dataset1
                    (table/rows :as-maps))
          :X "y"
          :Y "x"
          :COLOR "z"
          :SIZE 400)


;; metadata


(def abcd
  ^{:name "my data structure"
    :date "2021-08-07"}
  [1 5 1 6])

abcd

(meta abcd)

(= abcd [1 5 1 6]) ; equality does not care about metadata

(-> abcd
    (conj 9))

(-> abcd
    (conj 9)
    meta)

(def abcdef
  ^kind/vega {:abcdef 9})

(meta abcdef)



ht/point-chart


^kind/vega
(hc/xform ht/point-chart
          :DATA (-> dataset1
                    (table/rows :as-maps))
          :COLOR "z"
          :SIZE 400)


(viz/viz {:type  ht/point-chart
          :DATA  (-> dataset1
                     (table/rows :as-maps))
          :COLOR "z"
          :SIZE  400})


(-> dataset1
    viz/data
    (viz/type ht/point-chart)
    (viz/color "z")
    (assoc :SIZE 400)
    viz/viz)



developer-survey/processed-2019

(-> developer-survey/processed-2019
    (table/order-by [:salary] :desc))

(-> developer-survey/processed-2019
    (table/order-by [:salary] :asc))

:ok
