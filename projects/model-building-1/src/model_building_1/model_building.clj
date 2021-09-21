(ns model-building-1.model-building
  (:require
   [tablecloth.api :as table]
   [tech.v3.dataset.modelling :as modelling]
   [tech.v3.dataset :as dataset]
   [scicloj.metamorph.core :as morph]
   [scicloj.ml.core :as ml]
   [scicloj.metamorph.ml :as morphml]
   [scicloj.metamorph.ml.loss :as loss]
   [scicloj.ml.metamorph :as mlmorph]
   [scicloj.ml.smile.classification]
   [fastmath.clustering :as clustering]
   [fastmath.random :as random]
   [scicloj.viz.api :as viz]
   [aerial.hanami.common :as hc]
   [aerial.hanami.templates :as ht]
   [tech.viz.vega]
   [scicloj.notespace.v4.api :as notespace.v4]
   [scicloj.notespace.v4.render :as v4.render]
   [tech.v3.datatype.functional :as fun]
   [scicloj.ml.dataset :as ds]
   [tech.v3.dataset.math :as ds-math]
   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.functional :as dfn]
   [scicloj.ml.core :as ml]
   [scicloj.ml.metamorph :as mm]
   [scicloj.metamorph.ml :as mmml]
   [camel-snake-kebab.core :as csk]
   [scicloj.metamorph.ml.loss :as loss]
   [clojure.string :as str]
   [fastmath.stats :as stats]
   [fastmath.random :as rnd]
   [fitdistr.core :as fit]))



(comment
  (notespace.v4/start {}))

;; Following R4DS section 24: Model building

(defonce diamonds ; from the R package ggplot2
  (table/dataset "data/diamonds.csv"
                 {:key-fn keyword}))

(defonce flights ; from the R package nycflights13
  (table/dataset "data/flights.csv"
                 {:key-fn keyword}))

;; ggplot(diamonds, aes(cut, price)) + geom_boxplot()

;; ^kind/hiccup
(v4.render/as-hiccup
 (into [:div]
       (for [y-column [;; "cut" "color"
                       "clarity"]]
         [:div
          [:p
           [:big y-column]]
          [:p/vega
           (-> diamonds
               (table/select-columns [(keyword y-column) :price])
               (viz/data)
               (viz/y y-column {:type "nominal"})
               (viz/x "price")
               (viz/type "boxplot")
               (assoc :SIZE 30)
               viz/viz)]])))


;; ^kind/hiccup
(v4.render/as-hiccup
 (into [:div]
       (for [clarity-value ["I1" "IF"]]
         [:div
          [:p [:big clarity-value]]
          [:p/vega
           (-> diamonds
               (table/select-columns [:clarity :price])
               (table/select-rows (fn [row]
                                    (-> row
                                        :clarity
                                        (= clarity-value))))
               :price
               (tech.viz.vega/histogram "price"))]])))




(v4.render/as-vega
 (-> diamonds
     (viz/data)
     (viz/x "carat")
     (viz/y "price")
     (viz/type "point")
     viz/viz))



(def rect-chart
  (assoc ht/view-base
         :mark (merge ht/mark-base {:type "rect"})))


(v4.render/as-hiccup
 (-> diamonds
     (table/select-columns [:carat :price])
     (viz/data)
     (viz/type rect-chart)
     (viz/x "carat" {:XBIN {:maxbins 50}})
     (viz/y "price" {:YBIN {:maxbins 50}})
     (merge {:COLOR {:aggregate "count"
                     :type      "quantitative"}})
     viz/viz
     (update-in [:encoding :tooltip]
                conj {:aggregate "count" :type "quantitative"})))





(defn ->linear-model [dataset target-column explanatory-columns]
  (let [model (-> dataset
                  (ds/select-columns (conj explanatory-columns target-column))
                  (ds/set-inference-target target-column)
                  (mmml/train {:model-type :smile.regression/ordinary-least-square}))]
    {:model model
     :model-def (-> model
                    :options
                    mmml/options->model-def)}))

(defn explain [{:keys [model model-def]}]
  (mmml/explain model model-def))

(defn println-model [{:keys [model model-def]}]
  (-> model
      (mmml/thaw-model model-def)
      println))


(def processed-diamonds
  (-> diamonds
      (table/add-columns {:log-price (fn [ds]
                                       (-> ds :price fun/log))
                          :log-carat (fn [ds]
                                       (-> ds :carat fun/log))})))


(def linear-model-1
  (-> processed-diamonds
      (->linear-model :log-price [:log-carat])))


(-> linear-model-1
    explain)

(-> linear-model-1
    println-model)


(def processed-diamonds-with-prediction
  (-> processed-diamonds
      (table/add-column :predicted-log-price
                        (fn [ds]
                          (-> ds
                              (mmml/predict (:model linear-model-1))
                              :log-price)))
      (table/add-column :predicted-price
                        (fn [ds]
                          (-> ds
                              :predicted-log-price
                              fun/exp)))))

(-> [:div
     [:h1 (str (java.util.Date.))]
     [:p/vega
      (hc/xform ht/layer-chart
                :LAYER [(-> processed-diamonds
                            (table/select-columns [:log-carat :log-price])
                            (viz/data)
                            (viz/type ht/point-chart)
                            (viz/x "log-carat")
                            (viz/y "log-price")
                            viz/viz)
                        (-> processed-diamonds-with-prediction
                            (table/select-columns [:log-carat :predicted-log-price])
                            (viz/data)
                            (viz/type ht/line-chart)
                            (viz/x "log-carat")
                            (viz/y "predicted-log-price")
                            (assoc :MCOLOR "red")
                            viz/viz)])]]
    v4.render/as-hiccup)





(-> [:div
     [:h1 (str (java.util.Date.))]
     [:p/vega
      (hc/xform ht/layer-chart
                :LAYER [(-> processed-diamonds
                            (table/select-columns [:carat :price])
                            (viz/data)
                            (viz/type ht/point-chart)
                            (viz/x "carat")
                            (viz/y "price")
                            viz/viz)
                        (-> processed-diamonds-with-prediction
                            (table/select-columns [:carat :predicted-price])
                            (viz/data)
                            (viz/type ht/line-chart)
                            (viz/x "carat")
                            (viz/y "predicted-price")
                            (assoc :MCOLOR "red")
                            viz/viz)])]]
    v4.render/as-hiccup)



:ok
