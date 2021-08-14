(ns image-demo-1.session1-1
  (:require [notespace.api :as notespace]
            [tech.v3.libs.buffered-image :as bufimg]))

(require '[notespace.kinds :as kind]
         '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as fun]
         '[tech.v3.tensor :as tensor]
         '[tech.v3.libs.buffered-image :as bufimg]
         '[image-demo-1.helper :as helper])

^kind/hidden
(comment
  (notespace/init-with-browser)
  (notespace/listen))

["Inspired by:
https://realpython.com/numpy-tutorial/#practical-example-2-manipulating-images-with-matplotlib"]

(def original-img
  (bufimg/load "https://files.realpython.com/media/kitty.90952ca484f1.jpg"))

original-img

(def original-tensor
  (tensor/ensure-tensor original-img))

original-tensor

(def original-shape
  (dtype/shape original-tensor))

original-shape

(tensor/compute-tensor original-shape
                       (fn [i j k]
                         (if (= k 0)
                           (original-tensor i j k)
                           0))
                       :uint8)

(let [new-img (bufimg/new-image (original-shape 0)
                                (original-shape 1)
                                :byte-bgr)
      computed-tensor (tensor/compute-tensor
                       original-shape
                       (fn [i j k]
                         (if (= k 0)
                           (original-tensor i j k)
                           0))
                       :uint8)]
  (dtype/copy! computed-tensor
               (tensor/ensure-tensor new-img))
  new-img)








(tensor/compute-tensor [2 3 4]
                       (fn [i j k]
                         (+ i
                            (* 10 j)
                            (* 100 k)))
                       :int32)

(tensor/reduce-axis fun/mean
                    (tensor/compute-tensor [2 3 4]
                                           (fn [i j k]
                                             (+ i
                                                (* 10 j)
                                                (* 100 k)))
                                           :int32)
                    2
                    :int32)


(tensor/reduce-axis fun/mean
                    original-tensor
                    2
                    :uint8)


(let [new-img         (bufimg/new-image (original-shape 0)
                                        (original-shape 1)
                                        :byte-bgr)
      brightness      (tensor/reduce-axis fun/mean
                                          original-tensor
                                          2
                                          :uint8)
      computed-tensor (tensor/compute-tensor
                       original-shape
                       (fn [i j k]
                         (brightness i j))
                       :uint8)]
  (dtype/copy! computed-tensor
               (tensor/ensure-tensor new-img))
  new-img)




(fun/dot-product [1 2 3]
                 [1 10 100])


(fun/dot-product [4 -4 0]
                 [1/3 1/3 1/3])

(fun/dot-product [4 -4 0]
                 [1/2 1/6 1/3])



(defn weighted-mean-brightness-img [weights]
  (let [new-img         (bufimg/new-image (original-shape 0)
                                          (original-shape 1)
                                          :byte-bgr)
        brightness      (tensor/reduce-axis #(fun/dot-product weights %)
                                            original-tensor
                                            2
                                            :uint8)
        computed-tensor (tensor/compute-tensor
                         original-shape
                         (fn [i j k]
                           (brightness i j))
                         :uint8)]
    (dtype/copy! computed-tensor
                 (tensor/ensure-tensor new-img))
    new-img))


(weighted-mean-brightness-img [1 0 0])
(weighted-mean-brightness-img [0 1 0])
(weighted-mean-brightness-img [0 0 1])



(let [new-img         (bufimg/new-image (original-shape 1)
                                        (original-shape 0)
                                        :byte-bgr)
      computed-tensor (tensor/compute-tensor
                       [(original-shape 1)
                        (original-shape 0)
                        3]
                       (fn [i j k]
                         (original-tensor j i k))
                       :uint8)]
  (dtype/copy! computed-tensor
               (tensor/ensure-tensor new-img))
  new-img)


