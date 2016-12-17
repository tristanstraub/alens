(ns alens.core
  #?(:cljs (:require-macros [cljs.core.async.macros :as a]))
  (:require #?(:clj [clojure.core.async :as a]
               :cljs [cljs.core.async :as a])))

(defn projector [fapply]
  (fn
    ([x l] (fapply (l fapply) x))
    ([x l f] (fapply (l fapply) f x))))

(defn lens-juxt
  ([outer inner]
   (fn [fapply]
     (let [p (projector fapply)]
       (fn
         ([x] (-> x (p outer) (p inner)))
         ([f x]
          (p x outer #(p % inner f)))))))
  ([l1 l2 & more]
   (lens-juxt (lens-juxt l1 l2)
              (reduce lens-juxt more))))

(defn id [fapply]
  (fn
    ([x] x)
    ([f x] (fapply f x))))

(defn at
  ([k]
   (fn [fapply]
     (fn cb
       ([x] (get x k))
       ([f x]
        (fapply #(assoc %1 k %2) x (fapply f (cb x)))))))
  ([k & ks]
   (if ks
     (lens-juxt (at k) (apply at ks))
     (at k))))

(defn read-port? [ch]
  (satisfies? #?(:clj clojure.core.async.impl.protocols/ReadPort
                 :cljs cljs.core.async.impl.protocols/ReadPort)
              ch))

(defn fapply
  ([x] (a/go (if (read-port? x)
               (a/<! x)
               x)))
  ([f & xs] (a/go
              (let [xs     (loop [mxs []
                                  xs  xs]
                             (if xs
                               (let [[x & xs] xs]
                                 (recur (conj mxs (if (read-port? x)
                                                    (a/<! x)
                                                    x))
                                        xs))
                               mxs))
                    result (apply f xs)]
                (if (read-port? result)
                  (a/<! result)
                  result)))))
