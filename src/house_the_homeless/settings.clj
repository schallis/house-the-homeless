(ns house-the-homeless.settings)

(def debug true)
(def secret-key "asd98&7sd879/00)")

;; The amount of nights one person can stay
(def default-max-stay 28)

;; The amount of people that can stay 
(def max-capacity 15)

(def ethnicities
  ["White"
   "African"
   "Asian"
   "Pan-Asian"
   "Other"])

(def genders
  ["Male"
   "Female"])

(def client-statuses
  ["Waiting list" ;; default
   "Not waiting"
   "Rejected"])

(def stay-statuses
  ["Pending" ;; default
   "Waiting list"
   "Reserve"
   "Stayed"
   "No Bed"
   "Late"
   "Disiplinary"])
