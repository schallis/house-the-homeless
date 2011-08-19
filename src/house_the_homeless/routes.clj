(ns house-the-homeless.routes
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [appengine-magic.services.user :as ui]
            [appengine-magic.services.datastore :as ds]))

;;
;;
;; Entities
;;
;;

(ds/defentity Code [^:key content])
(ds/defentity Event [^:key content])
(ds/defentity Client [^:key firstname])

;;
;;
;; Utilities
;;
;;

(defn gen-code
  "Generate a random unique code"
  []
  (rand-int 100))

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
      [:li "Codes"
       [:ul
        [:li (link-to "/codes" "All Codes")]
        [:li (link-to "/code/new" "New Code")]]]]
     [:ul
      [:li "Not logged in"]
      [:li (link-to (ui/login-url) "Login")]]
     )])

(defpartial layout [title & content]
  (html5
     [:head
      [:title title]]
     [:body
      [:h1 "House the Homeless"]
      (side-bar)
      [:h2 title]
      content]))

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
  (if (and (ui/user-logged-in?) (not (ui/user-admin?)))
    (vali/rule (vali/min-length? code 1)
               [:code "You must supply a code"]))
  (vali/rule (vali/min-length? firstname 5)
             [:firstname "Your first name must have more than 5 letters."])
  (vali/rule (vali/has-value? lastname)
             [:lastname "You must have a last name"])
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

(defpage "/code/new" []
  (layout "New Code"
          (let [code (gen-code)
                codes (ds/query :kind Code)]
            (ds/save! (Code. code))
            (html
             [:p code]
             [:p (link-to "/new-code" "Generate another")]
             ))))

(defpage "/codes" []
  (layout "Codes"
          (let [codes (ds/query :kind Code)]
            (html
             [:p (link-to "/new-code" "Generate new code")]
             (ordered-list (map #(:content %) codes))))))

(defpage "/client/new" {code :code :as client}
  (layout "New Client"
          (form-to [:post "/client/new"]
            (if (and (ui/user-logged-in?) (not (ui/user-admin?)))
              (code-field code))
            (user-fields client)
            (submit-button "Add client"))))

(defpage [:post "/client/new"] {:as form}
  (if (valid? form)
    (layout "New Client"
     [:p (str "Success!" form)])
    (render "/client/new" form)))
