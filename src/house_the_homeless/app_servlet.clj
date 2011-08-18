(ns house-the-homeless.app_servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use house-the-homeless.core)
  (:use [appengine-magic.servlet :only [make-servlet-service-method]]))


(defn -service [this request response]
  ((make-servlet-service-method house-the-homeless-app) this request response))
