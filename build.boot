(set-env! :dependencies '[[boot-bundle "0.1.0-SNAPSHOT" :scope "test"]])

(require '[boot-bundle :refer [expand-keywords]])

(reset! boot-bundle/bundle-file-path "boot.bundle.edn")

(set-env! :source-paths #{"src" "test"}
          :resource-paths #{"resources"}
          :test-paths #{"test"}
          :dependencies
          (expand-keywords '[:base :testing]))

(require '[zilti.boot-midje :refer [midje]])

(deftask test []
  (comp (watch)
        (midje)))
