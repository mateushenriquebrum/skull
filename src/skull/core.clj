(ns skull.core
  (:require [clojure.java.io :as io]
            [skull.util :as util])
  (:import
   [java.nio.file OpenOption Paths StandardOpenOption]
   [java.net URI]
   [java.nio.channels FileChannel]))

(defn journal-ext [file]
  (str file "j"))

(defn journal [file structure]
  (let [options (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/APPEND])
        journal-file (journal-ext file)
        uri (URI/create (str "file:" journal-file))
        path (Paths/get uri)
        data (pr-str (util/versionate structure))
        bytes (util/string-to-byte-buffer data)]
    (with-open [c (FileChannel/open path options)]
      (.write c bytes))))

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


