(ns neanderthal-1.session3
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
