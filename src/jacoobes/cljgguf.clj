(ns jacoobes.cljgguf
  (:require [gloss.io :as gio :refer [decode]])
  (:require [gloss.core :as gcore :refer [compile-frame string]])
  (:require [clojure.java.io :as io]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (java.nio.ByteBuffer/wrap (.toByteArray out))))


(def gguf-header (compile-frame (gcore/ordered-map  
                                  :gguf (string :ascii :length 4)  
                                  :version :int32-le
                                  :tensor_count :uint64-le 
                                  :metadata_kv_count :uint64-le)))

(defn metadata-frame [ct] 
  
  )


(defn parse [x]
  (let [buf (slurp-bytes x)
        header (decode gguf-header buf false)]
    ; after reading headers, we should be at 24 bytes
    (do (.position buf 24)
      
      )))



