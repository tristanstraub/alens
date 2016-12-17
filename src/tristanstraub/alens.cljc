(ns tristanstraub.alens
  #?(:cljs (:require-macros [cljs.core.async.macros :as a]))
  (:require #?(:clj [clojure.core.async :as a]
               :cljs [cljs.core.async :as a])))


(defn projector%
  [fapply]
  (fn
    ([x l] (fapply (l fapply) x))
    ([x l f] (fapply (l fapply) f x))))

(defn projector
  [fapply]
  (fn
    ([x l] ((projector% fapply) x (l)))
    ([x l f] ((projector% fapply) x (l) f))))

(defn lift [outer]
  (fn
    ([] outer)
    ([inner]
     (fn [fapply]
       (let [p (projector% fapply)]
         (fn
           ([x] (-> x (p outer) (p inner)))
           ([f x] (p x outer #(p % inner f)))))))))

(defn lift2 [l]
  (lift (fn
          ([] l)
          ([fapply] l))))



(defn id
  ([x] x)
  ([f & xs] (apply f xs)))

(defn at
  ([k]
   (lift (fn [fapply]
            (fn cb
              ([x] (get x k))
              ([f x]
               (fapply #(assoc %1 k %2) x (fapply f (cb x))))))))
  ([k & ks]
   (if (not (empty? ks))
     (comp (at k) (apply at ks))
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

(def each
  (lift2
   (fn
     ([x] (seq x))
     ([f x] (map f x)))))

(def leaves
  (lift
   (fn [fapply]
     (let [p (projector fapply)]
       (fn cb
         ([x]
          (cond (map? x)
                (mapcat cb (->> x (map second)))
                :else
                [x]))
         ([f x]
          (cond (map? x)
                (->> x
                     (map (fn [[k v]] [k (p v leaves f)]))
                     (into {}))
                :else
                (f x))))))))

(defn fwhen [pred?]
  (lift
   (fn [fapply]
     (fn
       ([x] x)
       ([f x] (if (pred? x) (f x) x))))))
