(ns model-building-1.flights
  (:require
   [tablecloth.api :as table]
   [tech.v3.dataset.modelling :as modelling]
   [tech.v3.dataset :as dataset]
   [scicloj.metamorph.core :as morph]
   [scicloj.ml.core :as ml]
   [scicloj.metamorph.ml :as morphml]
   [scicloj.metamorph.ml.loss :as loss]
   [scicloj.ml.metamorph :as mlmorph]
   [fastmath.random :as random]
   [scicloj.viz.api :as viz]
   [aerial.hanami.common :as hc]
   [aerial.hanami.templates :as ht]
   [tech.viz.vega]
   [scicloj.notespace.v4.api :as notespace]
   [tech.v3.datatype.functional :as fun]
   [scicloj.ml.dataset :as ds]
   [tech.v3.dataset.math :as ds-math]
   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype.datetime :as datetime]
   [scicloj.ml.core :as ml]
   [scicloj.ml.metamorph :as mm]
   [scicloj.metamorph.ml :as mmml]
   [camel-snake-kebab.core :as csk]
   [scicloj.metamorph.ml.loss :as loss]
   [clojure.string :as str]
   [fastmath.stats :as stats]
   [fastmath.random :as rnd]
   [fitdistr.core :as fit]
   [scicloj.tempfiles.api :as tempfiles]
   [gorilla-notes.core :as gn]
   [scicloj.kindly.kind :as kind]
   [scicloj.kindly.api :as kindly])
  (:import java.util.Date
           java.time.LocalDate))


(comment
  (notespace/restart! {})
  (tempfiles/cleanup-session-tempdir!)
  (notespace/restart-events!)
  (notespace/render-as-html! "docs/model-building-1/flights/index.html"))


;; # Following R4DS section 24: Model building

(defonce flights ; from the R package nycflights13
  (table/dataset "data/flights.csv"
                 {:key-fn keyword}))

flights

;; (defn dataset-as-url [dataset]
;;   (let [tempfile (tempfiles/tempfile! ".csv")]
;;     (table/write! dataset (:path tempfile))
;;     (:route tempfile)))


(defonce processed-flights
  (-> flights
      (table/add-column
       :date (fn [ds]
               (map (fn [y m d]
                      (LocalDate/of y m d))
                    (:year ds)
                    (:month ds)
                    (:day ds))))))


(def daily
  (-> processed-flights
      (table/group-by [:date])
      (table/aggregate {:n table/row-count})))

daily


(-> daily
    viz/data
    (viz/type ht/line-chart)
    (viz/x "date" {:type "temporal"})
    (viz/y "n")
    viz/viz
    (assoc-in [:encoding :y :scale] {:zero false}))

(def int->dow
  ["" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])

(def dow->int
  (->> int->dow
       (map-indexed (fn [i dow]
                      [i dow]))
       (into {})))

(def daily-with-dow
  (-> daily
      (table/add-column
       :day-of-week
       (fn [ds]
         (->> ds
              :date
              (datetime/long-temporal-field :day-of-week)
              (map int->dow))))))

(def dow-means
  (-> daily-with-dow
      (table/group-by [:day-of-week])
      (table/aggregate {:mean-n (fn [ds]
                                  (-> ds
                                      :n
                                      fun/mean))})))

(def dow-n-boxplot
  (-> daily-with-dow
      (table/order-by [:day-of-week-int])
      viz/data
      (viz/type "boxplot")
      (viz/x "day-of-week" {:type "ordinal"})
      (viz/y "n")
      viz/viz
      (assoc-in [:encoding :y :scale] {:zero false})
      (assoc-in [:encoding :x :sort] int->dow)))

(def dow-means-point-chart
  (-> dow-means
      viz/data
      (viz/type "point")
      (viz/x "day-of-week" {:type "ordinal"})
      (viz/y "mean-n")
      (assoc :MCOLOR "red"
             :MSIZE 300)
      viz/viz
      (assoc-in [:encoding :y :scale] {:zero false})
      (assoc-in [:encoding :x :sort] int->dow)))


(-> (hc/xform ht/layer-chart
              :LAYER [dow-n-boxplot
                      dow-means-point-chart])
    (kindly/consider kind/vega))


(-> daily-with-dow
    (dataset/categorical->one-hot [:day-of-week]))


(def dow-model
  (-> daily-with-dow
      (dataset/categorical->one-hot [:day-of-week])
      (table/drop-columns [:date :day-of-week-Mon])
      (ds/set-inference-target :n)
      (mmml/train {:model-type
                   :smile.regression/ordinary-least-square})))

(mmml/explain dow-model)

(def data-for-prediction
  (-> {:day-of-week (rest int->dow)} ; using rest to remove ""
      (table/dataset)
      (dataset/categorical->one-hot [:day-of-week])))

(def dow-predictions
  (-> data-for-prediction
      (mmml/predict dow-model)
      (table/add-column :day-of-week (rest int->dow))))

(table/left-join dow-predictions
                 dow-means
                 [:day-of-week])


(def dow-with-residuals
  (-> daily-with-dow
      (table/add-column :predicted-n
                        (fn [ds]
                          (-> ds
                              (dataset/categorical->one-hot [:day-of-week])
                              (mmml/predict dow-model)
                              :n)))
      (table/add-column :residual
                        (fn [ds]
                          (fun/-
                           (:n ds)
                           (:predicted-n ds))))))



(-> dow-with-residuals
    viz/data
    (viz/type ht/line-chart)
    (viz/x "date" {:type "temporal"})
    (viz/y "residual")
    (viz/color "day-of-week")
    viz/viz)

(-> dow-with-residuals
    (table/select-rows (-> dow-with-residuals
                           :residual
                           (fun/< -100))))



(def residuals-plot
  (-> dow-with-residuals
      viz/data
      (viz/type ht/line-chart)
      (viz/x "date" {:type "temporal"})
      (viz/y "residual")
      viz/viz))



:ok
