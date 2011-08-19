(ns house-the-homeless.routes
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [appengine-magic.services.user :as ui]
            [appengine-magic.services.datastore :as ds]))

;; TODO investigate reverse url lookup by function name (django style)
;; TODO investigate exception handling in Clojure (i.e. for integer
;; conversion)
;; TODO investigate auto-increment in gae (for user ids etc.)
;; TODO filter "My Clients" based on author

;;
;;
;; Entities
;;
;;

(ds/defentity Code [^:key content])
(ds/defentity Event [^:key content])
(ds/defentity Client [^:key firstname])
(ds/defentity Stay [^:key date])

;;
;;
;; Utilities
;;
;;

(defn gen-code
  "Generate a random unique code"
  []
  (rand-int 100))

(defn code-issued? 
  "Returns true if the specified code is found in the database"
  [code]
  (> (count
      (ds/query
       :kind Code
       :filter (= :content (Integer. code))))
     0))

(defn is-admin?
  "Returns true if the current user is logged in and admin"
  []
  (and (ui/user-logged-in?) (ui/user-admin?)))


(defn link-client
  "Create a link from a client entity"
  [client]
  (link-to (str "/client/" (:firstname client)) (:firstname client)))

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

(defpartial layout [title & content]
  (html5
   [:head
    [:title title]
    (include-css "/css/main.css")]
   [:body
    [:h1 "House the Homeless"]
    (side-bar)
    (html
     [:h2 title]
     content)]))

(defpartial error-item [[first-error]]
  [:p.error first-error])

;;
;;
;; Forms
;;
;;

(defpartial code-field [code]
  (vali/on-error :code error-item)
  (label "code" "Code: ")
  (text-field "code" code))

(defpartial user-fields [{:keys [firstname lastname]}]
  (vali/on-error :firstname error-item)
  (label "firstname" "First name: ")
  (text-field "firstname" firstname)
  (vali/on-error :lastname error-item)
  (label "lastname" "Last name: ")
  (text-field "lastname" lastname))

(defn valid? [{:keys [code firstname lastname]}]
  (if (not (is-admin?))
    (or
     ;; TODO check for valid format
     (vali/rule (code-issued? code)
                [:code "That code is not valid"])
     (vali/rule (vali/has-value? code)
                [:code "You must supply a code"])))
  (vali/rule (vali/has-value? firstname)
             [:firstname "Your first supply a first name"])
  (vali/rule (vali/has-value? lastname)
             [:lastname "You must supply a last name"])
  (not (vali/errors? :code :lastname :firstname)))

;;
;;
;; Pages
;;
;;

(defpage "/" []
  (resp/redirect "/welcome"))

(defpage "/welcome" []
  (layout "Welcome"))

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
             [:p (link-to "/new-code" "Generate another")]
             ))))

(defpage "/clients" []
  (layout "Clients"
          (let [clients (ds/query :kind Client)]
            (html
             [:p (link-to "/client/new" "Add new client")]
             (ordered-list (map link-client clients))))))

(defpage "/client/:id" {id :id}
  (let [[client] (ds/query :kind Client :filter (= :firstname id))]
    (layout (:firstname client)
            (html
             [:p (:firstname client)]))))

(defpage "/client/new" {code :code :as client}
  (layout "New Client"
          (form-to [:post "/client/new"]
                   (if (not (is-admin?))
                     (code-field code))
                   (user-fields client)
                   (submit-button "Add client"))))

(defpage [:post "/client/new"] {:as form}
  (if (valid? form)
    (loop []
      ;; TODO check for errors with insertion
      (ds/save! (Client. (:firstname form)))
      (layout "New Client"
              [:p (str "Success!" form)]
              [:p (link-to "/clients" "View all clients")
               " or "
               (link-to "/client/new" "Add another client")]))
    (render "/client/new" form)))
