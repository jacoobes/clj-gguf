(ns jacoobes.cljgguf
  (:require [gloss.io :as gi])
  (:require [gloss.core :as g :refer [compile-frame string prefix
                                      finite-frame repeated enum 
                                      defcodec header ]])
  (:require [clojure.java.io :as io]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (gi/to-byte-buffer (.toByteArray out))))


(defcodec gguf_string (finite-frame :uint64-le (string :utf-8)))
(defcodec gguf_metadata_t (enum :uint32-le {:GGUF_METADATA_VALUE_TYPE_UINT8 0
                                            :GGUF_METADATA_VALUE_TYPE_INT8 1
                                            :GGUF_METADATA_VALUE_TYPE_UINT16 2
                                            :GGUF_METADATA_VALUE_TYPE_INT16 3
                                            :GGUF_METADATA_VALUE_TYPE_UINT32 4
                                            :GGUF_METADATA_VALUE_TYPE_INT32 5
                                            :GGUF_METADATA_VALUE_TYPE_FLOAT32 6
                                            :GGUF_METADATA_VALUE_TYPE_BOOL 7
                                            :GGUF_METADATA_VALUE_TYPE_STRING 8
                                            :GGUF_METADATA_VALUE_TYPE_ARRAY 9
                                            :GGUF_METADATA_VALUE_TYPE_UINT64 10
                                            :GGUF_METADATA_VALUE_TYPE_INT64 11
                                            :GGUF_METADATA_VALUE_TYPE_FLOAT64 12 }))


(defcodec GGUF_METADATA_VALUE_TYPE_UINT8 {:type :GGUF_METADATA_VALUE_TYPE_UINT8 :val :ubyte })
(defcodec GGUF_METADATA_VALUE_TYPE_INT8 {:type :GGUF_METADATA_VALUE_TYPE_INT8 :val :byte })
(defcodec GGUF_METADATA_VALUE_TYPE_UINT16 {:type :GGUF_METADATA_VALUE_TYPE_UINT16 :val :uint16-le })
(defcodec GGUF_METADATA_VALUE_TYPE_INT16 {:type :GGUF_METADATA_VALUE_TYPE_INT16 :val :int16-le })
(defcodec GGUF_METADATA_VALUE_TYPE_UINT32 {:type :GGUF_METADATA_VALUE_TYPE_UINT32 :val :uint32-le })
(defcodec GGUF_METADATA_VALUE_TYPE_INT32 {:type :GGUF_METADATA_VALUE_TYPE_INT32 :val :int32-le })
(defcodec GGUF_METADATA_VALUE_TYPE_FLOAT32 {:type :GGUF_METADATA_VALUE_TYPE_FLOAT32 :val :float32-le })
(defcodec GGUF_METADATA_VALUE_TYPE_BOOL {:type :GGUF_METADATA_VALUE_TYPE_BOOL :val :byte })
(defcodec GGUF_METADATA_VALUE_TYPE_STRING {:type :GGUF_METADATA_VALUE_TYPE_STRING :val gguf_string })
(defcodec GGUF_METADATA_VALUE_TYPE_UINT64 {:type :GGUF_METADATA_VALUE_TYPE_UINT64 :val :uint64-le })
(defcodec GGUF_METADATA_VALUE_TYPE_INT64 {:type :GGUF_METADATA_VALUE_TYPE_INT64 :val :int64-le })
(defcodec GGUF_METADATA_VALUE_TYPE_FLOAT64 {:type :GGUF_METADATA_VALUE_TYPE_FLOAT64 :val :float64-le })

(def GGUF_METADATA_VALUE_TYPE_ARRAY)
(def gguf-ty->code {:GGUF_METADATA_VALUE_TYPE_UINT8 GGUF_METADATA_VALUE_TYPE_UINT8 
                    :GGUF_METADATA_VALUE_TYPE_INT8 GGUF_METADATA_VALUE_TYPE_INT8 
                    :GGUF_METADATA_VALUE_TYPE_UINT16 GGUF_METADATA_VALUE_TYPE_UINT16 
                    :GGUF_METADATA_VALUE_TYPE_INT16 GGUF_METADATA_VALUE_TYPE_INT16 
                    :GGUF_METADATA_VALUE_TYPE_UINT32 GGUF_METADATA_VALUE_TYPE_UINT32 
                    :GGUF_METADATA_VALUE_TYPE_INT32 GGUF_METADATA_VALUE_TYPE_INT32 
                    :GGUF_METADATA_VALUE_TYPE_FLOAT32 GGUF_METADATA_VALUE_TYPE_FLOAT32 
                    :GGUF_METADATA_VALUE_TYPE_BOOL GGUF_METADATA_VALUE_TYPE_BOOL 
                    :GGUF_METADATA_VALUE_TYPE_STRING GGUF_METADATA_VALUE_TYPE_STRING
                    :GGUF_METADATA_VALUE_TYPE_UINT64 GGUF_METADATA_VALUE_TYPE_UINT64 
                    :GGUF_METADATA_VALUE_TYPE_INT64 GGUF_METADATA_VALUE_TYPE_INT64 
                    :GGUF_METADATA_VALUE_TYPE_FLOAT64 GGUF_METADATA_VALUE_TYPE_FLOAT64
                    :GGUF_METADATA_VALUE_TYPE_ARRAY GGUF_METADATA_VALUE_TYPE_ARRAY})

(defcodec gguf_metadata_value (header gguf_metadata_t gguf-ty->code :type))

(def GGUF_METADATA_VALUE_TYPE_ARRAY 
  (g/compile-frame {:type :GGUF_METADATA_VALUE_TYPE_ARRAY 
                    :val (header [gguf_metadata_t :uint64-le] 
                                 (fn [[ty len]]
                                    (g/compile-frame (repeat len (gguf-ty->code ty))))
                                 (fn [body] 
                                   )) 
                    
                    }))

(defcodec metadatap 
  (g/ordered-map :key gguf_string  
                 :value gguf_metadata_value))


(defcodec ggml-type
  (enum :uint32-le
    {:GGML_TYPE_F32      0 :GGML_TYPE_F16        1
     :GGML_TYPE_Q4_0     2 :GGML_TYPE_Q4_1       3 
     :GGML_TYPE_Q5_0     6, :GGML_TYPE_Q5_1      7,
     :GGML_TYPE_Q8_0     8, :GGML_TYPE_Q8_1      9,
     :GGML_TYPE_Q2_K     10, :GGML_TYPE_Q3_K     11,
     :GGML_TYPE_Q4_K     12, :GGML_TYPE_Q5_K     13,
     :GGML_TYPE_Q6_K     14, :GGML_TYPE_Q8_K     15,
     :GGML_TYPE_IQ2_XXS  16, :GGML_TYPE_IQ2_XS   17,
     :GGML_TYPE_IQ3_XXS  18, :GGML_TYPE_IQ1_S    19,
     :GGML_TYPE_IQ4_NL   20, :GGML_TYPE_IQ3_S    21,
     :GGML_TYPE_IQ2_S    22, :GGML_TYPE_IQ4_XS   23,
     :GGML_TYPE_I8       24, :GGML_TYPE_I16      25,
     :GGML_TYPE_I32      26, :GGML_TYPE_I64      27,
     :GGML_TYPE_F64      28, :GGML_TYPE_IQ1_M    29,
     :GGML_TYPE_COUNT    30 }))

(defcodec tensor-info-codec
  (g/ordered-map
    :name gguf_string
    :dimensions (repeated :uint64-le :prefix :uint32-le)
    :type ggml-type
    :offset :uint64-le))

(defcodec gguf-header
  (g/ordered-map
    :magic (string :ascii :length 4)
    :version :int32-le
    :tensor-ct :uint64-le
    :metadata-ct :uint64-le))

(defn gguf-map [head] ; there has to be a better way to do this
  (g/ordered-map 
    :magic (:magic head)
    :version (:version head)
    :metadata (repeat (:metadata-ct head) metadatap)
    :tensor-info (repeat (:tensor-ct head) tensor-info-codec)))

(defcodec gguf-file
  (g/header gguf-header 
            (fn [header] (g/compile-frame (gguf-map header) ))
            (fn [body] body )))

(defn parse 
  ([x] (if-let [resource (io/resource x)]
         (let [buf (slurp-bytes resource)
              decoded (gi/decode gguf-file buf false) ]
            decoded)
         (println "Resource not found:" x)))
  ([] (parse "example.gguf")))

