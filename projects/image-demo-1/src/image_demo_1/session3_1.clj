(ns image-demo-1.session3-1
  (:require [notespace.api :as notespace]))

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

(type original-img)

original-img

(dtype/get-datatype original-img)

(dtype/shape original-img)

(bufimg/image-channel-format original-img)

(def original-tensor
  (tensor/ensure-tensor original-img))

original-tensor

(type original-tensor)

(dtype/shape original-tensor)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(def original-shape
  (dtype/shape original-tensor))


original-shape


(-> original-tensor
    (tensor/slice-right 1))


;; the green component:

(def green
  (-> original-tensor
      (tensor/slice-right 1)
      (nth 1)))


(-> original-tensor
    (tensor/slice-right 1)
    (nth 1)
    dtype/shape)


(defn tensor->img [t]
  (let [new-img (bufimg/new-image (original-shape 0)
                                  (original-shape 1)
                                  :byte-bgr)]
    (dtype/copy! t
                 (tensor/ensure-tensor new-img))
    new-img))


(-> original-tensor
    tensor->img)


(defn color-component->tensor-for-img [component color-idx]
  (tensor/compute-tensor original-shape
                         (fn [i j k]
                           (if (= k color-idx)
                             (component i j)
                             0))
                         :uint8))

(-> original-tensor
    (tensor/slice-right 1)
    (nth 1)
    (color-component->tensor-for-img 1)
    tensor->img)


(-> (tensor/compute-tensor original-shape
                           (fn [i j k]
                             (if (> (green i j) 180)
                               0
                               (original-tensor i j k)))
                           :uint8)
    tensor->img)


(let [big-img (bufimg/new-image (* 4 (original-shape 0))
                                (* 4 (original-shape 1))
                                :byte-bgr)
      big-tensor (tensor/ensure-tensor big-img)
      subset1-of-big-tensor (-> big-tensor
                               (tensor/select
                                (range (original-shape 0))
                                (range (original-shape 1))))
      subset2-of-big-tensor (-> big-tensor
                               (tensor/select
                                (range (original-shape 0)
                                       (* 2 (original-shape 0)))
                                (range (original-shape 1)
                                       (* 2 (original-shape 1)))))]
  (dtype/copy! original-tensor subset1-of-big-tensor)
  (dtype/copy! original-tensor subset2-of-big-tensor)
  big-img)


