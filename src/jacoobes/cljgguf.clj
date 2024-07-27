(ns jacoobes.cljgguf
  (:require [gloss.io :as gio])
  (:require [gloss.core :as gcore :refer [compile-frame string prefix
                                          finite-frame repeated enum 
                                          defcodec header ]])
  (:require [clojure.java.io :as io]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (gio/to-byte-buffer (.toByteArray out))))


(declare gguf_metadata_value)

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
(defcodec GGUF_METADATA_VALUE_TYPE_ARRAY {:type :GGUF_METADATA_VALUE_TYPE_ARRAY 
                                          :val (gcore/ordered-map :element-type gguf_metadata_t
                                                                  :elements (repeated gguf_metadata_value :prefix :uint64-le)) })

(defcodec gguf_metadata_value (header gguf_metadata_t { :GGUF_METADATA_VALUE_TYPE_UINT8 GGUF_METADATA_VALUE_TYPE_UINT8 
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
                                                        :GGUF_METADATA_VALUE_TYPE_FLOAT64 GGUF_METADATA_VALUE_TYPE_FLOAT64} :type))
(defcodec metadata_pair 
  (gcore/ordered-map :key gguf_string  
                     :value gguf_metadata_value ))


(def metadata 
 (repeated metadata_pair :prefix :uint64-le))

(defcodec ggml-type
  (enum :uint32-le
    {:GGML_TYPE_F32  0
     :GGML_TYPE_F16  1
     :GGML_TYPE_Q4_0 2
     :GGML_TYPE_Q4_1 3 }))

(defcodec tensor-info-codec
  (gcore/ordered-map
    :name gguf_string
    :n-dimensions :uint32-le
    :dimensions (header :n-dimensions
                        (fn [m] (:n-dimensions m))
                        (fn [count] (repeated :uint64-le count)))
    :type ggml-type
    :offset :uint64-le))

(defcodec gguf-file
  (gcore/ordered-map
    :magic (string :ascii :length 4)
    :version :int32-le
    :tensor-count :uint64-le
    :metadata (repeated metadata_pair :prefix :uint64-le)))

(defn parse 
  ([x] (if-let [resource (io/resource x)]
         (let [buf (slurp-bytes resource)]
           (do (gio/decode gguf-file buf false) (println (.position buf)) ))
           
         (println "Resource not found:" x)))
  ([] (parse "example.gguf")))

