(ns taxi-demo-1.geoprocessing-1
  (:require [notespace.api]
            [clojure.walk :as walk]))

["# Taxi spatio-temporal data processing example - version 1"]

["## Setup"]

(require '[notespace.kinds :as kind]
         '[tablecloth.api :as tablecloth]
         '[tech.v3
           [dataset :as dataset]
           [datatype :as dtype]]
         '[tech.v3.datatype
          [functional :as dtype-fun]
           [datetime :as datetime]]
         '[tech.viz.vega :as viz]
         '[clojure.string :as string]
         '[jsonista.core :as jsonista]
         '[geo
           [geohash :as geohash]
           [jts :as jts]
           [spatial :as spatial]
           [io :as geoio]
           [crs :as crs]]
         '[gorilla-notes.components.leaflet.providers :as leaflet-providers]
         '[com.rpl.specter :as specter]
         '[plumbing.core :refer [fnk]])

(import (org.locationtech.jts.index.strtree STRtree)
        (org.locationtech.jts.geom Geometry Point Polygon Coordinate)
        (org.locationtech.jts.geom.prep PreparedGeometry
                                        PreparedLineString
                                        PreparedPolygon
                                        PreparedGeometryFactory)
        (java.util TreeMap))

["## GIS helper functions"]


["### Coordinate reference system transformation"]

;; https://epsg.io/4326
(def wgs84-crs (crs/create-crs 4326))

;; https://epsg.io/6539
;; Center coordinates
;; 1252460.55256480 263192.27520000
;; Projected bounds:
;; 911908.37699881 110617.87078182
;; 1588713.88595833 420506.52709693
(def nad83-2011-crs (crs/create-crs 6539))

(def crs-transform
  (crs/create-transform wgs84-crs nad83-2011-crs))

(defn wgs84->nad83-2011
  "Transforming latitude-longitude coordinates
  to local Eucledian coordinates around NYC."
 [geometry]
  (jts/transform-geom geometry crs-transform))

["## Data"]

["### Neighbourhoods"]

(def get-neighbourhoods-data
  (memoize
   (fn []
     (-> "data/nyc-taxi-trip-duration/nyc-neighbourhoods.json"
         slurp
         geoio/read-geojson))))

(delay
  (+ 1 2))

(delay
  (->> (get-neighbourhoods-data)
       (take 4)))

(defn polygon->latlngs
  "Convert a polygon to a sequence of longitude-latitude pairs, assuming it is in the appropriate coordinate reference system."
  [polygon]
  (->> polygon
       jts/coordinates
       (map (juxt spatial/latitude
                  spatial/longitude))))


(delay
  (->> (get-neighbourhoods-data)
       first
       :geometry
       polygon->latlngs))

^kind/hiccup
(delay
  [:p/leafletmap
   {:tile-layer leaflet-providers/Stamen-TonerLite}
   (into [{:type   :view
           :center [40.74242020 -73.98036957]
           :zoom   10
           :height 600
           :width  700}]
         (->> (get-neighbourhoods-data)
              (map (fn [neigh]
                     {:type      :polygon
                      :positions (-> neigh
                                     :geometry polygon->latlngs)
                      :color     :purple
                      :opacity 0.5}))))])


(def get-neighbourhoods
  (memoize
   (fn []
     (-> (get-neighbourhoods-data)
         (->> (map (fn [{:keys [geometry properties]}]
                     (assoc properties :geometry geometry))))
         tablecloth/dataset
         (tablecloth/rename-columns {(keyword "@id") :id})
         (tablecloth/add-or-replace-column
          :geometry #(map wgs84->nad83-2011
                          (:geometry %)))
         (vary-meta assoc :print-column-max-width 100)))))

^kind/dataset
(delay
  (get-neighbourhoods))

["### Taxi rides"]

(defn ->column-name [raw-column-name]
  (-> raw-column-name
      name
      (string/replace #"_" "-")
      keyword))

(->column-name "passenger_count")

(defonce get-taxi-data
  (memoize
   (fn []
     (let [path "data/nyc-taxi-trip-duration/train-sample0.05.csv.gz"
           _ (println [:reading path])
           data (tablecloth/dataset path {:key-fn    ->column-name
                                          :parser-fn {:pickup-datetime  [:local-date-time "yyyy-MM-dd HH:mm:ss"]
                                                      :dropoff-datetime [:local-date-time "yyyy-MM-dd HH:mm:ss"]}})]
      (println [:done-reading]) 
      data))))

^kind/dataset
(delay
  (get-taxi-data))

["## Adding geometries"]

(defn nad83-2011-point [latitude longitude]
  (-> (jts/point latitude longitude)
      wgs84->nad83-2011))

(defn distance [^Geometry g1 ^Geometry g2]
  (.distance g1 g2))

(defn add-taxi-geometries [taxi-data]
  (-> taxi-data
      (tablecloth/add-or-replace-columns
       {:pickup-location  #(map nad83-2011-point
                                (:pickup-latitude %)
                                (:pickup-longitude %))
        :dropoff-location #(map nad83-2011-point
                                (:dropoff-latitude %)
                                (:dropoff-longitude %))})
      (tablecloth/add-or-replace-column
       :distance  #(map distance
                        (:pickup-location %)
                        (:dropoff-location %)))))

^kind/dataset
(delay
  (-> (get-taxi-data)
      add-taxi-geometries))

(delay
  (-> (get-taxi-data)
      add-taxi-geometries
      :distance
      dtype-fun/mean))

["## Visualizing trips"]

(defn draw-trips-as-lines [ds]
  ^kind/hiccup
  [:p/leafletmap
   {:tile-layer leaflet-providers/Stamen-TonerLite}
   (into [{:type   :view
           :center [40.74242020 -73.98036957]
           :zoom   10
           :height 600
           :width  700}]
         (-> ds
             (tablecloth/rows :as-maps)
             (->> (map
                   (fn [{:keys [pickup-longitude
                                pickup-latitude
                                dropoff-longitude
                                dropoff-latitude]}]
                     {:type      :line
                      :positions [[pickup-latitude pickup-longitude]
                                  [dropoff-latitude dropoff-longitude]]
                      :color     :orange})))))])

["A random sample of trips"]

^kind/hiccup
(delay
  (-> (get-taxi-data)
      (dataset/sample 200 {:seed 1})
      add-taxi-geometries
      draw-trips-as-lines))

["The trips of longest duration"]

^kind/hiccup
(delay
  (-> (get-taxi-data)
      add-taxi-geometries
      (tablecloth/order-by :trip-duration :desc)
      (tablecloth/head 200)
      draw-trips-as-lines))

["The trips of longest distance"]

^kind/hiccup
(delay
  (-> (get-taxi-data)
      add-taxi-geometries
      (tablecloth/order-by :distance :desc)
      (tablecloth/head 5)
      draw-trips-as-lines))

["## Working with nested maps"]

(require '[clojure.walk :as walk])

(defn apply-in
  [ctx form]
  (->> form
       (walk/postwalk (fn [subform]
                        (if (vector? subform)
                          (cond (-> subform first (= :.)) (get-in ctx (vec (rest subform)))
                                (-> subform first fn?)    (apply (first subform)
                                                              (rest subform))
                                :else                     subform)
                          subform)))))

(apply-in {:x {:x1 [0 100 200 300]}
           :y {:y1 [0 10 20 30]}}
          [+
           [reduce + [10000 30000]]
           [* [+
               [:. :x :x1 3]
               [:. :y :y1 2]
               1]
            10]])

(defn as-path [p]
  (cond (keyword? p)    [p]
        (sequential? p) p))

(defn add-in [ctx
              new-path
              form]
  (assoc-in ctx
            (as-path new-path)
            (apply-in ctx form)))

(add-in {:x {:x1 [0 100 200 300]}
         :y {:y1 [0 10 20 30]}}
        :z
        [+
         [reduce + [10000 30000]]
         [* [+
             [:. :x :x1 3]
             [:. :y :y1 2]
             1]
          10]])

(-> {:x {:x1 [0 100 200 300]}
     :y {:y1 [0 10 20 30]}}
    (add-in [:z :z1]
            [0 1 2 3])
    (add-in [:z :z1 1]
     [+
      [reduce + [10000 30000]]
      [* [+
          [:. :x :x1 3]
          [:. :y :y1 2]
          1]
       10]]))


["## Spatial index structures

See the JTS [SearchUsingPreparedGeometryIndex tutorial](https://github.com/locationtech/jts/blob/master/modules/example/src/main/java/org/locationtech/jtsexample/technique/SearchUsingPreparedGeometryIndex.java)"]

(defn make-spatial-index [dataset & {:keys [geometry-column]
                                     :or   {geometry-column :geometry}}]
  (let [tree ^STRtree (STRtree.)]
    (doseq [row (tablecloth/rows dataset :as-maps)]
      (let [geometry ^Geometry (row geometry-column)]
        (.insert tree
                 (.getEnvelopeInternal geometry)
                 (assoc row
                        :prepared-geometry
                        (PreparedGeometryFactory/prepare geometry)))))
    tree))

(defn location->indexed-places [^Point location spatial-index]
  (->> (.query ^STRtree spatial-index
               ^Envelope (.getEnvelopeInternal ^Point location))
       (filter (fn [row]
                 (.intersects ^PreparedGeometry (:prepared-geometry row)
                              location)))))

(defn location->indexed-places-str [location spatial-index name-fn]
  (some-> location
          (location->indexed-places spatial-index)
          (->> (map name-fn)
               (string/join " | "))))

(delay
  (let [neighbourhoods-spatial-index (-> (get-neighbourhoods)
                                         make-spatial-index)
        pickup-location (-> (get-taxi-data)
                            add-taxi-geometries
                            (dataset/sample 1 {:seed 1})
                            :pickup-location
                            first)]
    (location->indexed-places-str pickup-location
                                  neighbourhoods-spatial-index
                                  :neighborhood)))

["## Joining the Taxi and Neighbourhoods datasets"]

(update {:x 9} :x inc)
{:x 10}

(delay
 (-> {:data           (get-taxi-data)
      :neighbourhoods {:data (get-neighbourhoods)}}
     (update :neighbourhoods
             add-in
             :spatial-index
             [make-spatial-index
              [:. :data]])
     (update :data add-taxi-geometries)))


(defn join-neighbourhoods-to-data [{:keys [data neighbourhoods]
                                    :as ctx}]
  (let [{:keys [spatial-index]} neighbourhoods]
    (-> ctx
        (update :data
                tablecloth/add-or-replace-columns
                {:pickup-neigbourhood   (fn [ds]
                                          (map #(location->indexed-places-str % spatial-index :neighborhood)
                                              (:pickup-location ds)))
                 :dropoff-neighbourhood (fn [ds]
                                          (map #(location->indexed-places-str % spatial-index :neighborhood)))}))))

(def taxi-data-with-neighbourhoods
  (memoize
   (fn []
     (-> {:data           (get-taxi-data)
          :neighbourhoods {:data (get-neighbourhoods)}}
         (update :neighbourhoods
                 add-in
                 :spatial-index
                 [make-spatial-index
                  [:. :data]])
         (update :data add-taxi-geometries)
         join-neighbourhoods-to-data))))

(delay
  (taxi-data-with-neighbourhoods))

["What pickup-dropoff neighbourhood-pairs are most frequent?"]

^kind/dataset
(delay
  (-> (taxi-data-with-neighbourhoods)
      :data
      (tablecloth/select-columns [:pickup-neigbourhood])
      (tablecloth/rows :as-maps)
      frequencies
      (->> (map (fn [[pair times]]
                  (assoc pair :times times))))
      tablecloth/dataset
      (tablecloth/order-by :times :desc)))

["## Mean durations by neighbourhoods and hour"]

(delay
  (-> (taxi-data-with-neighbourhoods)
      :data
      (tablecloth/add-or-replace-columns {:pickup-hour
                                          (fn [ds]
                                            (->> ds
                                                 :pickup-datetime
                                                 (datetime/long-temporal-field :hours)))
                                          :dropoff-hour
                                          (fn [ds]
                                            (->> ds
                                                 :dropoff-datetime
                                                 (datetime/long-temporal-field :hours)))})
      (tablecloth/group-by [:pickup-neigbourhood :pickup-hour])
      (tablecloth/aggregate {:mean-duration (fn [ds]
                                              (-> ds
                                                  :trip-duration
                                                  dtype-fun/mean))})))

["."]


