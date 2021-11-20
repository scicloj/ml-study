(ns iris-demo-1.synthetic
  (:require [notespace.api]))

(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mlmorph]
         '[scicloj.ml.dataset :as ds]

         '[scicloj.ml.smile.classification]

         '[fastmath.clustering :as clustering]
         '[fastmath.random :as random]

         '[scicloj.viz.api :as viz]
         '[aerial.hanami.common :as hc]
         '[aerial.hanami.templates :as ht]
         '[notespace.kinds :as kind])


(defn random-points [center-x center-y n rng]
  {:x (repeatedly n #(random/grandom rng center-x 1))
   :y (repeatedly n #(random/grandom rng center-y 1))})


(def centers
  [[-5 20]
   [3 9]
   [0 11]
   [4 19]
   [2 -16]
   [4 13]
   [-3 2]
   [-6 6]])


(def dataset-1
  (let [rng (random/rng :isaac 1337)]
    (-> (->> centers
             (map-indexed (fn [i center]
                            (-> (random-points (center 0)
                                               (center 1)
                                               100
                                               rng)
                                (ds/dataset)
                                (ds/add-column :class i))))
             (apply ds/bind))
        (ds/random {:seed 1}))))

(-> dataset-1
    viz/data
    (viz/type "point")
    (viz/color "class")
    viz/viz)

(def split
  (-> dataset-1
      (ds/train-test-split)))

(def pipeline-1
  (ml/pipeline
   (mlmorph/set-inference-target :class)
   (mlmorph/categorical->number [:class])
   (mlmorph/model {:model-type :smile.classification/decision-tree
                   :max-nodes  5})))

(def trained-ctx-1
  (pipeline-1
   {:metamorph/mode :fit
    :metamorph/data (:train-ds split)}))

(def predicted-ctx-1
  (pipeline-1
   (merge trained-ctx-1
          {:metamorph/mode :transform
           :metamorph/data (:test-ds split)})))






(-> (hc/xform ht/layer-chart
              :LAYER [(-> split
                          :train-ds
                          viz/data
                          (viz/type "point")
                          (viz/color "class")
                          viz/viz)
                      (-> split
                          :train-ds
                          (ds/select-columns [:x :y])
                          ds/rows
                          (clustering/k-means 8)
                          :representatives
                          (->> (map #(zipmap [:x :y] %)))
                          ds/dataset
                          viz/data
                          (viz/type "point")
                          (assoc :SIZE 200
                                 :MCOLOR "black")
                          viz/viz)])
    (kind/override kind/vega))


(defn cluster [ctx]
  (let [mode (:metamorph/mode ctx)
        id (:metamorph/id ctx)
        clustering (case mode
                     :fit (-> ctx
                              :metamorph/data
                              (ds/select-columns
                               [:x :y])
                              ds/rows
                              (clustering/k-means 8))
                     :transform (ctx id))
        clusters (-> ctx
                     :metamorph/data
                     (ds/select-columns
                      [:x :y])
                     ds/rows
                     (->> (map
                           (partial clustering/predict clustering))))]
    (cond-> ctx
      (= mode :fit) (assoc id clustering)
      true          (update :metamorph/data
                            ds/add-column :cluster clusters))))


(def pipeline-2
  (ml/pipeline
   (mlmorph/set-inference-target :class)
   (mlmorph/categorical->number [:class])
   cluster
   (mlmorph/model {:model-type :smile.classification/decision-tree
                   :max-nodes  5})))



(def trained-ctx-2
  (pipeline-2
   {:metamorph/mode :fit
    :metamorph/data (:train-ds split)}))

(def predicted-ctx-2
  (pipeline-2
   (merge trained-ctx-2
          {:metamorph/mode :transform
           :metamorph/data (:test-ds split)})))


(-> predicted-ctx-1
    :scicloj.metamorph.ml/feature-ds
    (ds/add-column :predicted-class (-> predicted-ctx-1
                                        :metamorph/data
                                        :class))
    viz/data
    (viz/type "point")
    (viz/color "predicted-class")
    viz/viz)

(-> predicted-ctx-2
    :scicloj.metamorph.ml/feature-ds
    (ds/add-column :predicted-class (-> predicted-ctx-2
                                        :metamorph/data
                                        :class))
    viz/data
    (viz/type "point")
    (viz/color "predicted-class")
    viz/viz)

(println
 (->> (ml/evaluate-pipelines
       [pipeline-1 pipeline-2]
       (ds/split->seq (:train-ds split)
                      :kfold
                      {:k 20})

       ml/classification-accuracy
       :accuracy
       {:return-best-pipeline-only        false
        :return-best-crossvalidation-only true})
      (map (fn [cases]
             (->> cases
                  (map (fn [case]
                         (-> case
                             (select-keys [:metric :min :mean :max])))))))))










(defn make-pipeline-fn [{:keys [model-type max-nodes cluster?]
                         :as options}]
  (ml/pipeline
   (fn [ctx]
     (assoc ctx :options options))
   (mlmorph/set-inference-target :class)
   (mlmorph/categorical->number [:class])
   (if cluster? cluster identity)
   (mlmorph/model {:model-type model-type
                   :max-nodes  max-nodes
                   :cluster? cluster})))


(def search-grid
  (for [model-type [:smile.classification/decision-tree]
        max-nodes [2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 32768]  
        cluster?  [false true]]
    {:model-type model-type
     :max-nodes max-nodes
     :cluster? cluster?}))


(def pipeline-fns (map make-pipeline-fn search-grid))

(def train-val-splits
  (ds/split->seq (:train-ds split)
                 :kfold
                 {:k 10}))

(def evaluations
  (ml/evaluate-pipelines pipeline-fns
                         train-val-splits
                         ml/classification-accuracy
                         :accuracy
                         {:return-best-pipeline-only        false
                          :return-best-crossvalidation-only true
                          :map-fn            :pmap
                          :result-dissoc-seq []}))

(println
 (->> evaluations
      flatten
      (map (fn [evaluation]
             (merge (select-keys  evaluation [:mean :metric])
                    (-> evaluation :fit-ctx :options))))
      (sort-by :metric)
      reverse
      ds/dataset))
