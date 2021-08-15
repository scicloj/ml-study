(ns image-demo-1.session2-1
  (:require [notespace.api :as notespace]
            [tech.v3.libs.buffered-image :as bufimg]))

(require '[notespace.kinds :as kind]
          '[tech.v3.datatype :as dtype]
          '[tech.v3.datatype.functional :as fun]
          '[tech.v3.tensor :as tensor]
          '[tech.v3.libs.buffered-image :as bufimg]
          '[image-demo-1.helper :as helper]
          '[aerial.hanami.common :as hanami-common]
          '[aerial.hanami.templates :as hanami-templates]
          '[tablecloth.api :as table])

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

(def t1
  (tensor/compute-tensor [2 3 4]
                         (fn [i j k]
                           (+ i
                              (* 10 j)
                              (* 100 k)))
                         :int32))

t1


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def original-shape
  (dtype/shape original-tensor))



(let [new-img (bufimg/new-image (original-shape 1)
                                (original-shape 0)
                                :byte-bgr)
      computed-tensor (tensor/compute-tensor
                       [(original-shape 1)
                        (original-shape 0)
                        (original-shape 2)]
                       (fn [j i k]
                         (original-tensor i j k))
                       :uint8)]
  (dtype/copy! computed-tensor
               (tensor/ensure-tensor new-img))
  new-img)




(let [new-img (bufimg/new-image (original-shape 0)
                                (original-shape 1)
                                :byte-bgr)
      computed-tensor (tensor/compute-tensor
                       [(original-shape 0)
                        (original-shape 1)
                        (original-shape 2)]
                       (fn [i j k]
                         (/ (+ (rem i 256)
                               (original-tensor i j k))
                             3))
                       :uint8)]
  (dtype/copy! computed-tensor
               (tensor/ensure-tensor new-img))
  new-img)





(defn recolor [proportion]
  (let [new-img (bufimg/new-image (original-shape 0)
                                  (original-shape 1)
                                  :byte-bgr)
        computed-tensor (tensor/compute-tensor
                         [(original-shape 0)
                          (original-shape 1)
                          (original-shape 2)]
                         (fn [i j k]
                           (* (proportion k)
                              (original-tensor i j k)))
                         :uint8)]
    (dtype/copy! computed-tensor
                 (tensor/ensure-tensor new-img))
    new-img))


(recolor [1 1/2 1/2])
(recolor [1/2 1 1/2])
(recolor [1/2 1/2 1])


t1

original-tensor



(-> [[[0 1 2]  [0 10 2]]
     [[0 1 2]  [0 10 2]]
     [[10 1 2] [0 100 20]]
     [[10 1 2] [0 100 20]]
     [[10 1 2] [0 100 20]]]
    (tensor/slice-right 1)
    (nth 1)
    (#(tensor/reduce-axis fun/mean
                          %
                          0
                          :uint32)))

(defn column-mean [t k]
  (-> t
      (tensor/slice-right 1)
      (nth k)
      (#(tensor/reduce-axis fun/mean
                            %
                            0
                            :uint32))))

(column-mean original-tensor 1)

(defn plot-means [t]
  (-> (hanami-common/xform
       hanami-templates/layer-chart
       :DATA (-> {:x     (range (original-shape 1))
                  :blue  (column-mean t 0)
                  :green (column-mean t 1)
                  :red   (column-mean t 2)}
                 table/dataset
                 (table/rows :as-maps))
       :LAYER (vec
               (for [color ["blue" "green" "red"]]
                 (hanami-common/xform
                  hanami-templates/line-chart
                  :Y color
                  :MCOLOR color))))
      (kind/override kind/vega)))


(plot-means original-tensor)


original-img




(defn darken-left-side [t]
  (tensor/compute-tensor
   [(original-shape 0)
    (original-shape 1)
    (original-shape 2)]
   (fn [i j k]
     (if (< j 400)
       (/ (t i j k)
          2)
       (t i j k)))
   :uint8))

(defn tensor->img [t]
  (let [new-img (bufimg/new-image (original-shape 0)
                                  (original-shape 1)
                                  :byte-bgr)]
    (dtype/copy! t
                 (tensor/ensure-tensor new-img))
    new-img))

(-> original-tensor
    darken-left-side
    tensor->img)

(-> original-tensor
    darken-left-side
    plot-means)
