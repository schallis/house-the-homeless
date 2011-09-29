(ns house-the-homeless.templates
  (:use [noir.core :only [defpartial render]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [link-to html5 include-css include-js ordered-list]]
        [hiccup.form-helpers :only [drop-down form-to label submit-button text-field
                                    check-box]]
        [clojure.pprint :only [pprint]]
        [house-the-homeless.entities]
        [house-the-homeless.utils]
        [noir.session :as sess])
  (:require [house-the-homeless.settings :as settings]
            [appengine-magic.services.user :as ui]))

(defpartial side-bar []
  [:div#sidebar
   (if (ui/user-logged-in?)
     [:ul
      [:li "Logged in as " (ui/current-user) (if (is-admin?) " (Admin)")]
      [:li (link-to (ui/logout-url) "Logout")]
      [:li "Clients"
       [:ul
        [:li (link-to "/clients" "My Clients")]
        [:li (link-to "/client/new" "New Client")]]]
      (if (is-admin?)
        (html
         [:li "Stays"
          [:ul
           [:li (link-to "/stays" "All Stays")]
           [:li (link-to "/stays/stats" "Reports")]]]
         [:li "Codes"
          [:ul
           [:li (link-to "/codes" "All Codes")]
           [:li (link-to "/code/new" "New Code")]]]))]
     [:ul
      [:li "Not logged in"]
      [:li (link-to (ui/login-url :destination "/admin") "Login")]]
     )])

(defpartial unauthorised []
  (html5
     [:head
      [:title "Unauthorised"]
      (include-css "/css/main.css")]
     [:body
      [:header [:h1 "Unauthorised"]]
      [:p "You must log in before viewing this page"]]))

(defpartial client-not-found []
  (html5
     [:head
      [:title "Client not found"]
      (include-css "/css/main.css")]
     [:body
      [:header [:h1 "Client not found"]]
      [:p "I'm afraid I can't do that, Dave."]]))

;; Protected admin page template
(defpartial layout [title & content]
  (if (ui/user-logged-in?)
    (html5
     [:head
      [:title (str title " - House the Homeless")]
      (include-css "/css/reset.css")
      (include-css "/css/main.css")
      (include-js "/js/jquery-1.4.2.min.js")
      (include-js "/js/jquery-ui-1.8.1.custom.min.js")
      (include-js "/js/jquery.address-1.3.min.js")
      (include-js "/js/main.js")
      [:body
       [:div#flash (sess/flash-get)]
       [:header [:h1 "House the Homeless"]]
       (side-bar)
       (html
        [:div#main
         [:h2 title]
         content])]
      [:script "hth.init();"]])
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
     (side-bar)
     [:div#main
      [:h2 "Welcome"]
      [:p "Welcome"]])]))

(defpartial error-item [[first-error]]
  [:p.error first-error])
