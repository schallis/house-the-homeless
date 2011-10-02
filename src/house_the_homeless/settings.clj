(ns house-the-homeless.settings)

(def debug true)
(def secret-key "asd98&7sd879/00)")

(def default-max-stay 28)

(def ethnicities
  ["White"
   "African"
   "Asian"
   "Pan-Asian"
   "Other"])

(def client-statuses
  ["Waiting list" ;; default
   "Not waiting"
   "Rejected"])

(def stay-statuses
  ["Pending" ;; default
   "Reserve"
   "Stayed"
   "No Bed"
   "Late"
   "Disiplinary"])
