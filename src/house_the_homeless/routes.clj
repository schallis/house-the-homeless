(ns house-the-homeless.routes
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers
        [clojure.pprint :only [pprint]])
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [appengine-magic.services.user :as ui]
            [appengine-magic.services.datastore :as ds]))

;; TODO investigate reverse url lookup by function name (django style)
;; TODO investigate exception handling in Clojure (i.e. for integer
;; conversion)
;; TODO investigate auto-increment in gae (for user ids etc.)
;; TODO filter "My Clients" based on author
;; TODO make hash more unique

;; TEST client pages (non-valid inputs)
;; TEST invalid codes (strings, integers, string-ints, symbols)

;;
;;
;; Entities
;;
;;

(ds/defentity Code [^:key content])
(ds/defentity Event [^:key content])
(ds/defentity Client [creator
                      code
                      firstname
                      lastname
                      dob
                      ethnicity
                      nationality
                      ;; home-number
                      ;; mobile-number
                      ;; email
                      ;; marital-status
                      ;; ni-number
                      ;;case-notes
                      terms])
(ds/defentity Stay [^:key date])

;;
;;
;; Utilities
;;
;;

(defn debug-client
  "Print out the map representing a client"
  [client]
  (with-out-str (pprint client)))

(def ethnicities
  ["White"
   "African"
   "Asian"
   "Other"])

(defn full-name
  "Take a client entity and return the full name as a string"
  [client]
  (str (:firstname client) " " (:lastname client)))

(defn gen-code
  "Generate a random unique code"
  []
  (rand-int 100))

(defn code-correct-format?
  [code]
  (if (parse-int code)
    'true
    'false))

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

(defn link-client
  "Create a link from a client entity"
  [client]
  (link-to (str "/client/" (ds/key-id client)) (full-name client)))

(defn parse-int
  [string]
  (try (Integer. string)
       (catch Exception e nil)))

(defn get-clients []
  "Return a list of clients visible to the current user"
  (if (is-admin?)
    (ds/query :kind Client)
    (try (ds/query :kind Client
                   :filter (= :creator (current-user-email)))
         (catch Exception e nil))))

(defn get-client [pk]
  "Return a list of clients visible to the current user"
  (let [client (ds/retrieve Client pk)]
    (if (is-admin?)
      client
      (and (= (current-user-email)
              (:creator client))
           client))))

;;
;;
;; Templates
;;
;;

(defpartial side-bar []
  [:div#sidebar
   (if (ui/user-logged-in?)
     [:ul
      [:li "Logged in as " (ui/current-user)]
      [:li (link-to (ui/logout-url) "Logout")]
      [:li "Clients"
       [:ul
        [:li (link-to "/clients" "My Clients")]
        [:li (link-to "/client/new" "New Client")]]]
      (if (is-admin?)
        [:li "Codes"
         [:ul
          [:li (link-to "/codes" "All Codes")]
          [:li (link-to "/code/new" "New Code")]]])]
     [:ul
      [:li "Not logged in"]
      [:li (link-to (ui/login-url) "Login")]]
     )])

(defpartial unauthorised []
  (html5
     [:head
      [:title "Unauthorised"]
      (include-css "/css/main.css")]
     [:body
      [:header [:h1 "Unauthorised"]]
      [:p "You must log in before viewing this page"]]))

;; Protected admin page template
(defpartial layout [title & content]
  (if (ui/user-logged-in?)
    (html5
     [:head
      [:title title]
      (include-css "/css/main.css")]
     [:body
      [:header [:h1 "House the Homeless"]]
      (side-bar)
      (html
       [:div#main
        [:h2 title]
        content])])
    ;; Unauthorised
    (unauthorised)))
  
;; Unprotected welcome page
(defpartial welcome []
  (html5
   [:head
    [:title "Welcome"]
    (include-css "/css/main.css")]
   [:body
    [:header [:h1 "House the Homeless"]]
    (html
     [:div#main
      [:h2 "Welcome"]
      (side-bar)
      [:p "Welcome"]])]))

(defpartial error-item [[first-error]]
  [:p.error first-error])

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
                                 nationality]}]
   [:tr [:td
         (vali/on-error :firstname error-item)
         (label "firstname" "First name: ")
         (text-field "firstname" firstname)]]
   [:tr [:td
    (vali/on-error :lastname error-item)
    (label "lastname" "Last name: ")
    (text-field "lastname" lastname)]]
   [:tr [:td
    (vali/on-error :dob error-item)
    (label "dob" "Date of Birth: ")
    (text-field "dob" dob)]]
   [:tr [:td
    (vali/on-error :ethnicity error-item)
    (label "dob" "Ethnicity: ")
    (drop-down "ethnicity" ethnicities ethnicity)]]
   [:tr [:td
    (vali/on-error :nationality error-item)
    (label "nationality" "Nationality: ")
    (text-field "nationality" nationality)]])

(defpartial terms-field [terms]
  [:tr [:td
        (vali/on-error :terms error-item)
        (label "terms" "Do you accept the terms and conditions? ")
        (check-box "terms" terms)]])

(defn valid? [{:keys [code firstname lastname dob terms]}]
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
             (ordered-list (map link-client clients))))))

(defpage "/client/:id" {id :id}
  (let [int-id (parse-int id)
        client (get-client int-id)]
    (if client
      (layout (full-name client)
            (html
             (link-to (str "/client/edit/" int-id) "Edit")
             [:p (debug-client client)]))
    (render "/unauthorised"))))

(defpage "/client/edit/:id" {:keys [posted id] :as env}
  (let [int-id (parse-int id)
        client (if (not posted)
                 (ds/retrieve Client int-id)
                 env)]
    (layout (full-name client)
            (form-to [:post (str "/client/edit/" int-id)]
                     [:table
                      (user-fields client)
                      [:tr [:td
                            (submit-button "Save")
                            " or "
                            (link-to (str "/client/" int-id) "Cancel")]]]))))

(defpage [:post "/client/edit/:id"] {id :id :as form}
  (let [int-id (parse-int id)
        client (ds/retrieve Client int-id)]
    (if (valid? form)
      (and
       (ds/save! (conj client form))
       (resp/redirect (str "/client/" int-id)))
      (render "/client/edit/:id" (assoc form :posted 'true)))))

(defpage "/client/new" {:keys [terms code] :as client}
  (layout "New Client"
          (form-to [:post "/client/new"]
                   [:table
                    (if (not (is-admin?))
                      (code-field code))
                    (user-fields client)
                    (if (not (is-admin?))
                      (terms-field terms))
                    [:tr [:td
                          (submit-button "Add client")
                          " or "
                          (link-to "/clients" "Cancel")]]])))

(defpage [:post "/client/new"] {:as form}
  (if (valid? form)
    (loop []
      ;; TODO check for errors with insertion
      (ds/save! (Client. (current-user-email)
                         (:code form) 
                         (:firstname form)
                         (:lastname form)
                         (:dob form)
                         (:terms form)
                         (:ethnicity form)
                         (:nationality form)))
      (layout "New Client"
              [:p (str "Success!" form)]
              [:p (link-to "/clients" "View all clients")
               " or "
               (link-to "/client/new" "Add another client")]))
    (render "/client/new" form)))
