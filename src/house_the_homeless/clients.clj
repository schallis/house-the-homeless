(ns house-the-homeless.clients
  (:use [noir.core :only [defpartial defpage render]]
        [clojure.pprint :only [pprint]]
        [hiccup.page-helpers :only [link-to]]
        [house-the-homeless.entities]
        [house-the-homeless.utils])
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [appengine-magic.services.user :as ui]
            [appengine-magic.services.datastore :as ds]))

(defn link-client
  "Create a link from a client entity"
  [client]
  (link-to (str "/client/edit/" (ds/key-id client)) (full-name client)))
