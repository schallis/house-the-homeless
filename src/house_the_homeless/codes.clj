(ns house-the-homeless.codes
  (:use [house-the-homeless.utils]
        [house-the-homeless.entities])
  (:require [house-the-homeless.settings :as settings]
            [appengine-magic.services.datastore :as ds]))

(defn gen-code
  "Generate a random unique code"
  []
  (rand-int 100))

(defn code-correct-format?
  [code]
  (if (parse-int code)
    'true
    'false))
