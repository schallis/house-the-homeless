(ns house-the-homeless.routes
  (:use [noir.core :only [defpartial defpage render]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [link-to html5 include-css ordered-list unordered-list]]
        [hiccup.form-helpers :only [drop-down form-to label submit-button text-field text-area
                                    check-box]]
        [clojure.pprint :only [pprint]]
        [house-the-homeless.entities]
        [house-the-homeless.utils]
        [house-the-homeless.templates]
        [house-the-homeless.clients]
        [house-the-homeless.codes])
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [noir.session :as sess]
            [house-the-homeless.settings :as settings]
            [appengine-magic.services.user :as ui]
            [appengine-magic.services.datastore :as ds]))

;; TODO investigate reverse url lookup by function name (django style)
;; TODO investigate exception handling in Clojure (i.e. for integer
;; conversion)
;; TODO investigate auto-increment in gae (for user ids etc.)
;; TODO make hash more unique

;; TEST client pages (non-valid inputs)q
;; TEST invalid codes (strings, integers, string-ints, symbols)

(ds/defentity Code [^:key content redeemed])
(ds/defentity Event [^:key content
                     date])
(ds/defentity Client [status
                      creator
                      last-modifier
                      code
                      firstname
                      lastname
                      dob
                      ethnicity
                      nationality
                      ni-number
                      notes
                      ;; home-number
                      ;; mobile-number
                      ;; email
                      ;; marital-status
                      ;; ni-number
                      ;;case-notes
                      terms])
(ds/defentity Stay [date
                    status
                    notes
                    client-id])

(defn stay-template [stay]
  (:status stay))

(defn code-issued? 
  "Returns true if the specified code is found in the database"
  [code]
  (if (parse-int code)
    (> (count
        (ds/query
         :kind Code
         :filter (= :content (parse-int code))))
       0)
    'false))

(defn get-clients []
  "Return a list of clients visible to the current user"
  (if (is-admin?)
    (ds/query :kind Client)
    (try (ds/query :kind Client
                   :filter (= :creator (current-user-email)))
         (catch Exception e nil))))

(defn get-clients-count []
  "Return the number of clients"
  (if (is-admin?)
    (ds/query :kind Client
              :count-only? true)
    (try (ds/query :kind Client
                   :filter (= :creator (current-user-email))
                   :count-only? true)
         (catch Exception e nil))))

(defn get-stays [& [client-id]]
  "Return a list of stays for this client, unformatted"
  (if (nil? client-id)
    (ds/query :kind Stay)
    (ds/query :kind Stay
              :filter (= :client-id client-id))))

(defn get-stays-count [& [client-id]]
  "Return the number of stays for a client"
  (if (nil? client-id)
    (ds/query :kind Stay
              :count-only? true)
    (ds/query :kind Stay
              :filter (= :client-id client-id)
              :count-only? true)))

(defn get-client [pk]
  ;; TODO handle appropriately when pk is nil
  "Return a list of clients visible to the current user"
  (let [client (ds/retrieve Client pk)]
    (if (is-admin?)
      client
      (and (= (current-user-email)
              (:creator client))
           client))))

;;
;;
;; Forms
;;
;;

(defpartial code-field [code]
  [:tr [:td
        (vali/on-error :code error-item)
        (label "code" "Code: ")
        (text-field "code" code)]])

;;                       home-number
;;                       mobile-number
;;                       email
;;                       marital-status
;;                       ni-number
;;                       case-notes

(defpartial user-fields [{:keys [firstname lastname dob ethnicity
                                 nationality ni-number notes]}]
  ;; Print out the fields two per line
  (map #(rowify (first %) (second %))
       (partition-all 2
                      [[:td (vali/on-error :firstname error-item)
                        (label "firstname" "First name:")
                        (text-field "firstname" firstname)]
                       [:td
                        (vali/on-error :lastname error-item)
                        (label "lastname" "Last name:")
                        (text-field "lastname" lastname)]
                       [:td
                        (vali/on-error :dob error-item)
                        (label "dob" "Date of Birth:")
                        (text-field "dob" dob)]
                       [:td
                        (vali/on-error :ethnicity error-item)
                        (label "dob" "Ethnicity:")
                        (drop-down "ethnicity" settings/ethnicities ethnicity)]
                       [:td
                        (vali/on-error :nationality error-item)
                        (label "nationality" "Nationality:")
                        (text-field "nationality" nationality)]
                       [:td
                        (vali/on-error :ni-number error-item)
                        (label "ni-number" "NI Number:")
                        (text-field "ni-number" ni-number)]
                       [:td
                        (vali/on-error :notes error-item)
                        (label "notes" "Notes")
                        (text-area "notes" notes)]])))

(defpartial stay-fields [{:keys [date status notes]}]
  (map #(rowify (first %) (second %))
       (partition-all 2
                      [[:td (vali/on-error :date error-item)
                        (label "date" "Date:")
                        (text-field "date" date)]
                       [:td (vali/on-error :status status)
                        (label "status" "Status:")
                        (drop-down "status" settings/stay-statuses status)]
                       [:td (vali/on-error :notes notes)
                        (label "notes" "Notes:")
                        (text-area "notes" notes)]])))

(defpartial terms-field [terms]
  [:tr [:td
        (vali/on-error :terms error-item)
        (label "terms" (str "I accept the " (html (link-to "/terms" "terms and contitions"))))
        (check-box "terms" terms)]])

(defn valid-client? [{:keys [code firstname lastname dob terms]}]
  (if (not (is-admin?))
    (loop []
      ;; TODO check for valid format
      (vali/rule (and (parse-int code) (code-issued? (parse-int code)))
                 [:code "That code is not valid"])
      (vali/rule (vali/has-value? code)
                 [:code "You must supply a code"])
      (vali/rule (vali/has-value? terms)
                 [:terms "You must accept the terms and conditions"])))
  (vali/rule (vali/has-value? firstname)
             [:firstname "Your must supply a first name"])
  (vali/rule (vali/has-value? lastname)
             [:lastname "You must supply a last name"])
  (vali/rule (vali/has-value? dob)
             [:dob "You must supply a date of birth"])
  (not (vali/errors? :code :lastname :firstname :dob :terms)))

(defn valid-stay? [{:keys [date status]}]
  (vali/rule (vali/has-value? date)
             [:date "You must supply a date"])
  (vali/rule (vali/has-value? status)
             [:status "You must supply a valid status"])
  (not (vali/errors? :date :status)))

;;
;;
;; Pages
;;
;;

(defpage "/" []
  (resp/redirect "/welcome"))

(defpage "/welcome" []
  (welcome))

(defpage "/unauthorised" []
  (unauthorised))

(defpage "/client-not-found" []
  (client-not-found))

(defpage "/admin" []
  (layout "Admin"
          [:p "Use the menu on the left to create clients and codes"]))

(defpage "/codes" []
  (layout "Codes"
          (let [codes (ds/query :kind Code)]
            (html
             [:p (link-to "/code/new" "Generate new code")]
             [:table.tabular
              [:tr.heading
               [:td "Code"]
               [:td "Redeemed?"]]
              (map
               #(html [:tr
                       [:td (:content %)]
                       [:td (:redeemed %)]])
               codes)]))))

(defpage "/code/new" []
  (layout "New Code"
          (let [code (gen-code)]
            (ds/save! (Code. code "no"))
            (html
             [:p code]
             [:p (link-to "/code/new" "Generate another")]))))

(defpage "/clients" []
  (layout "Clients"
          (let [clients (get-clients)]
            (html
             [:p (link-to "/client/new" "Add new client")]
             (if (empty? clients)
               "There are no clients"
               (html
                [:table.tabular
                 [:tr.heading
                  [:td "ID"]
                  [:td "Name"]
                  [:td "Status"]
                  [:td "Stays"]]
                 (map
                  #(html [:tr
                          [:td (ds/key-id %)]
                          [:td (link-client %)]
                          [:td (:status %)]
                          [:td (colourise-stays
                                (get-stays-count (ds/key-id %))
                                settings/default-max-stay)]])
                  clients)]))))))

(defpage "/stays" []
  (layout "Stays"
          (let [stays (get-stays)]
            (if (empty? stays)
              "Noone has stayed yet"
              (html
               [:table.tabular
                [:tr.heading
                 [:td "Date"]
                 [:td "Status"]
                 [:td "Client ID"]]
                (map
                 #(html [:tr
                         [:td (:date %)]
                         [:td (:status %)]
                         [:td (:client-id %)]])
                 stays)])))))

(defpage [:POST "/client/:id/stay/new"] {id :id :as form}
  (let [int-id (parse-int id)
        client (get-client int-id)]
    (if client
      (and
       (ds/save! (Stay.
                   (:date form)
                   (:status form)
                   (:notes form)
                   int-id))
       (sess/flash-put! "Stay created successfully")
       (resp/redirect (str "/client/edit/" int-id "#stays")))
      (render "/client-not-found"))))

(defpage [:GET "/client/:id/stay/new"] {id :id :as form}
  ;; INFO will be POST
  
  (let [int-id (parse-int id)
        client (get-client int-id)]
    (layout "New Stay"
          (form-to [:post (str "/client/" int-id "/stay/new")]
                   [:table.form
                    (stay-fields form)
                    [:tr [:td
                          (submit-button "Add stay")
                          " or "
                          (link-to (str "/client/edit/" int-id "#stays") "Cancel")]]]))
    #_(if client
      (if (valid-stay? form)
        (loop []
          ;; insert stay entity with client as parent
          (str form))
        "invalid"
        #_(render (str "/client/" int-id "/stay/new")))
      (render "/client-not-found"))))

;; (defpage [:GET  "/client/:id"] {id :id}
;;   (let [int-id (parse-int id)
;;         client (get-client int-id)]
;;     (if client
;;       (layout (full-name client)
;;             (html
;;              (link-to (str "/client/edit/" int-id) "Edit details")
;;              [:table.form (user-fields client)]
;;              [:h3 "Stays"]
;;              (link-to (str "/client/" int-id "/stay/new") "New stay")
;;              (ordered-list (map stay-template (get-stays client)))))
;;     (render "/client-not-found"))))

(defpage [:GET "/client/edit/:id"] {:keys [posted id] :as env}
  (let [int-id (parse-int id)
        client (if (not posted)
                 (ds/retrieve Client int-id)
                 env)
        stays (get-stays int-id)]
    (if client
      (layout (full-name client)
              [:div#uitabs               
               [:ul
                [:li (link-to "#details" "Details")]
                [:li (link-to "#stays"
                              (html "Stays " "(" (get-stays-count int-id) ")"))]
                [:li (link-to "#metadata" "Metadata")]]
               [:div#details
                (form-to [:post (str "/client/edit/" int-id)]
                         [:table.form
                          [:tr
                           [:td
                            (label "status" "Status:")
                            (drop-down "status" settings/client-statuses (:status client))]]
                          (user-fields client)
                          [:tr [:td
                                (submit-button "Save")]]])]
               (if (is-admin?)
                 (html
                  [:div#stays
                   (if (empty? stays)
                     (html
                      [:table.tabular
                       [:tr.heading
                        [:td "Date"]
                        [:td "Status"]]]
                      [:p "This client has not stayed yet"])
                     (html
                      [:table.tabular
                       [:tr.heading
                        [:td "Date"]
                        [:td "Status"]]
                       (map
                        #(html [:tr
                                [:td (:date %)]
                                [:td (:status %)]])
                        stays)]))
                   [:p (link-to (str "/client/" int-id "/stay/new") "New stay")]]
                  [:div#metadata
                   [:p (str "Referred by " (:creator client))]
                   [:p (str "Last edited by " (:last-modifier client))]]
                  ))])
      (render "/client-not-found"))))

(defpage [:POST "/client/edit/:id"] {id :id :as form}
  (let [int-id (parse-int id)
        client (ds/retrieve Client int-id)]
    (if (valid-client? form)
      (and
       (ds/save! (conj client form {:last-modifier (current-user-email)}))
       (sess/flash-put! "Client updated successfully")
       (resp/redirect (str "/client/edit/" int-id)))
      (render "/client/edit/:id" (assoc form :posted 'true)))))

(defpage [:GET "/client/new"] {:keys [terms code] :as client}
  (layout "New Client"
          (form-to [:post "/client/new"]
                   [:table.form
                    (if (not (is-admin?))
                      (code-field code))
                    (user-fields client)
                    (if (not (is-admin?))
                      (terms-field terms))
                    [:tr [:td
                          (submit-button "Add client")
                          " or "
                          (link-to "/clients" "Cancel")]]])))

(defpage [:POST "/client/new"] {:as form}
  (if (valid-client? form)
    (loop []
      ;; TODO check for errors with insertion
      (ds/save! (Client. (first settings/client-statuses)
                         (current-user-email)
                         (current-user-email)
                         (:code form) 
                         (:firstname form)
                         (:lastname form)
                         (:dob form)
                         (:ethnicity form)
                         (:nationality form)
                         (:notes form)
                         (:ni-number form)
                         (:terms form)))
      (sess/flash-put! "Client created successfully")
      (layout "New Client"
              [:p (str "Success!")]
              [:p (link-to "/clients" "View all clients")
               " or "
               (link-to "/client/new" "Add another client")]))
    (render "/client/new" form)))
