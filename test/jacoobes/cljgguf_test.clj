(ns jacoobes.cljgguf-test
  (:require [clojure.test :refer :all]
            [jacoobes.cljgguf :refer :all]))

(deftest a-test
  (testing "reads urls"
    (is (some? (slurp-bytes "https://example.com")) )))
