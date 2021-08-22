(ns neanderthal-1.hanami-extras
  (:require [aerial.hanami.templates :as ht]
            [aerial.hanami.common :as hc]))

(def extended-xy-encoding
  (merge
   ht/xy-encoding
   {:x2 {:field     :X2
         :type      :XTYPE
         :bin       :XBIN
         :timeUnit  :XUNIT
         :axis      :XAXIS
         :scale     :XSCALE
         :sort      :XSORT
         :aggregate :XAGG}
    :y2 {:field     :Y2
         :type      :YTYPE
         :bin       :YBIN
         :timeUnit  :YUNIT
         :axis      :YAXIS
         :scale     :YSCALE
         :sort      :YSORT
         :aggregate :YAGG}}))

(swap! hc/_defaults
       assoc
       :X2 "x2"
       :Y2 "y2"
       :ENCODING extended-xy-encoding)

(def rule-chart
  (assoc ht/view-base
         :mark "rule"))
