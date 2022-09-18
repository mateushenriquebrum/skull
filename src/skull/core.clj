(ns skull.core
  (:require [clojure.java.io :as io]
            [skull.util :as util])
  (:import
   [java.nio.file OpenOption Paths StandardOpenOption]
   [java.net URI]
   [java.nio.channels FileChannel]))

;;todo
;;snapshot logic

(defn skull-ext [file]
  (str file ".sk"))

(defn journal-ext [file]
  (str file ".skj"))

(defn string-to-path [string]
  (let [uri (URI/create (str "file:" string))]
    (Paths/get uri)))

(defn writeable-channel [path]
  (let [options (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/APPEND])]
    (FileChannel/open (string-to-path path) options)))

(defn readable-channel [path]
  (let [options (into-array OpenOption [StandardOpenOption/READ])]
    (FileChannel/open (string-to-path path) options)))

(defn write [path data]
  (let [bytes (util/string-to-byte-buffer data)]
    (with-open [c (writeable-channel path)]
      (.write c bytes))))

(defn transfer-to [src-path rec-path]
  (with-open [src (readable-channel src-path)
              rec (writeable-channel rec-path)]
    (.transferTo src 0 (.size src) rec)))

(defn journal [path structure]
  (let [journal-path (journal-ext path)
        data (pr-str (util/versionate structure))]
    (write journal-path data)))

(defn snapshot [file, structure]
  (let [data (pr-str (util/versionate structure))]
  (with-open [w (io/writer (skull-ext file) :append false)]
    (.write w data))))

(defn recover [file]
  (let [skull-path (skull-ext file)]
    (if (util/exists skull-path)
      (util/file-to-data skull-path)
      (let [h (util/adler (pr-str (list)))]
        (snapshot file (list {:version h}))))))

(defn transaction [src fn]
  (dosync
   (swap! src fn)))

(defn unit [file fn]
  (let [src (atom (recover file))]
    (transaction src fn)
    (snapshot file @src)))


