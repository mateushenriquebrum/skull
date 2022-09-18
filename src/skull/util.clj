(ns skull.util
  (:require
   [clojure.java.io :as io])
  (:import
   [java.util.zip Adler32]
   [java.nio ByteBuffer]
   [java.io File]
   [java.nio.charset StandardCharsets]
   [java.nio.file OpenOption Paths StandardOpenOption]
   [java.net URI]
   [java.nio.channels FileChannel]))

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

(defn pwd []
  (.getAbsolutePath (File. "")))

(defn string-to-byte-buffer [string]
  (let [bts (.getBytes string StandardCharsets/UTF_8)]
    (doto
     (ByteBuffer/wrap bts))))

(defn string-to-path [string]
  (let [uri (URI/create (str "file:" string))]
    (Paths/get uri)))

(defn writeable-channel [path]
  (let [options (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/APPEND])]
    (FileChannel/open (string-to-path path) options)))

(defn readable-channel [path]
  (let [options (into-array OpenOption [StandardOpenOption/READ])]
    (FileChannel/open (string-to-path path) options)))