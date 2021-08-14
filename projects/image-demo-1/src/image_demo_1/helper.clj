(ns image-demo-1.helper
  (:require [notespace.api :as notespace]
            [clojure.java.io :as io]
            [tech.v3.resource :as resource]
            [tech.v3.libs.buffered-image :as bufimg]
            [notespace.behavior]))

(defn show [image-buffer]
  (let [filename (str (rand-int 9999999) ".jpg")
        path     (notespace/file-target-path filename)
        hiccup   (notespace/img-file-tag filename {:width 400})]
    (resource/track hiccup
                    {:track-type :gc
                     :dispose-fn #(.delete ^java.io.File
                                   (io/file path))})
    (bufimg/save! image-buffer path)
    hiccup))

(extend-type java.awt.image.BufferedImage
  notespace.behavior/Behaving
  (->behavior [this]
    {:render-src?   true
     :value->hiccup show}))
