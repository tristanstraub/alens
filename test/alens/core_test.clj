(ns alens.core-test
  (:use midje.sweet)
  (:require [clojure.core.async :as a]
            [alens.core :as l :refer [projector at id]]
            [clojure.test :as t]))

(fact "Can view via 'at'"
  (let [project   (projector (id #(%1 %2)))]
    (project {:x {:y 2}}
             (l/lens-juxt (at :x) (at :y))))
  => 2)

(fact "Can update via 'at'"
  (let [fapply (fn
                 ([x] x)
                 ([f & xs] (apply f xs)))
        project   (projector fapply)]
    (project {:x {:y 2 :z 5}} (l/lens-juxt (at :x) (at :y)) inc))
  => {:x {:y 3 :z 5}})

(fact "Can view async via 'at'"
  (let [c       (a/chan)
        project (projector l/fapply)]
    (a/put! c {:y 1})
    (a/<!! (project {:x 1} (at :x))))
  => 1)

(fact "Can update async via 'at'"
  (let [c       (a/chan)
        project (projector l/fapply)]
    (a/put! c {:y 1})

    (a/<!! (project {:x c} (at :x))))
  => {:y 1})

(fact "Can update through single channel"
  (let [c       (a/chan)
        project (projector l/fapply)]
    (a/put! c {:y 1})
    (a/<!! (project {:x c} (l/lens-juxt (at :x) (at :y)) inc)))
  => {:x {:y 2}})

(fact "Can update through multiple channels"
  (let [c       (a/chan)
        d       (a/chan)
        project (projector l/fapply)]
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project {:x c} (l/lens-juxt (at :x) (at :y) (at :z)) inc)))
  => {:x {:y {:z 6}}})

(fact "Can update through root channel"
  (let [b (a/chan)
        c       (a/chan)
        d       (a/chan)
        project (projector l/fapply)]
    (a/put! b {:x c})
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project b (l/lens-juxt (at :x) (at :y) (at :z)) inc)))
 => {:x {:y {:z 6}}})

(fact "'in' lens works like (lens-juxt at...)"
  (let [b (a/chan)
        c       (a/chan)
        d       (a/chan)
        project (projector l/fapply)]
    (a/put! b {:x c})
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project b (at :x :y :z) inc)))
  => {:x {:y {:z 6}}})
