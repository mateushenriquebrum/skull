(ns skull.util
  (:require
   [clojure.java.io :as io])
  (:import
   [java.util.zip Adler32]
   [java.nio ByteBuffer]
   [java.nio.charset StandardCharsets]))

(defn adler [string]
  (let [bts (.getBytes string)
        alg (Adler32.)
        size (count bts)]
    (.update alg bts 0 size)
    (.getValue alg)))

(defn versionate [data]
  (let [meta {:version (adler (pr-str data))}]
    (conj data meta)))

(defn exists [file]
  (.exists (io/file file)))

(defn file-to-data [file]
  (let [data (slurp file)]
    (read-string data)))

(defn string-to-byte-buffer [string]
  (let [bts (.getBytes string StandardCharsets/UTF_8)]
    (doto
     (ByteBuffer/wrap bts))))