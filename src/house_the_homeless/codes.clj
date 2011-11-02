(ns house-the-homeless.codes
  (:use [house-the-homeless.utils]
        [house-the-homeless.entities])
  (:require [house-the-homeless.settings :as settings]
            [appengine-magic.services.datastore :as ds]
            [clojure.contrib.base64 :as base64]))

(defn gen-code
  "Generate a random unique code"
  []
  (base64/encode-str (str (rand-int 100000))))

(defn code-correct-format?
  [code]
  (if code
    'true
    'false))
