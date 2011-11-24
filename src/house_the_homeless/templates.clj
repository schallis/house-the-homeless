(ns house-the-homeless.templates
  (:use [noir.core :only [defpartial render]]
        [hiccup.core :only [html resolve-uri]]
        [hiccup.page-helpers :only [link-to html5 include-css include-js ordered-list]]
        [hiccup.form-helpers :only [drop-down form-to label submit-button text-field
                                    check-box]]
        [clojure.pprint :only [pprint]]
        [house-the-homeless.entities]
        [house-the-homeless.utils]
        [noir.session :as sess :only [flash-get flash-put!]])
  (:require [house-the-homeless.settings :as settings]
            [appengine-magic.services.user :as ui]))

(defpartial printout-info []
  [:span.center
   [:div.printout-info.printonly
    [:h3 "Important information for volunteers"]
    [:p
     "The confirmed list below and numbers 1-3 on the waiting list are
    asked to come to the venue for 7pm & wait outside "
    [:u "(volunteers to give tea, coffee and biscuits while they
    wait)."]
    " Take the logfile and start check-in "
    [:u "outside"]
    " the venue at 7:20pm (make sure two people are together doing
    check-in). Open the doors at 7:30pm to let those you have checked
    in, in. If one of 1-15 is not present at 7:30pm they lose their
    bed to any of the following that might be waiting: 1, 2 or 3 (in
    that order). If a confirmed name for example does not turn up till
    8pm but 1, 2 or 3 have not arrived yet, then the confirmed name
    can take their bed as it is still empty. Please tell him/her off
    though for being late. Do not let anyone from the waiting list
    into the building until they are guaranteed a bed (continue to
    offer them hot drinks outside). Once 15 spaces are checked-in,
    those remaining on the waiting list are asked to elave the church
    area. If someone from the top 15 comes late to find they have lost
    their bed, please give the person a referral agency map from the
    log file and explain that the next day they need to go back to one
    of these agencied to be re-referred back into the shelter. The max
    stay of any person including days they may or may not be at the
    shelter is 28 days for 2011/12 seasons. D or E mean Diabetic or
    Epileptic. CI means Check in; TU means Turned up and ST means
    Staying tomorrow."]]])

(defpartial side-bar []
  [:div#sidebar
   (if (ui/user-logged-in?)
     [:ul
      [:li (ui/current-user) #_(if (is-admin?) " (Admin)")
       [:ul
        [:li (link-to (ui/logout-url) "Logout")]]]
      [:li "Clients"
       [:ul
        [:li (link-to "/clients" "My Clients")]
        [:li (link-to "/client/new" "New Client")]]]
      (if (is-admin?)
        (html
         [:li "Shelter log"
          [:ul
           [:li (link-to "/calendar/today" "Today")]
           [:li (link-to "/stay/new" "New Stay")]
           [:li (link-to "/stays" "All Stays")]]]
         [:div#calendar ""]
         [:li "Codes"
          [:ul
           [:li (link-to "/codes" "All Codes")]
           [:li (link-to "/code/new" "New Code")]]]))]
     [:ul
      [:li "Anonymous"
       [:ul
        [:li (link-to (ui/login-url :destination "/admin") "Login")]]]]
     )])

(defpartial unauthorised []
  (html5
     [:head
      [:title "Unauthorised"]
      (include-css "/css/main.css")]
     [:body
      [:header [:h1 "Unauthorised"]]
      [:p "You must " (link-to (ui/login-url) "log in") " with an appropriate account before viewing this page"]]))

(defpartial not-found []
  (html5
     [:head
      [:title "Page not found"]
      (include-css "/css/reset.css")
      (include-css "/css/main.css")]
     [:body
      [:header [:h1 "Page not found"]]
      [:p "I'm afraid I can't do that, Dave."]]))

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
      (include-css "/css/chosen.css")
      [:link {:type "text/css",
              :href (resolve-uri "/css/print.css"),
              :media "print"
              :rel "stylesheet"}]
      (include-js "/js/jquery-1.4.2.min.js")
      (include-js "/js/jquery-ui-1.8.1.custom.min.js")
      (include-js "/js/jquery.ui.datepicker-en-GB.js")
      (include-js "/js/jquery.address-1.3.min.js")
      (include-js "/js/jquery.dataTables.min.js")
      (include-js "/js/chosen.jquery.min.js")
      (include-js "/js/main.js")
      "<link href='http://fonts.googleapis.com/css?family=Chivo:400' rel='stylesheet' type='text/css'>"
      [:body
       [:div#flash (sess/flash-get)]
       [:header [:h1 settings/site-title]]
       (side-bar)
       (html
        [:div#main
         [:h2 title]
         content])]
      [:script "hth.init();"]])
    ;; Unauthorised
    (if (true? settings/debug)
      (unauthorised)
      (not-found))))

(defpartial admin-only [& content]
  (if (is-admin?)
    content
    (if (true? settings/debug)
      (unauthorised)
      (not-found))))

;; Unprotected welcome page
(defpartial welcome []
  (html5
   [:head
    [:title "Welcome"]
    (include-css "/css/reset.css")
    (include-css "/css/main.css")
    "<link href='http://fonts.googleapis.com/css?family=Chivo:400' rel='stylesheet' type='text/css'>"]
   [:body
    [:header [:h1 settings/site-title]]
    (html
     (side-bar)
     [:div#main
      [:h2 "Hi There"]
      [:p "You must login with a Google or OpenID account to continue."]
      [:p "If you are unable to login, please <a href=\"https://accounts.google.com/NewAccount\">register</a> for a new account."]])]))

(defpartial error-item [[first-error]]
  [:p.error first-error])
