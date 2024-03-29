(ns house-the-homeless.settings)

(def debug true)
(def secret-key "asd98&7sd879/00)")

;; The amount of nights one person can stay
(def default-max-stay 28)

;; The amount of people that can stay 
(def max-capacity 15)

(def site-title "GrowTH Online Referrals")

(def dotw
  ["Monday"
   "Tuesday"
   "Wednesday"
   "Thursday"
   "Friday"
   "Saturday"
   "Sunday"])

(def ethnicities
  ["White"
   "African"
   "Asian"
   "Pan-Asian"
   "Other"])

(def right-to-remain
  ["Yes"
   "No"
   "Pending"])

(def reason-homeless
  ["Upbringing"
   "Relationship breakdown"
   "Lack of money"
   "Gambling"
   "Bad health"
   "Drug or alcohol addiction"
   "Other"])

(def no-fixed-abode
  ["Under 1 week"
   "Under 1 month"
   "Under 6 months"
   "Under 1 year"
   "Under 3 years"
   "More than 3 years"])

(def faiths
  ["None"
   "Christian"
   "Jewish"
   "Hindu"
   "Islamic"
   "Buddhist"
   "Sikh"
   "Other"])

(def normally-sleep
  ["Street - which streets?"
   "Friends sofa/floor - which address?"
   "Buses - which bus numbers?"
   "Squat - where?"
   "Other - please specify"])

(def genders
  ["Male"
   "Female"])

(def client-statuses
  ["Waiting list" ;; default
   "Not waiting"
   "Rejected"])

(def stay-statuses
  ["Pending" ;; default
   "Stayed"
   "No beds"
   "Late"
   "Absent"
   "Refused"
   "Disiplinary"])
