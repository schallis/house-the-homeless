(ns house-the-homeless.routes
  (:refer-clojure :exclude (extend)) 
  (:use [noir.core :only [defpartial defpage render]]
        [hiccup.core :only [html resolve-uri]]
        [hiccup.page-helpers :only [link-to html5 include-css ordered-list
                                    unordered-list]]
        [hiccup.form-helpers :only [drop-down form-to label submit-button
                                    text-field text-area check-box]]
        [clojure.pprint :only [pprint]]
        [house-the-homeless.entities]
        [house-the-homeless.utils]
        [house-the-homeless.templates]
        [house-the-homeless.clients]
        [house-the-homeless.codes]
        [clj-time.core]
        [clj-time.format])
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
;; TODO check security of all pages for logged in and non-admin users

;; TEST client pages (non-valid inputs)
;; TEST invalid codes (strings, integers, string-ints, symbols)

(def date-user (formatter "dd/MM/yy"))
(def date-storage (formatter "dd/MM/yy"))

(defn date-to-user [date]
  (unparse date-user (parse date-storage date)))
(defn date-to-storage [date]
  (unparse date-storage (parse date-user date)))
(defn datetime-to-storage [datetime]
  (unparse date-storage datetime))
(defn user-day-of-week [date]
  (settings/dotw (- (day-of-week (parse date-user date)) 1)))

(defn parse-date [date]
  "Takes a date string and validates it, returning a new date string"
  (date-to-storage (date-to-user date)))

(ds/defentity Code [^:key content redeemed])
(ds/defentity Day [^:key date venue coordinator])
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
                      gender
                      diabetic
                      epileptic
                      terms])
(ds/defentity Stay [date
                    status
                    notes
                    client-id
                    confirmed
                    staying-tomorrow
                    check-in])

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

(defn get-clients-dropdown []
  "Return a list of clients in a format suitable for a dropdown"
  (conj (map #(vector (full-name %) (:id %)) (get-clients)) ["---" "default"]))

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

(defn get-stay [pk]
  ;; TODO handle appropriately when pk is nil
  "Return a stay with the specified pk"
  (ds/retrieve Stay pk))

(defn get-day [date]
  "Return a day with the specified date"
  (ds/retrieve Day date))

(defn get-stays-by-date
  ([date confirmed]
     "Return a list of stays for a particular date, unformatted"
     (if confirmed
       (ds/query :kind Stay
                 :filter ((= :date date)
                          (= :confirmed (str confirmed))))
       (ds/query :kind Stay
                 :filter ((= :date date)
                          (!= :confirmed "true")))))
  ([date confirmed count-only?]
     "Return a count of the stays for a particular date"
     (ds/query :kind Stay
               :count-only? true
               :filter ((= :date date)
                        (= :confirmed (str confirmed))))))

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
        (label "code" "Authentication Code: ")
        (text-field "code" code)]])

;;                       home-number
;;                       mobile-number
;;                       email
;;                       marital-status
;;                       ni-number
;;                       case-notes

(defpartial user-fields [{:keys [firstname lastname dob gender
                                 ethnicity marital-status
                                 nationality ni-number notes
                                 diabetic epileptic
                                 agency staff-member
                                 contact-phone tried-thsort
                                 thsort-reason
                                 tried-thhost thhost-reason
                                 mobile-number passport
                                 email right-to-remain
                                 medical-card birth-cert
                                 drivers-license other-id
                                 uk-bank-acc normal-borough
                                 no-fixed-abode job-centre
                                 accommodation-year1 accommodation-year2
                                 accommodation-year3 accommodation-year4
                                 before-growth benefits
                                 normally-sleep
                                 normally-sleep-details
                                 rough-sleeping squatted
                                 ]}]
  ;; Print out the fields two per line
  [:tr [:td [:h3 "Referring Agency"]]]
  (two-col
   [[:td (vali/on-error :agency error-item)
     (label "agency" "Agency:")
     (text-field "agency" agency)]
    [:td (vali/on-error :staff-member error-item)
     (label "staff-member" "Staff member completing this form:")
     (text-field "staff-member" staff-member)]
    [:td (vali/on-error :contact-phone error-item)
     (label "contact-phone" "Contact phone number:")
     (text-field "contact-phone" contact-phone)]
    ])
  [:tr [:td "Have you tried referring to:"]]
  (two-col
   [[:td (vali/on-error :tried-thsort error-item)
     (label "tried-thsort"
            "Tower Hamlets SORT (Street Outreach Response Team)?:")
     (check-box "tried-thsort" tried-thsort)]
    [:td (vali/on-error :thsort-reason error-item)
     (label "thsort-reason" "Outcome/Why:")
     (text-field "thsort-reason" thsort-reason)]
    [:td (vali/on-error :tried-thhost error-item)
     (label "tried-thhost"
            "Tower Hamlets HOST (Housing Options & Support Team)?:")
     (check-box "tried-thhost" tried-thhost)]
    [:td (vali/on-error :thhost-reason error-item)
     (label "thhost-reason" "Outcome/Why:")
     (text-field "thhost-reason" thhost-reason)]
    ])
  [:tr [:td [:h3 "General Information"]]]
  (two-col
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
     (vali/on-error :gender error-item)
     (label "gender" "Gender:")
     (drop-down "gender" settings/genders gender)]
    [:td
     (vali/on-error :marital-status error-item)
     (label "marital-status" "Marital status:")
     (text-field "marital-status" marital-status)]
    [:td
     (vali/on-error :ethnicity error-item)
     (label "dob" "Ethnicity:")
     (drop-down "ethnicity" settings/ethnicities ethnicity)]
    [:td
     (vali/on-error :nationality error-item)
     (label "nationality" "Nationality:")
     (text-field "nationality" nationality)]
    [:td
     (vali/on-error :right-to-remain error-item)
     (label "right-to-remain"
            "Do you have the legal right to remain in the UK?:")
     (drop-down "right-to-remain"
                settings/right-to-remain right-to-remain)]
    [:td
     (vali/on-error :ni-number error-item)
     (label "ni-number" "NI Number:")
     (text-field "ni-number" ni-number)]
    [:td
     (vali/on-error :mobile-number error-item)
     (label "mobile-number" "Mobile Number:")
     (text-field "mobile-number" mobile-number)]
    [:td
     (vali/on-error :email error-item)
     (label "email" "Email:")
     (text-field "mobile-number" mobile-number)]
    [:td
     (vali/on-error :other-id error-item)
     (label "other-id" "Other ID:")
     (text-field "other-id" other-id)]
    [:td
     (vali/on-error :diabetic error-item)
     (label "diabetic" "Diabetic:")
     (check-box "diabetic" diabetic)]
    [:td
     (vali/on-error :epileptic error-item)
     (label "epileptic" "Epileptic:")
     (check-box "epileptic" epileptic)]
    [:td
     (vali/on-error :uk-bank-acc error-item)
     (label "uk-bank-acc" "UK bank account:")
     (check-box "uk-bank-acc" uk-bank-acc)]])
  [:tr [:td "Do you have the following?"]]
  (two-col
   [[:td (vali/on-error :passport error-item)
     (label "passport" "Passport?:")
     (check-box "passport" passport)]
    [:td (vali/on-error :birth-cert error-item)
     (label "birth-cert" "Birth Certificate?:")
     (check-box "birth-cert" birth-cert)]
    [:td (vali/on-error :medical-card error-item)
     (label "medical-card" "Medical card?:")
     (check-box "medical-card" medical-card)]
    [:td (vali/on-error :drivers-license error-item)
     (label "drivers-license" "Drivers license?:")
     (check-box "drivers-license" drivers-license)]
    ])
  [:tr [:td [:h3 "State Benefits"]]]
  [:tr [:td (vali/on-error :job-centre error-item)
        (label "job-centre" "What is the address of your local Job Centre where you sign on?:")
        (text-field "job-centre" job-centre)]]
  [:tr [:td "Please fill in all state benefits you are receiving (Including " [:b "type, amount received and claim start date"] ")"]]
  [:tr [:td (vali/on-error :benefits error-item)
        (label "benefits" "Benefit details:")
        (text-area "benefits" benefits)]]
  
  [:tr [:td
        [:h3 "Housing"]
        [:p "Please enter the full address (inluding postcode) of accomodation where you resided for each year"]]]
  (two-col
   [[:td (vali/on-error :accommodation-year1 error-item)
     (label "accommodation-year1" "2008:")
     (text-area "accommodation-year1" accommodation-year1)]
    [:td (vali/on-error :accommodation-year2 error-item)
     (label "accommodation-year2" "2009:")
     (text-area "accommodation-year2" accommodation-year2)]
    [:td (vali/on-error :accommodation-year3 error-item)
     (label "accommodation-year3" "2010:")
     (text-area "accommodation-year3" accommodation-year3)]
    [:td (vali/on-error :accommodation-year4 error-item)
     (label "accommodation-year4" "2011:")
     (text-area "accommodation-year4" accommodation-year4)]
    [:td (vali/on-error :normal-borough error-item)
     (label "normal-borough" "In which borough do you normally sleep?:")
     (text-field "normal-borough" normal-borough)]
    [:td (vali/on-error :no-fixed-abode error-item)
     (label "no-fixed-abode" "How long have you had No Fixed Abode (NFA)?:")
     (drop-down "no-fixed-abode" settings/no-fixed-abode no-fixed-abode)]
    [:td (vali/on-error :before-growth error-item)
     (label "before-growth" "Where did you sleep the night before you arrived at GrowTH?:")
     (text-field "before-growth" before-growth)]])
  (two-col
   [[:td (vali/on-error :normally-sleep error-item)
     (label "normally-sleep" "Where do you normally sleep:")
     (drop-down "normally-sleep" settings/normally-sleep normally-sleep)]
    [:td (vali/on-error :normally-sleep-details error-item)
     (label "normally-sleep-details" "Details:")
     (text-field "normally-sleep-details" normally-sleep-details)]
    [:td (vali/on-error :rough-sleeping error-item)
     (label "rough-sleeping" "If you have been rough sleeping, how long have you been doing so?:")
     (text-field "rough-sleeping" rough-sleeping)]
    [:td (vali/on-error :squatted error-item)
     (label "squatted" "Have you ever squatted?:")
     (check-box "squatted" squatted)]
    ])
  [:tr [:td [:h3 "Assistance from other services"]]]
  [:tr [:td [:h3 "Physical health"]]]
  [:tr [:td [:h3 "Mental health"]]]
  [:tr [:td [:h3 "Alcohol use"]]]
  [:tr [:td
        [:h3 "Controlled drug use"]
        [:p "Controlled drugs - a controlled drug is a drug or chemical
         whose manufacturer, possession, or use are regulated by a
         government. This includes illegal drugs and prescription
         medications."]]]
  [:tr [:td [:h3 "Employment"]]]
  [:tr [:td [:h3 "Criminal offences"]]]
  [:tr [:td [:h3 "For refugees or an asylum seeker only"]]]
  [:tr [:td [:h3 "Other"]]]
  [:tr [:td [:h3 "Project coordinator's notes"]]]
  [:tr [:td
        (vali/on-error :notes error-item)
        (label "notes" "Notes")
        (text-area "notes" notes)]]
  )

(defpartial stay-fields [{:keys [client-id date status confirmed staying-tomorrow
                                 check-in notes]}]
  (map #(rowify (first %) (second %))
       (partition-all 2
                      [[:td
                        (vali/on-error :client-id error-item)
                        (label "client-id" "Client:")
                        (drop-down
                         "client-id"
                         (get-clients-dropdown) (str client-id))]
                       [:td (vali/on-error :date error-item)
                        (label "date" "Date:")
                        (text-field "date" date)]
                       [:td (vali/on-error :status status)
                        (label "status" "Status:")
                        (drop-down "status" settings/stay-statuses status)]
                       [:td (vali/on-error :confirmed error-item)
                        (label "confirmed" "Confirmed?")
                        (check-box "confirmed" confirmed)]
                       [:td (vali/on-error :staying-tomorrow error-item)
                        (label "staying-tomorrow" "Staying tomorrow?:")
                        (check-box "staying-tomorrow" staying-tomorrow)]
                       [:td (vali/on-error :check-in error-item)
                        (label "check-in" "Check in:")
                        (check-box "check-in" check-in)]
                       [:td (vali/on-error :notes notes)
                        (label "notes" "Notes:")
                        (text-area "notes" notes)]])))

(defpartial terms-field [terms]
  [:tr [:td
        (vali/on-error :terms error-item)
        (label "terms" (str "I accept the " (html (link-to "/terms" "terms and contitions"))))
        (check-box "terms" terms)]])

(defpartial day-fields [{:keys [venue coordinator]}]
  (map #(rowify (first %) (second %))
       (partition-all 2
                      [[:td
                        (vali/on-error :terms error-item)
                        (label "venue" "Venue")
                        (text-field "venue" venue)]
                       [:td
                        (vali/on-error :coordinator error-item)
                        (label "coordinator" "Coordinator on duty")
                        (text-field "coordinator" coordinator)]])))

(defn valid-client? [{:keys [code firstname lastname dob gender terms]}]
  (if (not (is-admin?))
    (loop []
      ;; TODO check for valid format
      (vali/rule (or (= code settings/secret-key)
                     (and (parse-int code)
                          (code-issued? (parse-int code))))
                 [:code "That code is not valid"])
      (vali/rule (vali/has-value? code)
                 [:code "You must supply a code"])
      (vali/rule (vali/has-value? terms)
                 [:terms "You must accept the terms and conditions"])))
  (vali/rule (vali/has-value? firstname)
             [:firstname "Your must supply a first name"])
  (vali/rule (vali/has-value? gender)
             [:gender "Your must supply a gender"])
  (vali/rule (vali/has-value? lastname)
             [:lastname "You must supply a last name"])
  (vali/rule (vali/has-value? dob)
             [:dob "You must supply a date of birth"])
  (not (vali/errors? :code :lastname :firstname :dob :gender :terms)))

(defn valid-stay? [{:keys [client-id date status confirmed staying-tomorrow notes]}]
  (vali/rule (vali/has-value? client-id)
             [:client-id "You must supply a client-id"])
  (vali/rule (not (= client-id "default"))
             [:client-id "You must select a client"])
  (vali/rule (vali/has-value? date)
             [:date "You must supply a date"])
  ;; TODO! Check for duplicate bookings
  ;; (if (and (= confirmed "true")
  ;;          (vali/has-value? date))
  ;;   (vali/rule (= (get-stays-by-date (parse-date date) true true ) 0)))
  ;; Check there is capacity
  (if (and (= confirmed "true")
           (vali/has-value? date))
    (vali/rule (< (get-stays-by-date (parse-date date) true true)
                   settings/max-capacity)
               [:confirmed (str "There are already "
                                settings/max-capacity
                                " people confirmed for "
                                date)]))
  (vali/rule (vali/has-value? status)
             [:status "You must supply a valid status"])
  (not (vali/errors? :client-id :date :status :confirmed :staying-tomorrow :notes)))

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

(defpage "/reports" []
  (admin-only
   (layout "Reports")))

(defpage "/codes" []
  (layout "Codes"
          (let [codes (ds/query :kind Code)]
            (html
             [:p (link-to "/code/new" "Generate new code")]
             [:table.tabular
              [:thead
               [:tr.heading
                [:td "Code"]
                [:td "Redeemed?"]]]
              [:tbody
               (map
                #(html [:tr
                        [:td (:content %)]
                        [:td (:redeemed %)]])
                codes)]]))))

(defpage "/code/new" []
  (layout "New Code"
          (let [code (gen-code)]
            (sess/flash-put! (str "New code '" code "' generated"))
            (ds/save! (Code. code "no"))
            (html
             [:p code]
             [:p (link-to "/code/new" "Generate another")]))))

(defpage "/clients" []
  (layout "Clients"
          (let [clients (get-clients)]
            (html
             [:p (link-to "/client/new" "Add new client")]
               (html
                [:table.tabular
                 [:thead
                  [:tr.heading
                   [:td "ID"]
                   [:td "Name"]
                   [:td "Status"]
                   [:td "Stays"]]]
                 [:tbody
                  (map
                   #(html [:tr
                           [:td (ds/key-id %)]
                           [:td (link-client %)]
                           [:td (:status %)]
                           [:td (colourise-stays
                                 (get-stays-count (ds/key-id %))
                                 settings/default-max-stay)]])
                   clients)]])))))

(defpage "/stays" []
  (admin-only
   (layout "Stays"
           (let [stays (get-stays)]
             (html
              [:table.tabular
               [:thead
                [:tr.heading
                 [:td "Date"]
                 [:td "Confirmed"]
                 [:td "Staying tomorrow"]
                 [:td "Status"]
                 [:td "Client ID"]]]
               [:tbody
                (map
                 #(html [:tr
                         [:td (:date %)]
                         [:td (:confirmed %)]
                         [:td (:staying-tomorrow %)]
                         [:td (:status %)]
                         [:td (:client-id %)]])
                 stays)]])))))

(defpage "/calendar/today" []
  (resp/redirect (str "/calendar/" (datetime-to-storage (now)))))

(defpage "/calendar/:dd/:mm/:yy" {dd :dd
                                  mm :mm
                                  yy :yy}
  (let [view-date (parse-date (str dd "/" mm "/" yy))]
    (admin-only
     (layout "Log Sheet"
             (let [stays-confirmed (get-stays-by-date view-date true)
                   stays-waiting (get-stays-by-date view-date false)
                   day (get-day view-date)]
               (html
                (if (and dd mm yy)
                  [:p "For the day " (str view-date)
                   " (" (user-day-of-week view-date) ")"])
                [:a.print-button.noprint.button {:href (resolve-uri "javascript:window.print()")} "Print"]
                [:h2.printonly "15 guests - MAXIMUM. Do not sleep more."]
                [:p.noprint (link-to (str "/stay/new?date=" view-date) "Add new stay")]
                [:table.form
                 (day-fields day)]
                [:br]
                (printout-info)
                [:br]
                [:h3 "Confirmed"]
                [:table.tabular-nojs
                 [:thead
                  [:tr.heading
                   ;;[:td "Status"]
                   [:td "Name"]
                   [:td "Gender"]
                   [:td "Check in"]
                   [:td "Stay tomorrow?"]
                   [:td "Comments"]
                   [:td "DOB"]
                   [:td "Referrer"]
                   [:td "D/E"]
                   [:td "Dietary Reqs"]
                   [:td.noprint ""]]]
                 [:tbody
                  (map
                   #(let [client (ds/retrieve Client (:client-id %))]
                      (html [:tr
                             ;;[:td (:status %)]
                             [:td (link-client client)]
                             [:td (:gender client)]
                             [:td.shaded (:check-in %)]
                             [:td.shaded (:staying-tomorrow %)]
                             [:td.shaded.comment-box (:notes %)]
                             [:td (:dob client)]
                             [:td (:agency client)]
                             [:td (if (:diabetic client) "D") (if (:epileptic client) "E")]
                             [:td (:dietary-requirements client)]
                             [:td.noprint (link-to (str "/stay/edit/" (ds/key-id %)) "Edit")]]))
                   stays-confirmed)]]
                [:br]
                [:h3 "Waiting list"]
                [:table.tabular-nojs
                 [:thead
                  [:tr.heading
                   ;;[:td "Status"]
                   [:td "Name"]
                   [:td "Gender"]
                   [:td "Turned up?"]
                   [:td "Check in"]
                   [:td "Stay tomorrow?"]
                   [:td "Comments"]
                   [:td "DOB"]
                   [:td "Referrer"]
                   [:td "D/E"]
                   [:td "Dietary Reqs"]
                   [:td.noprint ""]]]
                 [:tbody
                  (map
                   #(let [client (ds/retrieve Client (:client-id %))]
                      (html [:tr
                             [:td (link-client client)]
                             [:td (:gender client)]
                             [:td.shaded ""]
                             [:td.shaded (:check-in %)]
                             [:td.shaded (:staying-tomorrow %)]
                             [:td.shaded.comment-box (:notes %)]
                             [:td (:dob client)]
                             [:td (:agency client)]
                             [:td (if (:diabetic client) "D") (if (:epileptic client) "E")]
                             [:td (:dietary-requirements client)]
                             [:td.noprint (link-to (str "/stay/edit/" (ds/key-id %)) "Edit")]]))
                   stays-waiting)]]))))))

(defpage [:GET "/stay/edit/:id"] {id :id :keys [posted] :as env}
  (let [stay-id (parse-int id)
        stay (get-stay stay-id)]
    (layout "Edit Stay"
            (form-to [:post (str "/stay/edit/" stay-id)]
                     [:table.form.chosen
                      (stay-fields stay)
                      [:tr [:td (submit-button "Save stay") " or " (link-to "javascript:history.back()" "Cancel")]]]))))

(defpage [:POST "/stay/edit/:id"] {id :id :as form}
  (let [stay-id (parse-int id)
        stay (get-stay stay-id)]
    (if (valid-stay? form)
      (and
       (ds/save! (conj stay
                       {:date (:date form)
                        :status (:status form)
                        :notes (:notes form)
                        :client-id (parse-int (:client-id form))
                        :confirmed (:confirmed form)
                        :staying-tomorrow (:staying-tomorrow form)
                        :check-in (:check-in form)}))
       (sess/flash-put! "Stay updated successfully")
       (resp/redirect (str "/calendar/" (:date form))))
      (and
       (sess/flash-put! "There were errors in the form")
       (render "/stay/edit/:id" (assoc form :posted 'true))))))

(defpage [:GET "/stay/new"] {:keys [posted] :as env}
  (layout "New Stay"
          (form-to [:post (str "/stay/new")]
                   [:table.form.chosen
                    (stay-fields env)
                    [:tr [:td (submit-button "Save stay")]]])))

(defpage [:POST "/stay/new"] {:as form}
  (if (valid-stay? form)
    (and
     (ds/save! (Stay.
                (unparse date-storage (parse date-user (:date form)))
                (:status form)
                (:notes form)
                (parse-int (:client-id form))
                (:confirmed form)
                (:staying-tomorrow form)
                (:check-in form)))
     (sess/flash-put! "Stay created successfully")
     (resp/redirect (str "/calendar/" (:date form))))
    (render "/stay/new" (assoc form :posted 'true))))

(defpage [:GET "/client/edit/:id"] {:keys [posted id] :as env}
  (let [int-id (parse-int id)
        client (if (not posted)
                 (get-client int-id)
                 env)
        stays (get-stays int-id)]
    (if client
      (layout (full-name client)
              [:div#uitabs
               [:ul
                [:li (link-to "#details" "Details")]
                (if (is-admin?)
                  (html
                   [:li (link-to "#stays"
                                 (html "Stays " "(" (get-stays-count int-id) ")"))]
                   [:li (link-to "#newstay" "New Stay")]
                   [:li (link-to "#casenotes" "Case Notes")]
                   [:li (link-to "#metadata" "Metadata")]))]
               [:div#details
                (form-to [:post (str "/client/edit/" int-id)]
                         [:table.form.chosen
                          [:tr
                           [:td
                            (label "status" "Status:")
                            (drop-down "status"
                                       settings/client-statuses (:status client))]]
                          (user-fields client)
                          [:tr [:td
                                (submit-button "Save")]]])]
               (if (is-admin?)
                 (html
                  [:div#stays
                   (html
                    [:table.tabular
                     [:thead
                      [:tr
                       [:td "Date"]
                       [:td "Status"]
                       [:td "Confirmed?"]
                       [:td ""]]]
                     [:tbody
                      (map
                       #(html [:tr
                               [:td (link-to (str "/calendar/" (date-to-user (:date %)))
                                             (date-to-user (:date %)))]
                               [:td (:status %)]
                               [:td (:confirmed %)]
                               [:td (link-to (str "/stay/edit/" (ds/key-id %)) "Edit")]
                               ]) stays)]])]
                  [:div#newstay
                   (form-to [:post (str "/stay/new")]
                            [:table.form
                             (stay-fields (assoc env :client-id int-id))
                             [:tr [:td (submit-button "Add stay")]]])]
                  [:div#casenotes
                   (html
                    [:table.tabular
                     [:thead
                      [:tr
                       [:td "Date"]
                       [:td "Comments"]
                       [:td ""]]]
                     [:tbody
                      (map
                       #(html [:tr
                               [:td (link-to (str "/calendar/" (date-to-user (:date %)))
                                             (date-to-user (:date %)))]
                               [:td (:notes %)]
                               [:td (link-to (str "/stay/edit/" (ds/key-id %)) "Edit")]
                               ]) stays)]])]
                  [:div#metadata
                   [:p (str "Referred by " (:creator client))]
                   [:p (str "Last edited by " (:last-modifier client))]]))
               ])
      (render "/client-not-found"))))

(defpage [:POST "/client/edit/:id"] {id :id :as form}
  (let [int-id (parse-int id)
        client (get-client int-id)]
    (if (valid-client? (assoc form :code settings/secret-key :terms "true"))
      (and
       (ds/save! (conj client form {:last-modifier (current-user-email)}))
       (sess/flash-put! "Client updated successfully")
       (resp/redirect (str "/client/edit/" int-id)))
      (and
       (sess/flash-put! (vali/get-errors :code))
       (render "/client/edit/:id" (assoc form :posted 'true))))))

(defpage [:GET "/client/new"] {:keys [terms code] :as client}
  (layout "New Client"
          (form-to [:post "/client/new"]
                   [:table.form.chosen
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
      (if (not (is-admin?))
        ;; TODO figure outbetter way to validate this form to avoid
        ;; horrible secret-key hack
        (let [code (ds/retrieve Code (parse-int (:code form)))]
          (ds/save! (conj code {:redeemed "Yes"}))))
      ;; Save the raw form with some fields added/overwritten
      (let [client (ds/new* Client {:creator (current-user-email)
                                    :last-modifier (current-user-email)})]
        (ds/save! (assoc (conj client form) :dob (date-to-storage (:dob form)))))
      (sess/flash-put! "Client created successfully")
      (layout "New Client"
              [:p (str "Success!")]
              [:p (link-to "/clients" "View all clients")
               " or "
               (link-to "/client/new" "Add another client")]))
    (render "/client/new" form)))
