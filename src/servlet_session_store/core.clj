(ns servlet-session-store.core
  (:require [ring.middleware.session.store :refer [SessionStore]])
  (:import  [javax.servlet.http HttpServletRequest HttpSession]))

(defprotocol HttpSessionCoercion
  (^HttpSession as-http-session [_ _]))

(extend-protocol HttpSessionCoercion
  java.util.Map
  (as-http-session [m options] (as-http-session (:servlet-request m) options))

  HttpServletRequest
  (as-http-session [request {:keys [timeout]}]
    (let [session (.getSession request)]
      (when timeout
        (.setMaxInactiveInterval session))
      session))

  HttpSession
  (as-http-session [session _] session))

(deftype ServletSessionStore [^HttpSession hs]
  SessionStore
  (read-session [_ key] (.getAttribute hs ^String key))
  (write-session [_ key data] (.setAttribute hs ^String key data))
  (delete-session [_ key] (.removeAttribute hs ^String key)))

(defn servlet-session-store [request options]
  (ServletSessionStore.
   (as-http-session request options)))

(defn wrap-servlet-session
  ([handler] (wrap-servlet-session handler {}))
  ([handler options]
   (fn [request]
    (handler
     (assoc request :session (servlet-session-store request options))))))
