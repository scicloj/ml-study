(ns image-demo-1.session1
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;





;; a reader -- an abstract notion of an array tha can only be read


(def x
  (dtype/make-reader :int64 9999 (* idx idx)))

x

(def y
  (dtype/->reader [1 54 1 5] :int64))

y

(dtype/elemwise-datatype x)

(dtype/elemwise-datatype y)

(def z
  (dtype/->int-array [1 5 1 51 185 10]))

z

(type z)


(def w
  (-> z
      (fun/* 1000)
      (fun/+ (rand-int 10))
      dtype/clone))

w


;; readers are lazy, non-caching by default

;; clone makes it actually remember

;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def t
  (tensor/compute-tensor [2 3 4000]
                         (fn [i j k]
                           (+ (rand)
                              i
                              (* 10 j)
                              (* 100 k)))
                         :float32))

t




