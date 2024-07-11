(ns jacoobes.cljgguf
  (:require [gloss.io :as gio :refer [decode]])
  (:require [gloss.core :as gcore :refer [compile-frame 
                                          string 
                                          prefix
                                          finite-frame
                                          enum 
                                          defcodec
                                          header ]])
  (:require [clojure.java.io :as io]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (gio/to-byte-buffer (.toByteArray out))))



(def gguf_string (finite-frame (prefix :uint64-le) 
                               (string :ascii)))

(defcodec gguf_metadata_t (enum :int32 
                                :GGUF_METADATA_VALUE_TYPE_UINT8
                                :GGUF_METADATA_VALUE_TYPE_INT8
                                :GGUF_METADATA_VALUE_TYPE_UINT16
                                :GGUF_METADATA_VALUE_TYPE_INT16
                                :GGUF_METADATA_VALUE_TYPE_UINT32
                                :GGUF_METADATA_VALUE_TYPE_INT32
                                :GGUF_METADATA_VALUE_TYPE_FLOAT32
                                :GGUF_METADATA_VALUE_TYPE_BOOL
                                :GGUF_METADATA_VALUE_TYPE_STRING
                                :GGUF_METADATA_VALUE_TYPE_ARRAY
                                :GGUF_METADATA_VALUE_TYPE_UINT64
                                :GGUF_METADATA_VALUE_TYPE_INT64
                                :GGUF_METADATA_VALUE_TYPE_FLOAT64))

(defcodec GGUF_METADATA_VALUE_TYPE_UINT8 {:type :GGUF_METADATA_VALUE_TYPE_UINT8 })
(defcodec GGUF_METADATA_VALUE_TYPE_INT8 {:type :GGUF_METADATA_VALUE_TYPE_INT8 })
(defcodec GGUF_METADATA_VALUE_TYPE_UINT16 {:type :GGUF_METADATA_VALUE_TYPE_UINT16 })
(defcodec GGUF_METADATA_VALUE_TYPE_INT16 {:type :GGUF_METADATA_VALUE_TYPE_INT16 })
(defcodec GGUF_METADATA_VALUE_TYPE_UINT32 {:type :GGUF_METADATA_VALUE_TYPE_UINT32 })
(defcodec GGUF_METADATA_VALUE_TYPE_INT32 {:type :GGUF_METADATA_VALUE_TYPE_INT32 })
(defcodec GGUF_METADATA_VALUE_TYPE_FLOAT32 {:type :GGUF_METADATA_VALUE_TYPE_FLOAT32 })
(defcodec GGUF_METADATA_VALUE_TYPE_BOOL {:type :GGUF_METADATA_VALUE_TYPE_BOOL })
(defcodec GGUF_METADATA_VALUE_TYPE_STRING {:type :GGUF_METADATA_VALUE_TYPE_STRING })
(defcodec GGUF_METADATA_VALUE_TYPE_ARRAY {:type :GGUF_METADATA_VALUE_TYPE_ARRAY })
(defcodec GGUF_METADATA_VALUE_TYPE_UINT64 {:type :GGUF_METADATA_VALUE_TYPE_UINT64 })
(defcodec GGUF_METADATA_VALUE_TYPE_INT64 {:type :GGUF_METADATA_VALUE_TYPE_INT64 })
(defcodec GGUF_METADATA_VALUE_TYPE_FLOAT64 {:type :GGUF_METADATA_VALUE_TYPE_FLOAT64 })


(def metadata_pair 
  (header gguf_metadata_t {:GGUF_METADATA_VALUE_TYPE_UINT8 GGUF_METADATA_VALUE_TYPE_UINT8 
                           :GGUF_METADATA_VALUE_TYPE_INT8 GGUF_METADATA_VALUE_TYPE_INT8 
                           :GGUF_METADATA_VALUE_TYPE_UINT16 GGUF_METADATA_VALUE_TYPE_UINT16 
                           :GGUF_METADATA_VALUE_TYPE_INT16 GGUF_METADATA_VALUE_TYPE_INT16 
                           :GGUF_METADATA_VALUE_TYPE_UINT32 GGUF_METADATA_VALUE_TYPE_UINT32 
                           :GGUF_METADATA_VALUE_TYPE_INT32 GGUF_METADATA_VALUE_TYPE_INT32 
                           :GGUF_METADATA_VALUE_TYPE_FLOAT32 GGUF_METADATA_VALUE_TYPE_FLOAT32 
                           :GGUF_METADATA_VALUE_TYPE_BOOL GGUF_METADATA_VALUE_TYPE_BOOL 
                           :GGUF_METADATA_VALUE_TYPE_STRING GGUF_METADATA_VALUE_TYPE_STRING
                           :GGUF_METADATA_VALUE_TYPE_ARRAY GGUF_METADATA_VALUE_TYPE_ARRAY 
                           :GGUF_METADATA_VALUE_TYPE_UINT64 GGUF_METADATA_VALUE_TYPE_UINT64 
                           :GGUF_METADATA_VALUE_TYPE_INT64 GGUF_METADATA_VALUE_TYPE_INT64 
                           :GGUF_METADATA_VALUE_TYPE_FLOAT64 GGUF_METADATA_VALUE_TYPE_FLOAT64 } :type))


(def gguf-header (compile-frame (gcore/ordered-map :gguf (string :ascii :length 4)  
                                                   :version :int32-le
                                                   :tensor_count :uint64-le 
                                                   :metadata_kv_count :uint64-le)))

(defn metadata-frame [metadatact] )

(defn tensor-frame [tensorct] tct)

(defn parse [x]
  (let [buf (slurp-bytes x)
        header (decode gguf-header buf false)]
    ; after reading headers, we should be at 24 bytes
    (do (.position buf 24)
        ; to fix
        )))



