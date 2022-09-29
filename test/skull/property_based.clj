(ns skull.property-based
  (:require
   [clojure.test.check.clojure-test :as test]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

(def property
  (prop/for-all [v (gen/vector gen/small-integer)]
                (let [s (sort v)]
                  (and (= (count v) (count s))
                       (or (empty? s) (apply <= s))))))

(test/defspec test 100 property)

