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

(ds/defentity Code [^:key content])
(ds/defentity Event [^:key content
                     date])
(ds/defentity Client [creator
                      code
                      firstname
                      lastname
                      dob
                      ethnicity
                      nationality
                      notes
                      ;; home-number
                      ;; mobile-number
                      ;; email
                      ;; marital-status
                      ;; ni-number
                      ;;case-notes
                      terms])
(ds/defentity Stay [date
                    status])

(defn get-stays [client]
  "Return a list of stays for this client, unformatted"
  [])

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

(defn get-num-stays
  "Return the number of stays for a client"
  [client]
  0)

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
                                 nationality notes]}]
  [:h3 "Details"]
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
                    (vali/on-error :notes error-item)
                    (label "notes" "Notes")
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
             [:p (link-to "/new-code" "Generate new code")]
             (ordered-list (map #(:content %) codes))))))

(defpage "/code/new" []
  (layout "New Code"
          (let [code (gen-code)]
            (ds/save! (Code. code))
            (html
             [:p code]
             [:p (link-to "/new-code" "Generate another")]))))

(defpage "/clients" []
  (layout "Clients"
          (let [clients (get-clients)]
            (html
             [:p (link-to "/client/new" "Add new client")]
             #_(ordered-list (map link-client clients))
             [:table.tabular
              [:tr.heading
               [:td "Name"]
               [:td "Stays"]]
              (map
               #(html [:tr
                       [:td (link-client %)]
                       [:td (colourise-stays (get-num-stays %) settings/default-max-stay)]])
               clients)]))))

#_(defpage [:GET "/client/:id/stay/new"] {id :id}
  (let [int-id (parse-int id)
        client (get-client int-id)]
    (if client
      "Adding stay ... this will probably be an ajax call"
      (let [parent (ds/retrieve Client id)
            child (ds/new* Stay ["a date" "Stayed"] :parent parent)]
        (ds/with-transaction
          (ds/save! (assoc parent :members (conj (:children child) (ds/key-id child))))
          (ds/save! child)))
      (render "/client-not-found"))))

(defpage [:GET "/client/:id/stay/new"] {id :id :as form}
  ;; INFO will be POST
  (let [int-id (parse-int id)
        client (get-client int-id)]
    (if client
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
                 env)]
    (if client
      (layout (full-name client)
              (form-to [:post (str "/client/edit/" int-id)]
                       [:table.form
                        (user-fields client)
                        [:tr [:td
                              (submit-button "Save")]]])
              (if (is-admin?)
                (html
                 [:h3 "Stays " "(" (colourise-stays 0 settings/default-max-stay) ")"]
                 (link-to (str "/client/" int-id "/stay/new") "New stay")
                 [:table.tabular
                  [:tr.heading
                   [:td "Date"]
                   [:td "Status"]]
                  [:tr
                   [:td "asd"]
                   [:td "sdsd"]]
                  [:tr
                   [:td "asd"]
                   [:td "sdsd"]]
                  [:tr
                   [:td "asd"]
                   [:td "sdsd"]]]
                 [:h3 "Metadata"]
                 [:p (str  "Referred by " (:creator client))])))
      (render "/client-not-found"))))

(defpage [:POST "/client/edit/:id"] {id :id :as form}
  (let [int-id (parse-int id)
        client (ds/retrieve Client int-id)]
    (if (valid-client? form)
      (and
       (ds/save! (conj client form))
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
      (ds/save! (Client. (current-user-email)
                         (:code form) 
                         (:firstname form)
                         (:lastname form)
                         (:dob form)
                         (:ethnicity form)
                         (:nationality form)
                         (:notes form)
                         (:terms form)))
      (layout "New Client"
              [:p (str "Success!" form)]
              [:p (link-to "/clients" "View all clients")
               " or "
               (link-to "/client/new" "Add another client")]))
    (render "/client/new" form)))
