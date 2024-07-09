(ns jacoobes.cljgguf
  (:require [gloss.io :as gio :refer [decode]])
  (:require [gloss.core :as gcore :refer [compile-frame string enum defcodec]])
  (:require [clojure.java.io :as io]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (gio/to-byte-buffer (.toByteArray out))))


(def gguf-header (compile-frame (gcore/ordered-map  
                                  :gguf (string :ascii :length 4)  
                                  :version :int32-le
                                  :tensor_count :uint64-le 
                                  :metadata_kv_count :uint64-le)))
(def gguf_string (compile-frame 
                   (string :ascii :length 65535 :delimiters [ \0 ])))

(defcodec tmetadata (enum :int32 GGUF_METADATA_VALUE_TYPE_UINT8
                                 GGUF_METADATA_VALUE_TYPE_INT8
                                 GGUF_METADATA_VALUE_TYPE_UINT16
                                 GGUF_METADATA_VALUE_TYPE_INT16
                                 GGUF_METADATA_VALUE_TYPE_UINT32
                                 GGUF_METADATA_VALUE_TYPE_INT32
                                 GGUF_METADATA_VALUE_TYPE_FLOAT32
                                 GGUF_METADATA_VALUE_TYPE_BOOL
                                 GGUF_METADATA_VALUE_TYPE_STRING
                                 GGUF_METADATA_VALUE_TYPE_ARRAY
                                 GGUF_METADATA_VALUE_TYPE_UINT64
                                 GGUF_METADATA_VALUE_TYPE_INT64
                                 GGUF_METADATA_VALUE_TYPE_FLOAT64))

(defn metadata-frame [mct] 
  
  )

(defn tensor-frame [tct] tct)

(defn parse [x]
  (let [buf (slurp-bytes x)
        header (decode gguf-header buf false)]
    ; after reading headers, we should be at 24 bytes
    (do (.position buf 24)
        (let [ ])
        )))



