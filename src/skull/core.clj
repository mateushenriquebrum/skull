(ns skull.core
  (:require [skull.io :as io]))

(defn skull-ext [file]
  (str file ".sk"))

(defn journal-ext [file]
  (str file ".skj"))

(defn write [path data]
  (let [bytes (io/string-to-byte-buffer data)]
    (with-open [c (io/writeable-channel path)]
      (.write c bytes))))

(defn transfer-to [src-path rec-path]
  (with-open [src (io/readable-channel src-path)
              rec (io/writeable-channel rec-path)]
    (.transferTo src 0 (.size src) rec)))

(defn versiontated-data [struct]
  (pr-str (io/versionate struct)))

(defn journal [path structure]
  (let [journal-path (journal-ext path)
        data (versiontated-data structure)]
    (write journal-path data)))

(defn snapshot [file, structure]
  (journal file structure)
  (transfer-to (journal-ext file) (skull-ext file)))

(defn recover [file]
  (let [skull-path (skull-ext file)
        journal-path (journal-ext file)]
    (if (io/exists skull-path)
      (io/file-to-data skull-path)
      (io/file-to-data journal-path))))

(defn smt [src fn]
  (dosync
   (swap! src fn)))

(defn transaction [file fn]
  (let [src (atom (recover file))]
    (smt src fn)
    (snapshot file @src)))


