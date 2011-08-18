(ns house-the-homeless.core
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [noir.util.gae :as noir]))

;; load all of the routes (defpage's) so they get def'd
(require 'house-the-homeless.routes)

(ae/def-appengine-app house-the-homeless-app (noir/gae-handler nil))
