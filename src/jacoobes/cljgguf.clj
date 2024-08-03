(ns jacoobes.cljgguf
  (:require [byte-streams :as bs])
  (:require [gloss.io :as gi])

  (:require [gloss.core :as g :refer [compile-frame string prefix
                                      finite-frame repeated enum 
                                      defcodec header]])
  (:require [clojure.java.io :as io]))


(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (bs/convert (io/input-stream x)
              (bs/seq-of java.nio.ByteBuffer)))


(defcodec gguf_string 
  (finite-frame :uint64-le (string :utf-8)))

(defcodec gguf_metadata_t (enum :uint32-le {:gguf_type_uint8 0
                                            :gguf_type_int8 1
                                            :gguf_type_uint16 2
                                            :gguf_type_int16 3
                                            :gguf_type_uint32 4
                                            :gguf_type_int32 5
                                            :gguf_type_float32 6
                                            :gguf_type_bool 7
                                            :gguf_type_string 8
                                            :gguf_type_array 9
                                            :gguf_type_uint64 10
                                            :gguf_type_int64 11
                                            :gguf_type_float64 12 }))


(defcodec gguf_type_uint8 {:type :gguf_type_uint8 :val :ubyte })
(defcodec gguf_type_int8 {:type :gguf_type_int8 :val :byte })
(defcodec gguf_type_bool {:type :gguf_type_bool :val :byte })
(defcodec gguf_type_string {:type :gguf_type_string :val gguf_string })

(defcodec gguf_type_uint16  {:type :gguf_type_uint16 :val :uint16-le })
(defcodec gguf_type_int16   {:type :gguf_type_int16 :val :int16-le   })
(defcodec gguf_type_uint32  {:type :gguf_type_uint32 :val :uint32-le })
(defcodec gguf_type_int32   {:type :gguf_type_int32 :val :int32-le   })
(defcodec gguf_type_float32 {:type :gguf_type_float32 :val :float32-le })
(defcodec gguf_type_uint64  {:type :gguf_type_uint64 :val :uint64-le })
(defcodec gguf_type_int64   {:type :gguf_type_int64 :val :int64-le })
(defcodec gguf_type_float64 {:type :gguf_type_float64 :val :float64-le })

(def gguf_type_array)
(def gguf-ty->code {:gguf_type_uint8 gguf_type_uint8 
                    :gguf_type_int8 gguf_type_int8 
                    :gguf_type_uint16 gguf_type_uint16 
                    :gguf_type_int16 gguf_type_int16 
                    :gguf_type_uint32 gguf_type_uint32 
                    :gguf_type_int32 gguf_type_int32 
                    :gguf_type_float32 gguf_type_float32 
                    :gguf_type_bool gguf_type_bool 
                    :gguf_type_string gguf_type_string
                    :gguf_type_uint64 gguf_type_uint64 
                    :gguf_type_int64 gguf_type_int64 
                    :gguf_type_float64 gguf_type_float64
                    :gguf_type_array gguf_type_array})

(defcodec gguf_metadata_value (header gguf_metadata_t gguf-ty->code :type))

(def gguf_type_array 
  (g/compile-frame {:type :gguf_type_array 
                    :val (header [gguf_metadata_t :uint64-le] 
                                 (fn [[ty len]]
                                    (g/compile-frame (repeat len (gguf-ty->code ty))))
                                 (fn [body] [(->> body first :type) (count body)] )) }))

(defcodec metadatap 
  (g/ordered-map :key gguf_string  
                 :value gguf_metadata_value))

(defcodec ggml-type
  (enum :uint32-le
    {:GGML_TYPE_F32      0   :GGML_TYPE_F16        1
     :GGML_TYPE_Q4_0     2   :GGML_TYPE_Q4_1       3 
     :GGML_TYPE_Q5_0     6,  :GGML_TYPE_Q5_1      7,
     :GGML_TYPE_Q8_0     8,  :GGML_TYPE_Q8_1      9,
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
            (fn [body] { :magic (body :magic)
                         :version (body :version)
                         :tensor-ct  (count (body :tensor-info))
                         :metadata-ct (count (body :metadata)) })))

(defn decode [src] 
  "Decodes a gguf file metadata into a clojure map. 
   Accepts remote resources, urls. relative paths (your resources path)
   Example:
   (decode \"example.gguf\") ; decode from resources

   (decode \"~/.cache/gpt4all/nomic-embed-text-v1.5.f16.gguf\")"
  (if-let [resource (io/resource src)]
     (let [buf (slurp-bytes resource)]
        (gi/decode gguf-file buf false))
     (let [buf (slurp-bytes src)]
        (gi/decode gguf-file buf false))))

(defn encode [file-data]
  (gi/encode gguf-file file-data))
