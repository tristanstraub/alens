(set-env! :dependencies '[[boot-bundle "0.1.0-SNAPSHOT" :scope "test"]])

(require '[boot-bundle :refer [expand-keywords]])

(reset! boot-bundle/bundle-file-path "boot.bundle.edn")

(set-env! :source-paths #{"src"}
          :dependencies
          (expand-keywords '[:base :testing]))

(deftask testing [] (merge-env! :source-paths #{"test"}) identity)
(deftask installing [] (merge-env! :resource-paths #{"src"}) identity)

(require '[zilti.boot-midje :refer [midje]])
(require '[crisptrutski.boot-cljs-test :as cljs])

(deftask test []
  (comp (testing)
        (midje)
        (cljs/test-cljs)))

(def +version+ "0.1.0-SNAPSHOT")

(task-options!
  pom {:project     'tristanstraub/alens
       :version     +version+
       :description "Lenses that work with core.async."
       :url         "https://github.com/tristanstraub/alens"
       :license     {"MIT" "https://opensource.org/licenses/MIT"}})

(deftask build []
  (comp (installing)
        (pom)
        (jar)
        (install)))
