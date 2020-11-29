(ns taxi-demo-1.ml
  (:require [notespace.api :as ns]
            [notespace.kinds :as k]
            [tablecloth.api :as t]))

^k/hidden
(comment
  (ns/init-with-browser)
  (ns/init)
  (ns/listen)
  (ns/unlisten)
  (notespace.state/single-note-mode?)
  (ns/toggle-single-note-mode false)
  (ns/toggle-single-note-mode true)
  (ns/render-static-html))

["# Taxi Demo 1"

 "Analysing NYC taxi trip durations, inspired by [Beluga](https://www.kaggle.com/gaborfodor)'s [Kaggle kernel](https://www.kaggle.com/gaborfodor/from-eda-to-the-top-lb-0-367)."

 "## The stack --

We will be mainbly using libraries by Haifeng Li, Tomasz Sulej and Chris Nuernberger.
Here are the main ones (and their depednencies upon each other).

 `smile` (Haifeng) - ML algorithms

 `fastmath` (Tomasz) - misc math algorithms
 ----> `smile`, `apache-commons-math`, etc.

 `dtype-next` (Chris) - fundamentals

 `tech.ml.dataset` (Chris) - dataframe infrastructure
 ----> `dtype-next`

 `tablecloth` (Tomasz) - dataframe grammar
 ----> `tech.ml.dataset`, `dtype-next`

 `tech.ml` (Chris) - ML platform
 ----> `tech.ml.dataset`, `dtype-next`, `smile`

 `tech.viz` (Chris) - visualization (on top of Vega)"]


["## Setup"]

(require '[notespace.api]
         '[notespace.kinds :as k]
         '[tablecloth.api :as t]
         '[tech.v3.dataset :as ds]
         '[tech.v3.dataset.column :as ds-col]
         '[tech.v3.ml :as ml]
         '[tech.v3.ml.loss :as loss]
         '[tech.v3.dataset :as dataset]
         '[tech.v3.datatype.functional :as dfn]
         '[tech.viz.vega :as viz-vega]
         '[fastmath.core :as fm]
         '[fastmath.stats :as fm.stats]
         '[fastmath.clustering :as fm.clustering]
         '[tech.v3.datatype.datetime :as dtype-dt]
         '[tech.v3.datatype.datetime.operations :as dtype-dt-ops]
         '[tech.v3.tensor.color-gradients :as color-gradients]
         '[tech.v3.datatype :as dtype]
         '[clojure.pprint :as pp :refer [pprint]]
         '[tech.v3.dataset.categorical :as categorical]
         '[tech.v3.dataset.modelling :as modelling]
         '[tech.v3.libs.smile.classification]
         '[tech.v3.libs.xgboost]
         '[clojure.string :as string]
         '[clojure.core.memoize :as memoize :refer [memo]]
         ;; '[com.rpl.specter :as specter]
         )


["## Data"]

["We will use the training set from the Kaggle problem, and divide it into training and tests sets for our experiment.

Our task is to predict a trip's duration using our knowledge at the beginning of the trip."]

["This function will help us in giving Clojury names to the columns."]

(defn ->column-name [raw-column-name]
  (-> raw-column-name
      name
      (string/replace #"_" "-")
      keyword))

["For example:"]

(->column-name "passenger_count")

["Now let us read the data."]

^k/void
(defonce original-dataset
  (delay
    (let [path "data/nyc-taxi-trip-duration/train-sample0.05.csv.gz"]
      (println [:reading path])
      (t/dataset path {:key-fn ->column-name}))))

["Let us look into the data. The `:trip-duration` column is the one to be predicted."]

^k/dataset
(delay
  (-> @original-dataset
      (ds/sample 100 {:seed 1})))

["For this learning project, we are removing some columns which are unnecessary (and are not supposed to be known at the time of prediction, which is the beginning of the ride.)"]

(def learning-dataset
  (delay
    (-> @original-dataset
        (ds/sample 10000 {:seed 1})
        (t/drop-columns [:id :dropoff-datetime]))))

(delay
  (-> @learning-dataset
      (t/rows :as-maps)
      (->> (take 9))))

["Splitting to training and test (note that the test set does not contain the column to be predicted):"]

(def split-dataset
  (delay
    (modelling/train-test-split @learning-dataset)))

(def training-dataset
  (delay
    (:train-ds @split-dataset)))

(def test-dataset
  (delay
    (:test-ds @split-dataset)))

(def training-context
  (delay
    {:title     "Training"
     :dataset   @training-dataset
     :training? true}))

(def test-context
  (delay
    {:title     "Test"
     :dataset   @test-dataset
     :training? false}))

["Dimensions:"]

(delay
  {:train (t/shape @training-dataset)
   :test  (t/shape @test-dataset)})

["## Feature engineering

Here we demonstrate just a tiny bit of the useful ways to add informative columns to our dataset."]

["### Drawing points on a map"]

(defn color-gradient [numbers gradient-name]
  (-> numbers
      (color-gradients/colorize gradient-name)
      dtype/->reader
      (->> (partition 3)
           (map (fn [[r g b]]
                  (format "#%02x%02x%02x" r g b))))))

(def draw-on-map
   (fn [dataset
        {:keys [sample-size
                lat long
                popup
                color-fn
                radius]
         :or   {sample-size 1000
                radius 2}}]
     [:div {:style {:width 100}}
      [:link {:rel  "stylesheet"
              :href "https://unpkg.com/leaflet@1.6.0/dist/leaflet.css"}]
      [:p/leaflet
       (into [{:type :view :center [40.730610 -73.935242] :zoom 12 :height 600 :width 700}]
             (-> dataset
                 (ds/sample sample-size {:seed 1})
                 (t/add-or-replace-column :color color-fn)
                 (t/rows :as-maps)
                 (->>
                  (map (fn [{:keys [color]
                             :as   row}]
                         {:type        :circlemarker
                          :center      ((juxt lat long) row)
                          :fillColor   color
                          :color       color
                          :fillOpacity 0.9
                          :weight      0
                          :radius radius
                          :popup       (when popup
                                         (popup row))})))))]]))

^k/hiccup
(delay
  (-> @training-dataset
      (draw-on-map {:lat  :pickup-latitude
                    :long :pickup-longitude})))

["### Clusters of pickup locations"]

(defn compute-pickup-clustering [dataset {:keys [n-clusters]}]
  (smile.math.MathEx/setSeed 988779)
  (-> dataset
      (t/select-columns [:pickup-latitude :pickup-longitude])
      (ds/sample 10000 {:seed 1})
      t/rows
      (fm.clustering/k-means n-clusters)))

^k/dataset
(delay
  (-> @training-dataset
      (compute-pickup-clustering {:n-clusters 10})
      :representatives
      (->> (map (fn [[long lat]]
                  {:repr-latitude  lat
                   :repr-longitude long})))
      t/dataset))

["Clustering:"]

^k/hiccup
(delay
  (-> @training-dataset
      (compute-pickup-clustering {:n-clusters 10})
      :representatives
      (->> (map (fn [[long lat]]
                  {:repr-latitude  lat
                   :repr-longitude long})))
      t/dataset
      (draw-on-map {:lat  :repr-latitude
                    :long :repr-longitude})))


(defn apply-pickup-clustering [dataset pickup-clustering]
  (-> dataset
      (t/add-or-replace-column
       :pickup-cluster
       (fn [ds]
         (-> ds
             (t/select-columns [:pickup-latitude :pickup-longitude])
             t/rows
             (->> (map (fn [row]
                         (fm.clustering/predict pickup-clustering row)))))))))

["Clusters:"]

(defn draw-pickup-clusters [dataset]
  (-> dataset
      (draw-on-map {:lat      :pickup-latitude
                    :long     :pickup-longitude
                    :color-fn #(color-gradient (% :pickup-cluster)
                                               :rainbow)})))

^k/hiccup
(delay
  (let [clustering (compute-pickup-clustering @training-dataset {:n-clusters 10})]
    (-> @training-dataset
        (ds/sample 5000 {:seed 1})
        (apply-pickup-clustering clustering)
        draw-pickup-clusters)))

(def compute-and-apply-pickup-clustering
   (fn [{:keys [dataset training? n-clusters]
         :as   context}]
     (let [pickup-clustering (if training?
                               (compute-pickup-clustering dataset {:n-clusters n-clusters})
                               (:pickup-clustering context))]
       (assoc
        context
        :dataset (-> dataset
                     (apply-pickup-clustering pickup-clustering))
        :pickup-clustering pickup-clustering))))

(def training-context-1
  (delay
    (-> @training-context
        (merge {:n-clusters 10})
        compute-and-apply-pickup-clustering)))

(def test-context-1
  (delay
    (-> @test-context
        (merge (select-keys @training-context-1
                            [:pickup-clustering]))
        compute-and-apply-pickup-clustering)))

^k/hiccup
(delay
  (->> [@training-context-1 @test-context-1]
       (map (fn [context]
              [:div
               [:h1 (:title context)]
               (-> context
                   :dataset
                   draw-pickup-clusters)]))
       (into [:div])))

["### More features"]

(def haversine-kilometers-multiplier 6371)
(def haversine-miles-multiplier 3958.756)

(defn add-distance-feature [dataset]
  (-> dataset
      (t/add-or-replace-columns
       ;; https://stackoverflow.com/a/26393502https://stackoverflow.com/a/26393502fduu
       {:distance           #(dtype/->double-array
                              (map (comp (partial * haversine-kilometers-multiplier)
                                         fm/haversine-dist)
                                   (% :pickup-latitude)
                                   (% :pickup-longitude)
                                   (% :dropoff-latitude)
                                   (% :dropoff-longitude)))})))

(defn one-hot-encode-pickup-clusters [dataset n-clusters]
  (let [[n _] (t/shape dataset)]
    (-> dataset
        (t/add-or-replace-columns
         {:pickup-cluster-str #(map str (:pickup-cluster %))})
        (t/add-or-replace-columns
         (->> (range n-clusters)
              (map (fn [i]
                     [(keyword (str "pickup-cluster-str-" i))
                      (fn [_]
                        (repeat n 0.0))]))
              (into {})))
        (ds/categorical->one-hot [:pickup-cluster-str])
        (t/drop-columns [:pickup-cluster-str]))))

(def preprocess
   (fn [{:keys [dataset training?]
         :as   context}]
     (merge
      context
      {:dataset
       (-> dataset
           add-distance-feature
           (one-hot-encode-pickup-clusters (:n-clusters context))
           (t/drop-columns [:pickup-datetime :store-and-fwd-flag]))})))

(def training-context-2
  (delay
    (-> @training-context-1
        preprocess)))

(def test-context-2
  (delay
    (-> @test-context-1
        preprocess)))

^k/dataset
(delay
  (-> @training-context-2
      :dataset
      (ds/sample 100 {:seed 1})))

 ^k/hiccup
(delay
  [:div
   [:p/vega
    (-> @training-context-2
        :dataset
        (t/column :trip-duration)
        (viz-vega/histogram "trip duration"
                            {:bin-count 100}))]
   [:p/vega
    (-> @training-context-2
        :dataset
        (t/column :trip-duration)
        dfn/log1p
        (viz-vega/histogram "log(trip duration)"
                            {:bin-count 100}))]])

^k/hiccup
(delay
  [:div
   [:p/vega
    (-> @training-context-2
        :dataset
        (t/column :distance)
        (viz-vega/histogram "distance"
                            {:bin-count 100}))]
   [:p/vega
    (-> @training-context-2
        :dataset
        (t/column :distance)
        dfn/log1p
        (viz-vega/histogram "log(distance)"
                            {:bin-count 100}))]])

^k/vega
(delay
  (-> @training-context-2
      :dataset
      (t/select-columns [:distance :trip-duration])
      (ds/sample 10000 {:seed 1})
      (t/rows :as-maps)
      (viz-vega/scatterplot
       :distance :trip-duration)))

^k/vega
(delay
  (-> @training-context-2
      :dataset
      (t/select-columns [:distance :trip-duration])
      (ds/sample 10000 {:seed 1})
      (t/select-rows #(<= (:trip-duration %)
                          7000))
      (t/rows :as-maps)
      (viz-vega/scatterplot
       :distance :trip-duration)))

["## Training & Prediction"]

(def target :trip-duration)

(defn compute-model [dataset]
  (-> dataset
      (ml/train {:model-type :xgboost/regression
                 :accuracy   0.01})))

(def compute-and-apply-model
   (fn [{:keys [dataset training?]
         :as   context}]
     (let [model               (if training?
                                 (-> dataset
                                     (modelling/set-inference-target target)
                                     (compute-model))
                                 (:model context))]
       (assoc
        context
        :predicted (-> dataset
                       (ml/predict model)
                       (t/column target))
        :model model))))

(def training-context-3
  (delay
    (-> @training-context-2
        compute-and-apply-model)))

(def test-context-3
  (delay
    (-> @test-context-2
        (merge (select-keys @training-context-3
                            [:model]))
        compute-and-apply-model)))

["## Evaluation"]

(defn evaluate [{:keys [title dataset predicted]
                 :as context}]
  (let [actual (t/column dataset target)]
    [:div
     [:h1 title]
     [:h2 "Loss: " (loss/mae predicted actual)]
     [:p/vega
      (-> {:log-predicted (dfn/log predicted)
           :log-actual    (dfn/log actual)
           ;; :cluster (t/column dataset :pickup-cluster)
           }
          t/dataset
          (t/rows :as-maps)
          (viz-vega/scatterplot :log-predicted
                                :log-actual
                                ;; {:label-key :cluster}
                                ))]]))

^k/hiccup
(delay
  (->> [@training-context-3 @test-context-3]
       (map evaluate)
       (into [:div])))

["Comparing different flows"]

(defn pipeline [context hyperparams]
  (-> context
      (merge hyperparams)
      compute-and-apply-pickup-clustering
      preprocess
      compute-and-apply-model))

(defn compute-final-contexts [initial-contexts
                              hyperparams]
  (let [final-training-context (-> initial-contexts
                                   :training
                                   (pipeline hyperparams))
        final-test-context     (-> initial-contexts
                                   :test
                                   (merge (select-keys final-training-context
                                                       [:pickup-clustering
                                                        :model]))
                                   (pipeline hyperparams))]
    {:training final-training-context
     :test     final-test-context}))

(defn evaluate-final-context [{:keys [title dataset predicted]
                               :as   context}]
  (let [actual (t/column dataset target)]
    [:div
     [:h1 title]
     [:h2 "Loss: " (loss/mae predicted actual)]
     [:p/vega
      (-> {:log-predicted (dfn/log predicted)
           :log-actual    (dfn/log actual)
           ;; :cluster (t/column dataset :pickup-cluster)
           }
          t/dataset
          (t/rows :as-maps)
          (viz-vega/scatterplot :log-predicted
                                :log-actual
                                ;; {:label-key :cluster}
                                ))]]))

(def hyperparams-sets
  (for [n-clusters [3 10 100]]
    {:n-clusters n-clusters}))

^k/dataset
(t/dataset hyperparams-sets)

^k/hiccup
(delay
  (->> hyperparams-sets
       (map (fn [hyperparams]
              (let [final-contexts (compute-final-contexts
                                    {:training @training-context
                                     :test     @test-context}
                                    hyperparams)]
                (->> [:training :test]
                     (map final-contexts)
                     (map evaluate-final-context)
                     (into [:div
                            [:h1 (pr-str hyperparams)]])))))
       (into [:div])))

[:done]
