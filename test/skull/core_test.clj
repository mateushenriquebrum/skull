(ns skull.core-test
  (:require [clojure.test :refer :all]
            [skull.core :refer :all]
            [clojure.java.io :as io]
            [skull.util :as util]))

;;refactor the tests

(defn delete [file]
  (when (util/exists file)
    (io/delete-file file)))

(defn res-path [file]
  (str (util/pwd) "/test/res/" file))

(deftest persist-file
  
  (let [data '({:name "Mateus" :surname "Brum"} {:name "Iago" :surname "Brum"})]
    
    (testing "it serialize data into the file"
      (let [file (res-path "serialize")]
        (delete file)
        (snapshot file data)
        (is (= true (util/exists (skull-ext file))))
        (is (= data (rest (recover file))))))
    
    (testing "convert to byte buffer"
      (let [to-convert "àáçćÿįī@%±~abc123"]
        (is 16 (.capacity (util/string-to-byte-buffer to-convert)))))
    
    (testing "it recover empty list"
      (let [file (res-path "empty")]
        (delete (skull-ext file))
        (is (= nil (recover file)))))
    
    (testing "it journal the file before persist"
      (let [file (res-path "journaled")]
        (delete (journal-ext file))
        (journal file data)
        (is (= true (util/exists (journal-ext file))))))))

(deftest transfer-by-channel
  
  (testing "it should transfer"
    (let [r (res-path "rec.transfer")
          s (res-path "read.only")]
      (delete r)
      (transfer-to s r)
      (is (= "read only file" (slurp r))))))

(deftest access
  
  (testing "is queues the access to data"
    (let [src (atom 0)]
      (dotimes [_ 9]
        (future (smt src (fn [data] (+ data 1)))))
      (Thread/sleep 100) ;; workaround, how to join all futures?
      (is (= 9 @src)))))

(deftest transactional

  (testing "is persisting changes for single access"
    (let [file (res-path "single")
          added {:name "Mateus" :surname "Brum"}]
      (delete file)
      (transaction file (fn [data] (cons added data)))
      (is (= added (last (recover file)))))))

(deftest version
  
  (testing "it returs a hash number"
     (is (= 110297644 (util/adler "skull" ))))
  
  (testing "it versionate a data"
    (is (= {:version 8061010} (first (util/versionate (list))))))
  
  (testing "it versionate persistence file"
    (let [data (list #{:skull :rules})
          main-file (res-path "main-versionated")
          jour-file (res-path "journal-versionated")]
      (delete main-file)
      (delete (str jour-file "j"))
      (snapshot main-file data)      
      (is (= {:version 961807959} (first (util/file-to-data (res-path "main-versionated.sk")))))
      (is (= {:version 961807959} (first (util/file-to-data (res-path "journal-versionated.skj"))))))))