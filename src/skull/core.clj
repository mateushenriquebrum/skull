(ns skull.core
  (:require [clojure.java.io :as io])
  (:import
   [java.nio ByteBuffer]
   [java.nio.charset StandardCharsets]
   [java.nio.file OpenOption Paths StandardOpenOption]
   [java.net URI]
   [java.util.zip Adler32]
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

(defn string-to-byte-buffer [string]
  (let [bts (.getBytes string StandardCharsets/UTF_8)]
    (doto
     (ByteBuffer/wrap bts))))

(defn journal-ext [file]
  (str file "j"))

(defn journal [file structure]
  (let [options (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/APPEND])
        journal-file (journal-ext file)
        uri (URI/create (str "file:" journal-file))
        path (Paths/get uri)
        data (pr-str structure)
        bytes (string-to-byte-buffer data)]
    (with-open [c (FileChannel/open path options)]
      (.write c bytes))))

(defn snapshot [file, structure]
  (let [data (pr-str (versionate structure))]
  (with-open [w (io/writer file :append false)]
    (.write w data))))

(defn recover [file]
  (if (exists file)
    (let [data (slurp file)]
      (read-string data))
    (let [h (adler (pr-str (list)))]
      (snapshot file (list {:version h})))))

(defn transaction [src fn]
  (dosync
   (swap! src fn)))

(defn unit [file fn]
  (let [src (atom (recover file))]
    (transaction src fn)
    (snapshot file @src)))


