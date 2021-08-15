(ns image-demo-1.session3
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




(def a1
  (dtype/->int-array [1 58 10 18 -1 7 1 17]))

a1

(type a1)


;; int        - primitive type
;; Integer    - boxed type (an Object)


(def r1 ; reader
  (dtype/->reader a1))

r1

(r1 2)

(dtype/elemwise-datatype r1)


(def r2
  (dtype/make-reader :int32 1000 (* idx idx)))

r2

;; lazy, non-caching

(r2 9)

(def r3
  (dtype/clone r2))

r3

;; not lazy, cached

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

[4 2 3]

(def v1
  [[[3 1 4]  [3 18 4]]
   [[31 1 4] [3 182 4]]
   [[3 1 34] [33 18 4]]
   [[3 1 34] [3 18 4]]] )

(def t1
  (tensor/->tensor v1))


t1

(def t2
  (-> v1
      flatten
      dtype/->int-array
      (tensor/reshape [4 2 3])))

t2

(t2 2 1 2)


(first t2)

(rest t2)


(t2 2 1)

(first (t2 2 1))

;; (conj t2 (first t2)) fails


(def t3
  (tensor/compute-tensor [4 2 3]
                         (fn [i j k]
                           (+ i
                              (* 10 j)
                              (* 100 k)))
                         :int32))

t3

;; lazy, non-caching

(def t4
  (dtype/clone t3))

t4

;; not lazy, cached







t3

(-> t3
    (tensor/slice 1))

(-> t3
    (tensor/slice 2))

(-> t3
    (tensor/slice-right 1))
