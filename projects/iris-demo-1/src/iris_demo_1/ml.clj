(ns iris-demo-1.ml
  (:require [notespace.api]))

(require '[tablecloth.api :as table]
         '[tech.v3.dataset.modelling :as modelling]
         '[tech.v3.dataset :as dataset]
         '[scicloj.metamorph.core :as morph]
         '[scicloj.metamorph.ml :as morphml]
         '[scicloj.metamorph.ml.loss :as loss]
         '[scicloj.ml.metamorph :as mlmorph]
         '[scicloj.ml.smile.classification]
         '[fastmath.clustering :as clustering])

(defonce iris-dataset
  (-> "https://datahub.io/machine-learning/iris/r/iris.csv"
      (table/dataset {:key-fn keyword})))

(def split
  (-> iris-dataset
      (modelling/train-test-split)))

(def pipeline-1
  (morph/pipeline
   (mlmorph/set-inference-target :class)
   (mlmorph/categorical->number [:class])
   (morphml/model {:model-type :smile.classification/decision-tree
                   :max-nodes 2})))

(def trained-ctx-1
  (pipeline-1
   {:metamorph/mode :fit
    :metamorph/data (:train-ds split)}))

(def predicted-ctx-1
  (pipeline-1
   (merge trained-ctx-1
          {:metamorph/mode :transform
           :metamorph/data (:test-ds split)})))

(morphml/evaluate-pipelines
 [pipeline-1]
 (table/split->seq (:train-ds split)
                   :kfold)
 loss/classification-accuracy
 :accuracy)


(defn cluster [ctx]
  (case (:metamorph/mode ctx)
    :fit (let [id (:metamorph/id ctx)
               clustering (-> ctx
                              :metamorph/data
                              (table/select-columns
                               [:sepallength :sepalwidth :petallength :petalwidth])
                              table/rows
                              (clustering/k-means 3))]
           (assoc ctx id clustering))
    :transform (let [id (:metamorph/id ctx)
                     clustering (ctx id)
                     clusters (-> ctx
                                  :metamorph/data
                                  (table/select-columns
                                   [:sepallength :sepalwidth :petallength :petalwidth])
                                  table/rows
                                  (->> (map
                                        (partial clustering/predict clustering))))]
                 (-> ctx
                     (update :metamorph/data
                             table/add-column :cluster clusters)))))


(defn print-data [ctx]
  (println (:metamorph/data ctx))
  ctx)

(def pipeline-2
  (morph/pipeline
   (mlmorph/set-inference-target :class)
   (mlmorph/categorical->number [:class])
   cluster
   print-data
   (morphml/model {:model-type :smile.classification/decision-tree})))

(def trained-ctx-2
  (pipeline-2
   {:metamorph/mode :fit
    :metamorph/data (:train-ds split)}))

(def predicted-ctx-2
  (pipeline-2
   (merge trained-ctx-2
          {:metamorph/mode :transform
           :metamorph/data (:test-ds split)})))

(->> (morphml/evaluate-pipelines
      [pipeline-1 pipeline-2]
      (table/split->seq (:train-ds split)
                        :kfold
                        {:k 5})
     loss/classification-accuracy
     :accuracy
     {:return-best-pipeline-only        false
      :return-best-crossvalidation-only true})
     (map (fn [cases]
            (->> cases
                 (map (fn [case]
                        (-> case
                            (select-keys [:metric :min :mean :max]))))))))


:ok


