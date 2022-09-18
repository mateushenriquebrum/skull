(ns skull.core
  (:require [clojure.java.io :as io]
            [skull.util :as util])
  (:import
   [java.nio.file OpenOption Paths StandardOpenOption]
   [java.net URI]
   [java.nio.channels FileChannel]))

(defn journal-ext [file]
  (str file "j"))

(defn channel-write [path]
  (let [options (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/APPEND])
        uri (URI/create (str "file:" path))
        path (Paths/get uri)]
    (FileChannel/open path options)))

(defn channel-read [path]
  (let [options (into-array OpenOption [StandardOpenOption/READ])
        uri (URI/create (str "file:" path))
        path (Paths/get uri)]
    (FileChannel/open path options)))

(defn write [path data]
  (let [bytes (util/string-to-byte-buffer data)]
    (with-open [c (channel-write path)]
      (.write c bytes))))

(defn transfer-to [src-path rec-path]
  (with-open [src (channel-read src-path)
              rec (channel-write rec-path)]
    (.transferTo src 0 (.size src) rec)))

(defn journal [path structure]
  (let [journal-path (journal-ext path)
        data (pr-str (util/versionate structure))]
    (write journal-path data)))

(defn snapshot [file, structure]
  (let [data (pr-str (util/versionate structure))]
  (with-open [w (io/writer file :append false)]
    (.write w data))))

(defn recover [file]
  (if (util/exists file)
    (util/file-to-data file)
    (let [h (util/adler (pr-str (list)))]
      (snapshot file (list {:version h})))))

(defn transaction [src fn]
  (dosync
   (swap! src fn)))

(defn unit [file fn]
  (let [src (atom (recover file))]
    (transaction src fn)
    (snapshot file @src)))


