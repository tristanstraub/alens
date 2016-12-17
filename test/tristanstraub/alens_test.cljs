(ns tristanstraub.alens-test
  (:require-macros [cljs.test :refer [deftest testing is async]]
                   [cljs.core.async.macros :as a])
  (:require [cljs.test]
            [cljs.core.async :as a]
            [tristanstraub.alens :refer [at projector async-fapply async-fjoin]]))

(deftest test-async-view
  (async done
         (a/go
           (let [c       (a/chan)
                 project (projector (async-fapply async-fjoin) async-fjoin)]
             (a/put! c {:y 1})
             (is (= 1 (a/<! (project {:x 1} (at :x)))))
             (done)))))

(deftest test-async-update
  (async done
         (a/go
           (is (= {:x {:y {:z 6}}}
                  (let [b       (a/chan)
                        c       (a/chan)
                        d       (a/chan)
                        project (projector (async-fapply async-fjoin) async-fjoin)]
                    (a/put! b {:x c})
                    (a/put! c {:y d})
                    (a/put! d {:z 5})
                    (a/<! (project b (at :x :y :z) inc)))))
           (done))))
