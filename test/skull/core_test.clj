(ns skull.core-test
  (:require [clojure.test :refer :all]
            [skull.core :refer :all]
            [skull.io :as io]))

(defn res-path [file]
  (str (io/pwd) "/test/res/" file))

(deftest persist-file
  
  (let [data '({:name "Mateus" :surname "Brum"} {:name "Iago" :surname "Brum"})]

    (testing "it serialize data into the file"
      (let [file (res-path "serialize")]
        (io/safe-delete file)
        (snapshot file data)
        (is (= true (io/exists (skull-ext file))))
        (is (= data (rest (recover file))))))

    (testing "convert to byte buffer"
      (let [to-convert "àáçćÿįī@%±~abc123"]
        (is 16 (.capacity (io/string-to-byte-buffer to-convert)))))

    (testing "it fail if no recover file is found"
      (let [file (res-path "empty")]
        (io/safe-delete (journal-ext file))
        (io/safe-delete (skull-ext file))
        (is (thrown? Exception (recover file)))))

    (testing "it recover from journal"
      (let [file (res-path "broken")]
        (io/safe-delete (journal-ext file))
        (io/safe-delete (skull-ext file))
        (is (= true false))))

    (testing "it journal the file before persist"
      (let [file (res-path "journaled")]
        (io/safe-delete (journal-ext file))
        (io/safe-delete (skull-ext file))
        (journal file data)
        (is (= true (io/exists (journal-ext file))))))))

(deftest transfer-by-channel
  
  (testing "it should transfer"
    (let [r (res-path "rec.transfer")
          s (res-path "read.only")]
      (io/safe-delete r)
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
      (io/safe-delete file)
      (transaction file (fn [data] (cons added data)))
      (is (= added (last (recover file)))))))

(deftest version
  
  (testing "it returs a hash number"
     (is (= 110297644 (io/adler "skull" ))))
  
  (testing "it versionate a data"
    (is (= {:version 8061010} (first (io/versionate (list))))))
  
  (testing "it versionate persistence file"
    (let [data (list #{:skull :rules})
          main-file (res-path "main-versionated")
          jour-file (res-path "journal-versionated")]
      (io/safe-delete main-file)
      (io/safe-delete (str jour-file "j"))
      (snapshot main-file data)      
      (is (= {:version 961807959} (first (io/file-to-data (res-path "main-versionated.sk")))))
      (is (= {:version 961807959} (first (io/file-to-data (res-path "journal-versionated.skj"))))))))