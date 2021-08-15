(ns image-demo-1.session2
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
  (dtype/->int-array [1 4 1 7 -9 1]))

a1

(def r1
  (dtype/->reader a1))

r1

(r1 4)

(def r2
  (dtype/make-reader :int32
                     10000
                     (* idx idx)))

(r2 12)


(def r3
  (dtype/make-reader :int32
                     100
                     (rand-int 9999)))

(r3 12)

r3

;; This reader is lazy and non-caching.


(def r4
  (dtype/clone r3))

(r4 12)

r4
