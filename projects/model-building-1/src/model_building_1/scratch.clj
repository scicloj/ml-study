(ns model-building-1.scratch
  (:require [notespace.api]))

(require '[tablecloth.api :as table]
         '[tech.v3.dataset.modelling :as modelling]
         '[tech.v3.dataset :as dataset]
         '[scicloj.metamorph.core :as morph]
         '[scicloj.ml.core :as ml]
         '[scicloj.metamorph.ml :as morphml]
         '[scicloj.metamorph.ml.loss :as loss]
         '[scicloj.ml.metamorph :as mlmorph]
         '[scicloj.ml.smile.classification]
         '[fastmath.clustering :as clustering]
         '[fastmath.random :as random]
         '[scicloj.viz.api :as viz]
         '[aerial.hanami.common :as hc]
         '[aerial.hanami.templates :as ht]
         '[notespace.kinds :as kind]
         '[tech.viz.vega])

;; Following R4DS section 24: Model building

(defonce diamonds ; from the R package ggplot2
  (table/dataset "data/diamonds.csv"
                 {:key-fn keyword}))

(defonce flights ; from the R package nycflights13
  (table/dataset "data/flights.csv"
                 {:key-fn keyword}))

;; ggplot(diamonds, aes(cut, price)) + geom_boxplot()

;; Hanami, viz.clj


;; ^kind/hiccup
;; (into [:div]
;;       (for [y-column [;; "cut" "color"
;;                       "clarity"]]
;;         [:div
;;          [:p
;;           [:big y-column]]
;;          [:p/vega
;;           (-> diamonds
;;               (table/select-columns [(keyword y-column) :price])
;;               (viz/data)
;;               (viz/y y-column {:type "nominal"})
;;               (viz/x "price")
;;               (viz/type "boxplot")
;;               (assoc :SIZE 30)
;;               viz/viz)]]))


;; ^kind/hiccup
;; (into [:div]
;;       (for [clarity-value ["I1" "IF"]]
;;         [:div
;;          [:p [:big clarity-value]]
;;          [:p/vega
;;           (-> diamonds
;;               (table/select-columns [:clarity :price])
;;               (table/select-rows (fn [row]
;;                                    (-> row
;;                                        :clarity
;;                                        (= clarity-value))))
;;               :price
;;               (tech.viz.vega/histogram "price"))]]))




;; (-> diamonds
;;     (viz/data)
;;     (viz/x "carat")
;;     (viz/y "price")
;;     (viz/type "point")
;;     viz/viz)



(def rect-chart
  (assoc ht/view-base
         :mark (merge ht/mark-base {:type "rect"})))


(-> diamonds
    (table/select-columns [:carat :price])
    (table/head 3)
    (viz/data)
    (viz/type rect-chart)
    (viz/x "carat" {:XBIN {:maxbins 10}})
    (viz/y "price" {:YBIN {:maxbins 10}})
    (merge {:COLOR {:aggregate "count"
                    :type "quantitative"}})
    viz/viz
    (update-in [:encoding :tooltip]
               conj {:aggregate "count" :type "quantitative"}))



(-> diamonds
    (table/select-columns [:carat :price])
    (table/head 10)
    (viz/data)
    (viz/type ht/bar-chart)
    (viz/x "carat" {:XBIN {:maxbins 10}})
    (viz/y "" {:aggregate "count"
               :type      "quantitative"})
    viz/viz
    (update :encoding
            assoc :y
            {:aggregate "count"}))




