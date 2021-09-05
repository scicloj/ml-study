(ns iris-demo-1.toy
  (:require [notespace.api]))

(require '[tablecloth.api :as table]
         '[tech.v3.dataset.modelling :as modelling]
         '[tech.v3.dataset :as dataset]
         '[scicloj.metamorph.core :as morph]
         '[scicloj.metamorph.ml :as morphml]
         '[scicloj.metamorph.ml.loss :as loss]
         '[scicloj.ml.core :as ml]
         '[scicloj.ml.dataset :as ds]
         '[scicloj.ml.metamorph :as mlmorph]
         '[scicloj.ml.smile.classification]
         '[fastmath.clustering :as clustering]
         '[tech.v3.datatype.functional :as fun])

(def dataset
  (ds/dataset {:y (repeatedly 1000 rand)}))


(-> dataset
    (ds/head 5))

(def f
  (ml/lift ds/head 5))

(-> {:metamorph/data dataset}
    f)


(def g
  (ml/lift (fn [x]
             (* 10 x))))

(-> {:metamorph/data 99
     :abcd 1234}
    g)

;; {:metamorph/data 990, :abcd 1234}


(defn h [ctx]
  (update ctx
          :metamorph/data
          (fn [x]
            (* 10 x))))

(-> {:metamorph/data 99
     :abcd           1234}
    h)

;; {:metamorph/data 990, :abcd 1234}



(def split
  (-> dataset
      modelling/train-test-split))


(-> split
    :train-ds
    ds/shape)

(-> split
    :test-ds
    ds/shape)


(defn train-model [dataset]
  (-> dataset
      :y
      fun/mean)) ; arithmetic mean

(def model
  (-> split
      :train-ds
      train-model))

(defn predict [dataset model]
  (-> dataset
      (ds/add-column :prediction model)))

(-> split
    :test-ds
    (predict model))


(def toy-pipeline
  (ml/pipeline
   (fn [ctx] (update ctx :abcd * 10))
   (fn [ctx] (update ctx :efgh * 0.1))
   (fn [ctx] (assoc ctx :ijkl 10))))


(-> {:abcd 1234
     :efgh 5678}
    toy-pipeline)


(defn model [ctx]
  )


(def ml
  (ml/pipeline
   (ml/set-inference-target :y)
   (model)))


{:metamorph/data (:train-ds split)
 :metamorph/mode :fit}



