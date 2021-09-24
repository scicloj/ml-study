(defproject ml-study/model-building-1 "0.1.0-SNAPSHOT"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"}
  :dependencies [[scicloj/scicloj.ml   "0.1.0-beta3"]
                 [scicloj/notespace    "4-pre-alpha-2-SNAPSHOT"]
                 [org.scicloj/viz.clj  "0.1.0-SNAPSHOT"]
                 [techascent/tech.viz  "6.00-beta-16-2"]]
  :profiles {:dev {:repl-options {:nrepl-middleware [scicloj.notespace.v4.nrepl/middleware]}}})

