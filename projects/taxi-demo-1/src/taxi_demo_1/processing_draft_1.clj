(ns taxi-demo-1.processing-draft-1
  (:require [notespace.api]))

["# Taxi spatio-temporal data processing example"]

["We want to compute, for every taxi trip,
the average duration of trips
that dropped-off in the last 3 hours
with the same pickup neighbourhood and dropoff neighbourhood."]

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
         '[com.rpl.specter :as specter])

(import (org.locationtech.jts.index.strtree STRtree)
        (org.locationtech.jts.geom Geometry Point Polygon)
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

(def neighbourhoods
  (delay
    (-> "data/nyc-taxi-trip-duration/nyc-neighbourhoods.json"
        slurp
        geoio/read-geojson
        (->> (map (fn [{:keys [geometry properties]}]
                    (assoc properties :geometry geometry))))
        tablecloth/dataset
        (tablecloth/rename-columns {(keyword "@id") :id})
        (tablecloth/add-or-replace-column
         :geometry #(map wgs84->nad83-2011
                         (:geometry %)))
        (vary-meta assoc :print-column-max-width 100))))

^kind/dataset
(delay
  @neighbourhoods)

["### Taxi rides"]

(defn ->column-name [raw-column-name]
  (-> raw-column-name
      name
      (string/replace #"_" "-")
      keyword))

(->column-name "passenger_count")

(def raw-taxi-data
  (delay
   (let [path "data/nyc-taxi-trip-duration/train-sample0.05.csv.gz"]
     (println [:reading path])
     (-> (tablecloth/dataset path {:key-fn    ->column-name
                                   :parser-fn {:pickup-datetime  [:local-date-time "yyyy-MM-dd HH:mm:ss"]
                                               :dropoff-datetime [:local-date-time "yyyy-MM-dd HH:mm:ss"]}})
         #_(tablecloth/head 5000)))))

^kind/dataset
(delay
  @raw-taxi-data)

(defn nad83-2011-point [latitude longitude]
  (-> (jts/point latitude longitude)
      wgs84->nad83-2011))

(defn distance [^Geometry g1 ^Geometry g2]
  (.distance g1 g2))

(def taxi-data-with-geometries
  (delay
    (-> @raw-taxi-data
        (tablecloth/add-or-replace-columns
         {:pickup-location  #(map nad83-2011-point
                                  (:pickup-latitude %)
                                  (:pickup-longitude %))
          :dropoff-location #(map nad83-2011-point
                                  (:dropoff-latitude %)
                                  (:dropoff-longitude %))})
        (tablecloth/add-or-replace-column
         :distance  #(-> (dtype/emap distance
                                     :float32
                                     (:pickup-location %)
                                     (:dropoff-location %))
                         dtype/clone)))))

^kind/dataset
(delay
  @taxi-data-with-geometries)

(delay
  (-> @taxi-data-with-geometries
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
  (-> @taxi-data-with-geometries
    (dataset/sample 200 {:seed 1})
    draw-trips-as-lines))

["The trips of longest duration"]

^kind/hiccup
(delay
  (-> @taxi-data-with-geometries
      (tablecloth/order-by :trip-duration :desc)
      (tablecloth/head 200)
      draw-trips-as-lines))

["The trips of longest distance"]

^kind/hiccup
(delay
  (-> @taxi-data-with-geometries
      (tablecloth/order-by :distance :desc)
      (tablecloth/head 5)
      draw-trips-as-lines))



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

(def neighbourhoods-index
  (delay (make-spatial-index @neighbourhoods)))

(defn location->neighbourhoods [^Geometry location]
  (->> (.query ^STRtree @neighbourhoods-index
               ^Envelope (.getEnvelopeInternal ^Point location))
       (filter (fn [row]
                 (.intersects ^PreparedGeometry (:prepared-geometry row)
                              location)))))

(defn location->neighbourhoods-string [^Geometry location]
  (some->> location
           location->neighbourhoods
           (map :neighborhood)
           (string/join " | ")))

["## Joining the Taxi and Neighbourhoods datasets"]

(def taxi-data-with-neighbourhoods
  (delay
    (-> @taxi-data-with-geometries
        (tablecloth/add-or-replace-columns
         {:pickup-neigbourhood #(dtype/clone
                                 (dtype/emap location->neighbourhoods-string
                                             :string
                                             (:pickup-location %)))
          :dropoff-neighbourhood #(dtype/clone
                                 (dtype/emap location->neighbourhoods-string
                                             :string
                                             (:dropoff-location %)))}))))

["What pickup-dropoff neighbourhood-pairs are most frequent?"]

^kind/dataset
(delay
  (-> @taxi-data-with-neighbourhoods
      (tablecloth/select-columns [:pickup-neigbourhood :dropoff-neighbourhood])
      (tablecloth/rows :as-maps)
      frequencies
      (->> (map (fn [[pair times]]
                  (assoc pair :times times))))
      tablecloth/dataset
      (tablecloth/order-by :times :desc)))

["## Temporal index structures"]

(defn make-temporal-index [datetime-column]
  (TreeMap. ^java.util.Map
            (zipmap (dtype/emap datetime/local-date-time->milliseconds-since-epoch
                                :int64
                                datetime-column)
                    (dtype/make-reader :int64
                                       (count datetime-column)
                                       idx))))

(def pickup-datetime-index
  (delay
    (-> @taxi-data-with-neighbourhoods
        :pickup-datetime
        make-temporal-index)))

(def dropoff-datetime-index
  (delay
    (-> @taxi-data-with-neighbourhoods
        :dropoff-datetime
        make-temporal-index)))

(def milliseconds-in-an-hour (* 60 60 1000))

(defn row-numbers-of-last-few-hours-by-index [index reference-datetime num-hours]
  (let [end   (datetime/local-date-time->milliseconds-since-epoch
               reference-datetime)
        start (- end
                 (* num-hours milliseconds-in-an-hour))]
    (-> (.subMap ^TreeMap
                 index
                 start
                 true
                 end
                 true)
        (.values))))

(delay
  (let [index              @dropoff-datetime-index
        reference-datetime (-> @taxi-data-with-neighbourhoods
                               (dataset/sample 1 {:seed 1})
                               :pickup-datetime
                               first)
        num-hours          3
        row-numbers        (row-numbers-of-last-few-hours-by-index index
                                                                   reference-datetime
                                                                   num-hours)]
    {:pickup-datetime          reference-datetime
     :recent-dropoff-datetimes (-> @taxi-data-with-neighbourhoods
                                   (tablecloth/select-rows row-numbers)
                                   :dropoff-datetime)}))

["## Rolling windows"]

(defn roll-window
  [ds reference-column-name temporal-index-structure num-hours]
  (->> reference-column-name
       ds
       (map-indexed (fn [reference-row-number reference-datetime]
                      (let [row-numbers (row-numbers-of-last-few-hours-by-index
                                         temporal-index-structure
                                         reference-datetime
                                         num-hours)
                            group-name  {:original-row-number reference-row-number}]
                        [group-name row-numbers])))
       (into {})
       (tablecloth/group-by ds)))


(delay
  (-> @taxi-data-with-neighbourhoods
      (roll-window :pickup-datetime
                   @dropoff-datetime-index
                   3)
      time))


^kind/dataset
(delay
  (-> @taxi-data-with-neighbourhoods
      (roll-window :pickup-datetime
                   @dropoff-datetime-index
                   3)
      ;; (tablecloth/group-by [:pickup-neigbourhood :dropoff-neighbourhood])
      (tablecloth/aggregate (fn [ds]
                              {:mean-trip-duration (-> ds :trip-duration dtype-fun/mean)}))
      ;; (tablecloth/order-by :original-row-number)
      time))

["."]
