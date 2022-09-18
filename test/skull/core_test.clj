(ns skull.core-test
  (:require [clojure.test :refer :all]
            [skull.core :refer :all]
            [clojure.java.io :as io]
            [skull.util :as util]))

(defn delete [file]
  (when (util/exists file)
    (io/delete-file file)))

(defn in-path [file]
  (str "/Users/mbrum/Workspace/Clojure/skull/test/res/" file))

(deftest persist
  (let [data '({:name "Mateus" :surname "Brum"} {:name "Iago" :surname "Brum"})]
    
    (testing "it serialize data into the file"
      (let [file (in-path "serialize.sk")]
        (delete file)
        (snapshot file data)
        (is (= true (util/exists file)))
        (is (= data (rest (recover file))))))
    
    (testing "convert to byte buffer"
      (let [to-convert "àáçćÿįī@%±~abc123"]
        (is 16 (.capacity (string-to-byte-buffer to-convert)))))
    
    (testing "it recover empty list"
      (let [file (in-path "empty.sk")]
        (delete file)
        (is (= nil (recover file)))))
    
    (testing "it journal the file before persist"
      (let [file (in-path "journaled.sk")]
        (delete (journal-ext file))
        (journal file data)
        (is (= true (util/exists (journal-ext file))))))))

(deftest access
  
  (testing "is queues the access to data"
    (let [src (atom 0)]
      (dotimes [_ 9]
        (future (transaction src (fn [data] (+ data 1)))))
      (Thread/sleep 100) ;; workaround, how to join all futures?
      (is (= 9 @src)))))

(deftest unit-of-work

  (testing "is persisting changes for single access"
    (let [file (in-path "single.sk")
          added {:name "Mateus" :surname "Brum"}]
      (delete file)
      (unit file (fn [data] (cons added data)))
      (is (= added (last (recover file)))))))

(deftest version
  
  (testing "it returs a hash number"
     (is (= 110297644 (util/adler "skull" ))))
  
  (testing "it versionate a data"
    (is (= {:version 8061010} (first (util/versionate (list)))))))