(ns house-the-homeless.routes
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:require [noir.response :as resp]
            [noir.validation :as vali]
            [appengine-magic.services.user :as ui]
            [appengine-magic.services.datastore :as ds]))

(defpartial error-item [[first-error]]
  [:p.error first-error])

(defpartial greeting [fname lname]
  [:h1 "Hello, " fname " " lname])

(defpage "/" []
  (resp/redirect "/welcome"))

(defpage "/welcome" []
  "Welcome to the Noir/App Engine test!")

(defpage "/test" {:keys [fname lname]}
  (greeting fname lname))

(defn login-link
  [text]
  (let [user (ui/current-user)]
    (link-to (.createLoginURL (:user-service user) "/") text)))

(defn logout-link
  [text]
  (let [user (ui/current-user)]
    (link-to (.createLogoutURL (:user-service user) "/") text)))

(defn side-bar []
  (let [user (ui/current-user)]
    [:div#sidebar
     [:h3 "Current User"]
     (if (ui/user-logged-in?)
       [:ul
        [:li "Logged in as " (ui/current-user)]
        [:li (link-to (ui/logout-url) "Logout")]
        [:li (link-to "/generate-code" "Generate Code")]
        [:li (link-to "/codes" "Codes")]
        [:li (link-to "/clients" "Clients")]
        [:li (link-to "/client/add" "New Client")]]
       [:ul
        [:li "Not logged in"]
        [:li (link-to (ui/login-url) "Login")]]
       )]))

(defpartial layout [title & content]
  (html5
    [:head
     [:title title]]
    [:body
     (side-bar)
     [:h1 title]
     content]))

(defn gen-code
  "Generate a random unique code"
  []
  (rand-int 100))

(ds/defentity Code [^:key content])

(defpage "/generate-code" []
  (let [code (gen-code)
        codes (ds/query :kind Code)]
    (ds/save! (Code. code))
    (html
     [:p code]
     [:p (link-to "/generate-code" "Generate another")]
     )))

(defpage "/codes" []
  (let [codes (ds/query :kind Code)]
    (html
     [:p (link-to "/generate-code" "Generate new code")]
     (ordered-list (map #(:content %) codes))
     )))

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

(defpage "/client/add" {code :code :as client}
  (layout "Add Client"
   (form-to [:post "/client/add"]
            (if (and (ui/user-logged-in?) (not (ui/user-admin?)))
              (code-field code))
            (user-fields client)
            (submit-button "Add client"))))

(defpage [:post "/client/add"] {code :code :as client}
  (if (valid? client)
    (layout "Add Client"
     [:p (str "Success!" code client)])
    (render "/client/add" client)))
