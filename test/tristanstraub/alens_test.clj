(ns tristanstraub.alens-test
  (:use midje.sweet)
  (:require [clojure.core.async :as a]
            [tristanstraub.alens :refer [project project-async projector at id async-fapply async-fjoin fwhen leaves]]
            [clojure.test :as t]))

(fact "Can view via 'at'"
  (project {:x {:y 2}}
           (at :x :y))
  => 2)

(fact "Can update via 'at'"
  (project {:x {:y 2 :z 5}} (comp (at :x) (at :y)) inc)
  => {:x {:y 3 :z 5}})

(fact "Can view async via 'at'"
  (let [c       (a/chan)]
    (a/put! c {:y 1})
    (a/<!! (project-async {:x 1} (at :x))))
  => 1)

(fact "Can update async via 'at'"
  (let [c       (a/chan)]
    (a/put! c {:y 1})

    (a/<!! (project-async {:x c} (at :x))))
  => {:y 1})

(fact "Can update through single channel"
  (let [c       (a/chan)]
    (a/put! c {:y 1})
    (a/<!! (project-async {:x c} (comp (at :x) (at :y)) inc)))
  => {:x {:y 2}})

(fact "Can update through multiple channels"
  (let [c       (a/chan)
        d       (a/chan)]
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project-async {:x c} (comp (at :x) (at :y) (at :z)) inc)))
  => {:x {:y {:z 6}}})

(fact "Can update and read through root channel"
  (let [b (a/chan)
        c (a/chan)
        d (a/chan)]
    (a/put! b {:x {:y 3} :z c})
    (a/put! c {:k d})
    (a/put! d {:l 5})
    (-> b
        (project-async (comp (at :x) leaves) inc)
        a/<!!
        (project-async (comp (at :z :k)))
        a/<!!))
 => {:l 5})

(fact "Can update through root channel"
  (let [b (a/chan)
        c       (a/chan)
        d       (a/chan)]
    (a/put! b {:x c})
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project-async b (comp (at :x) (at :y) (at :z)) inc)))
 => {:x {:y {:z 6}}})

(fact "'in' lens works like (comp at...)"
  (let [b (a/chan)
        c       (a/chan)
        d       (a/chan)]
    (a/put! b {:x c})
    (a/put! c {:y d})
    (a/put! d {:z 5})
    (a/<!! (project-async b (at :x :y :z) inc)))
  => {:x {:y {:z 6}}})

(fact "Can recurse update over leaves of map"
  (let [m       {:a 1 :c 3 :d {:e 4 :z [1 2 3]}}]
    (project m (comp leaves (fwhen number?)) inc))
  => {:a 2, :c 4, :d {:e 5, :z [1 2 3]}})

(fact "Can recurse over leaves of map with chans within"
  (let [c       (a/chan)
        m       {:a c}]
    (a/put! c 1)
    (a/<!! (project-async m leaves)))
  => [1])

(fact "Can read leaves through channel"
  (let [l (leaves)
        l (l (async-fapply async-fjoin) async-fjoin)
        c (a/chan)]
    (a/put! c 1)
    (a/<!! (l {:x c})))
  => [1])

(fact "Can read leaves through nested channels"
  (let [l (leaves)
        l (l (async-fapply async-fjoin) async-fjoin)
        c (a/chan)]
    (a/put! c {:y 2})
    (a/<!! (l {:x c})))
  => [2])

(fact "Can increment leaves over async without channel values"
  (a/<!! (project-async {:x 1} leaves inc))
  => {:x 2})

(fact "Can increment leaves over async without with channel values"
  (let [c (a/chan)]
    (a/put! c 2)
    (a/<!! (project-async {:x c} leaves inc)))
  => {:x 3})

(fact "Can increment leaves over async without with nested channel values"
  (let [c (a/chan)]
    (a/put! c {:y 3})
    (a/<!! (project-async {:x c} leaves inc)))
  => {:x {:y 4}})

(fact "Can increment leaves over async without with very nested channel values"
  (let [c       (a/chan)
        d       (a/chan)]
    (a/put! d {:z 4})
    (a/put! c {:y d})
    (a/<!! (project-async {:x c} leaves inc)))
  => {:x {:y {:z 5}}})

(fact "Can increment leaves through channels"
  (let [l (leaves)
        l (l (async-fapply async-fjoin) async-fjoin)
        c (a/chan)
        d (a/chan)]
    (a/put! d {:z 3})
    (a/put! c {:y d})
    (a/<!! (l inc {:x c})))
  => {:x {:y {:z 4}}})
