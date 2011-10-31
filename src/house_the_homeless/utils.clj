(ns house-the-homeless.utils
  (:use [noir.core :only [defpartial defpage render]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [link-to html5 include-css ordered-list]]
        [hiccup.form-helpers :only [drop-down form-to label submit-button text-field
                                    check-box]]
        [clojure.pprint :only [pprint]])
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [appengine-magic.services.user :as ui]
            [appengine-magic.services.datastore :as ds]))

(defn debug-client
  "Print out the map representing a client"
  [client]
  (with-out-str (pprint client)))

(defn full-name
  "Take a client entity and return the full name as a string"
  [client]
  (str (:firstname client) " " (:lastname client)))

(defn parse-int
  [string]
  (try (Integer. string)
       (catch Exception e nil)))

(defn is-admin?
  "Returns true if the current user is logged in and admin"
  []
  (and (ui/user-logged-in?) (ui/user-admin?)))

(defn current-user-email
  "Return the email of the current user or nil"
  []
  (let [user (ui/current-user)]
    (if user
      (ui/get-email user)
      nil)))

(defn rowify [a b] [:tr a b])

(defn two-col [fields]
  (map #(rowify (first %) (second %))
       (partition-all 2 fields)))

(defn colourise-stays
  "Return a colour formatted integer based on whether the number of
  allowed days has been exceeded"
  [current allowed]
  (cond (> current allowed) [:span.red current]
        (== current allowed) [:span.yellow current]
        (< current allowed) [:span.green current]))
