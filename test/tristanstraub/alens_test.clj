(ns tristanstraub.alens-test
  (:use midje.sweet)
  (:require [clojure.core.async :as a]
            [tristanstraub.alens :refer [projector at id fapply each fwhen leaves]]
            [clojure.test :as t]))

(fact "Can view via 'at'"
  (let [project   (projector id)]
    (project {:x {:y 2}}
             (at :x :y)))
  => 2)

(fact "Can update via 'at'"
  (let [fapply (fn
                 ([x] x)
                 ([f & xs] (apply f xs)))
        project   (projector fapply)]
    (project {:x {:y 2 :z 5}} (comp (at :x) (at :y)) inc))
  => {:x {:y 3 :z 5}})

(fact "Can view async via 'at'"
  (let [c       (a/chan)
        project (projector fapply)]
    (a/put! c {:y 1})
    (a/<!! (project {:x 1} (at :x))))
  => 1)

(fact "Can update async via 'at'"
  (let [c       (a/chan)
        project (projector fapply)]
    (a/put! c {:y 1})

    (a/<!! (project {:x c} (at :x))))
  => {:y 1})

(fact "Can update through single channel"
  (let [c       (a/chan)
        project (projector fapply)]
    (a/put! c {:y 1})
    (a/<!! (project {:x c} (comp (at :x) (at :y)) inc)))
  => {:x {:y 2}})

(fact "Can update through multiple channels"
  (let [c       (a/chan)
        d       (a/chan)
        project (projector fapply)]
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project {:x c} (comp (at :x) (at :y) (at :z)) inc)))
  => {:x {:y {:z 6}}})

(fact "Can update through root channel"
  (let [b (a/chan)
        c       (a/chan)
        d       (a/chan)
        project (projector fapply)]
    (a/put! b {:x c})
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project b (comp (at :x) (at :y) (at :z)) inc)))
 => {:x {:y {:z 6}}})

(fact "'in' lens works like (comp at...)"
  (let [b (a/chan)
        c       (a/chan)
        d       (a/chan)
        project (projector fapply)]
    (a/put! b {:x c})
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project b (at :x :y :z) inc)))
  => {:x {:y {:z 6}}})

(fact "Can recurse over leaves of map"
  (let [m       {:a 1 :c 3 :d {:e 4 :z [1 2 3]}}
        project (projector id)]
    (project m (comp leaves (fwhen number?)) inc))
  => {:a 2, :c 4, :d {:e 5, :z [1 2 3]}})

(fact "Can recurse over leaves of map with chans within"
  (let [c       (a/chan)
        m       {:a c}
        project (projector fapply)]
    (a/put! c 1)
    (a/<!! (project m leaves)))
  => 1)

(fact "Can increment leaves through channels"
  (let [l (leaves)
        l (l fapply)
        c (a/chan)
        d (a/chan)]
    (a/put! d {:z 3})
    (a/put! c {:y d})
    (a/<!! (l inc {:x c})))
  => {:x {:y {:z 4}}})

(fact "Can read leaves through channel"
  (let [l (leaves)
        l (l fapply)
        c (a/chan)]
    (a/put! c 1)
    (a/<!! (l {:x c})))
  => [1])

(fact "Can read leaves through nested channels"
  (let [l (leaves)
        l (l fapply)
        c (a/chan)]
    (a/put! c {:y 2})
    (a/<!! (l {:x c})))
  => [2])
