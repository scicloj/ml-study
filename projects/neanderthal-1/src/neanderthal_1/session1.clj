(ns neanderthal-1.session1
  (:require [uncomplicate.commons.core :refer [with-release]]
            [uncomplicate.fluokitten.core :refer [foldmap]]
            [uncomplicate.clojurecl.core :as opencl]
            [uncomplicate.clojurecuda.core :as cuda]
            [uncomplicate.neanderthal
             [core :refer [dot copy asum copy! row col mv mm rk axpy entry!
                           subvector trans mm! zero
                           scal]]
             [vect-math :refer [mul]]
             [native :refer [dv dge fge]]
             [cuda :refer [cuv cuge with-default-engine]]
             [opencl :as cl :refer [clv]]
             [random :refer [rand-uniform!]]]
            [criterium.core :refer :all]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]))



(dv 1 3 1)


(def products {:banana    {:price 1.3 :id :banana}
               :mango     {:price 2.0 :id :mango}
               :pineapple {:price 1.9 :id :pineapple}
               :pears     {:price 1.8 :id :pears}})


(def cart1 {:banana    10
            :pineapple 7
            :pears     3})

(def cart2 {:pineapple 3
            :mango 9})


(defn cart-price [product-db cart]
  (reduce (fn [total [product quantity]]
            (+ total (* (:price (product-db product)) quantity)))
          0
          cart))

(cart-price products cart2)



(reduce +
        (map (partial cart-price products)
             [cart1 cart2]))



(def product-prices [1.3 2.0 1.9 1.8])
(def cart-vec-1 [10 0 7 3])
(def cart-vec-2 [0 9 3 0])


(defn dot-product-vec [xs ys]
  (reduce + (map * xs ys)))

(dot-product-vec product-prices
                 cart-vec-1)


(fun/dot-product product-prices cart-vec-1)
31.699999999999996

(fun/dot-product product-prices cart-vec-2)
23.7

(fun/+ cart-vec-1
       cart-vec-2)
[10 9 10 3]


(def f
  #(fun/dot-product product-prices %))

(f (fun/+ cart-vec-1
          cart-vec-2))
55.4

(+ (f cart-vec-1)
   (f cart-vec-2))
55.39999999999999


;; #(fun/dot-product product-prices %)
;; is additive


(defn g [x]
  (* 9 x))

(g (+ 1 4))
(+ (g 1) (g 4))


(defn g1 [x]
  (+ (* 9 x) 1))


(g1 (+ 1 4))
(+ (g1 1) (g1 4))

(g1 0)

;; linear algebra: g is linear, g1 is "affine"


(defn g2 [x]
  (* x x))

(g2 (+ 1 4))
(+ (g2 1) (g2 4))

;; g2 is "nonlinear"



;; being linear is a little more than being additive

(f [1 10 100 1000])
2011.3

;; we saw f was additive

(f (fun/* 5 [1 10 100 1000]))
10056.5
(* 5 (f [1 10 100 1000]))
10056.5

;; it respects multiplication by a number


;; begin additive and respecting multiplication by a number
;; = being linear



;; f: (vector of 4 numbers) -> (1 number)


;; linear algebra is about linear functions ("linear transformations")
;; from any dimension (4 in our example)
;; to any dimension (1 in our example)

;; matrices give linear transformations

;; all linear transformations in finite dimensions can be seen as matrices

;; composing linear transformations is like mulitplying matrices



(def v1 (dv 1 2))
(def v2 (dv 3 4))

(axpy v1 v2)

(scal 10 v1)


(def m1 (dge 3 2))

m1

(copy! (dv 1 2) (row m1 0))
(copy! (dv 3 4) (row m1 1))
(copy! (dv 5 6) (row m1 2))

m1

(def m2 (trans m1))


v1

(mv m1 v1)

;; v1: 2
;; m1: 3x2
;; m1*v1: 3

;; every element of m1*v1 is a dot product of a row of m1 with v1


(dot (row m1 0) v1)
5.0


m1

m2

(mm m1 m2)

;; every column of m1*m2 is
;; m1*v where v is
;; one of the columns of m2

(col m2 0)

(mv m1 (col m2 0))

(mv m1 (col m2 1))

(mv m1 (col m2 2))


(axpy
 (mv m1 v1)
 (mv m1 v2))

(mv m1 (axpy v1 v2))

;; additive!


(mv m1 (scal 10 v1))

(scal 10 (mv m1 v1))

;; also respects multiplication by scalar
;; so linear!



(mv (mm m2 m1) v1)


(mv m2
 (mv m1
     v1))


;; matrix multiplication is functional composition of linear transformations



