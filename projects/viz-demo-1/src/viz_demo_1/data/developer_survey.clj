(ns viz-demo-1.data.developer-survey
  (:require [tablecloth.api :as table]
            [clojure.string :as string]))

(def raw-2019
  (-> "data/developer_survey_2019/survey_results_public.csv"
      (table/dataset {:key-fn keyword})))

(def processed-2019
  (-> raw-2019
      (table/add-columns
       {:experience (fn [ds]
                      (->> ds
                           :YearsCodePro
                           (map (fn [y]
                                  (case y
                                    nil                  nil
                                    "Less than 1 year"   0.5
                                    "More than 50 years" 51
                                    (Integer/valueOf y))))))
        :salary     (fn [ds]
                      (->> ds
                           :ConvertedComp))
        :languages  (fn [ds]
                      (->> ds
                           :LanguageWorkedWith
                           (map (fn [s]
                                  (or (some-> s
                                              (string/split #";")
                                              set)
                                      #{})))))
        :dev-types  (fn [ds]
                      (->> ds
                           :DevType
                           (map (fn [s]
                                  (or (some-> s
                                              (string/split #";")
                                              set)
                                      #{})))))})
      (table/add-columns
       {:clojure?      (fn [ds]
                         (->> ds
                              :languages
                              (map (fn [languanges-set]
                                     (some? (languanges-set "Clojure"))))))
        :r?            (fn [ds]
                         (->> ds
                              :languages
                              (map (fn [languanges-set]
                                     (some? (languanges-set "R"))))))
        :data-science? (fn [ds]
                         (->> ds
                              :dev-types
                              (map (fn [dev-types-set]
                                     (some? (dev-types-set "Data scientist or machine learning specialist"))))))})
      (table/select-columns
       [:year :salary :experience :clojure? :r? :data-science?])))
