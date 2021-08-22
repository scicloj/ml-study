(ns neanderthal-1.session2
  (:require [uncomplicate.commons.core :refer [with-release]]
            [uncomplicate.fluokitten.core :refer [foldmap]]
            [uncomplicate.clojurecl.core :as opencl]
            [uncomplicate.clojurecuda.core :as cuda]
            [uncomplicate.neanderthal
             [core :refer [dot copy asum copy! row col mv mm rk axpy entry!
                           subvector trans mm! zero
                           scal]]
             [math :refer [cos sin]]
             [vect-math :refer [mul]]
             [native :refer [dv dge fge]]
             [cuda :refer [cuv cuge with-default-engine]]
             [opencl :as cl :refer [clv]]
             [random :refer [rand-uniform!]]]
            [criterium.core :refer :all]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [notespace.api :as notespace]
            [neanderthal-1.plot :as plot]))


(def v1 (dv 1 2))

(def v2 (dv -1 3))

(def v1+v2 (axpy v1 v2))

(def v1*3 (scal 3 v1))


(plot/plot-vs [v1 v2 v1+v2 v1*3])


(defn f1 [v]
  (dv (v 0)
      (* 2 (v 1))))


(f1 (dv 1 2))


;; parallelogram

(plot/plot-change f1 [v1 v2 v1+v2 v1*3])



;; f1 is a linear tranformation

v1

v2

v1+v2


;; f1 is additive:

(f1 (axpy v1
          v2))

(axpy
 (f1 v1)
 (f1 v2))

;; f1 respects scaling

(f1 (scal 3 v1))

(scal 3 (f1 v1))


;; f1 is linear -- additive and respects scaling





(def random-vectors
  (repeatedly
   4
   (fn []
     (dv (rand)
         (rand)))))


random-vectors

(plot/plot-vs random-vectors)


(plot/plot-change f1 random-vectors)


;; nonlinear

(defn f2 [v]
  (dv (v 0)
      (* (v 1) (v 1))))


(plot/plot-change f2 random-vectors)


;;


(defn new-2x2-matrix [a b c d]
  (let [m (dge 2 2)]
    (copy! (dv a b) (row m 0))
    (copy! (dv c d) (row m 1))
    m))

(def m1
  (new-2x2-matrix 1 2 3 4))

m1


(mv m1 (dv 0 0))

(mv m1 (dv 1 0)) ; 1st column of m1

(mv m1 (dv 0 1)) ; 2nd column of m1

(mv m1 (dv 10 100)) ; 10 times 1st column plus 100 times 2nd column




(plot/plot-change (fn [v] (mv m1 v))
                  random-vectors)





(plot/plot-change (fn [v] (mv 
                           (new-2x2-matrix 1 0
                                           0 2)
                           v))
                  random-vectors)


(plot/plot-change (fn [v] (mv 
                           (new-2x2-matrix -3 0
                                           0 2)
                           v))
                  random-vectors)


(defn rot [theta]
  (new-2x2-matrix (cos theta) (- (sin theta))
                  (sin theta) (cos theta)))


(plot/plot-change #(mv (rot 0.2) %)
                  random-vectors)


(defn rescale-y [scale]
  (new-2x2-matrix 1 0
                  0 scale))


(plot/plot-change #(mv (rescale-y 2) %)
                  random-vectors)





(plot/plot-change (comp #(mv (rot 0.2) %)
                        #(mv (rescale-y 2) %))
                  random-vectors)



(plot/plot-change (comp #(mv (rescale-y 2) %)
                        #(mv (rot 0.2) %))
                  random-vectors)

(plot/plot-change #(mv (mm (rescale-y 2)
                           (rot 0.2))
                       %)
                  random-vectors)



(rescale-y 2)


(rot 0.2)


(mm (rescale-y 2)
    (rot 0.2))
