(ns neanderthal-1.plot
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
            [tech.v3.datatype.functional :as fun]
            [notespace.api :as notespace]
            [notespace.kinds :as kind]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [neanderthal-1.hanami-extras :as he]))



(def origin (dv 0 0))

(def v1 (dv 1 2))

(def v2 (dv -1 3))

(defn plot-vs
  ([vs] (plot-vs vs nil))
  ([vs {:keys [xscale yscale]}]
   (let [points (->> vs
                     (cons origin)
                     (map-indexed (fn [i v]
                                    {:x (v 0)
                                     :y (v 1)
                                     :c i})))
         segments (->> points
                       (map (fn [point]
                              (merge point
                                     {:x2 (:x 0)
                                      :y2 (:y 0)}))))]
     (-> (hc/xform ht/layer-chart
                   :LAYER [(hc/xform ht/point-chart
                                     :DATA segments
                                     :SIZE 1000
                                     :COLOR "c"
                                     :XSCALE (or xscale hc/RMV)
                                     :YSCALE (or yscale hc/RMV))
                           (hc/xform he/rule-chart
                                     :DATA segments
                                     :SIZE 5
                                     :COLOR "c"
                                     :XSCALE (or xscale hc/RMV)
                                     :YSCALE (or yscale hc/RMV))])
         (kind/override kind/vega)))))


(plot-vs [v1 v2])

(def m1 (dge 2 2))
(copy! (dv 1 0) (row m1 0))
(copy! (dv 0 2) (row m1 1))

m1


(plot-vs [(mv m1 v1)
         (mv m1 v2)])





(defn plot-change [f vs]
  (let [new-vs (mapv f vs)
        all-vs (concat [origin] vs new-vs)
        xdomain (->> all-vs
                     (map #(% 0))
                     ((juxt (partial apply min)
                            (partial apply max))))
        ydomain (->> all-vs
                     (map #(% 1))
                     ((juxt (partial apply min)
                            (partial apply max))))]
      (-> [:div
           [:div {:style {:display "flex"}}
            [:div {:style {:display "inline-block"}}
             [:p/vega
              (plot-vs vs
                       {:xscale {:domain xdomain}
                        :yscale {:domain ydomain}})]]
            [:div {:style {:display "inline-block"
                           :vertical-align "middle"}}
             [:h1 "|--------------->"]]
            [:div {:style {:display "inline-block"}}
             [:p/vega
              (plot-vs new-vs
                       {:xscale {:domain xdomain}
                        :yscale {:domain ydomain}})]]]]
          (kind/override kind/hiccup))))


(plot-change (partial mv m1) [v1 v2])
