(ns quotwit.web
  (:import (com.ning.http.multipart FilePart ByteArrayPartSource))
  (:require
   [quotwit.cards :as cards]
   [clojure.java.io :as io]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [oauth.client :as oauth]
   [ring.util.response :as response]
   [noir.session :as nsess]
   [twitter.oauth :as tauth]
   [twitter.api.restful :as tapi]
   [twitter.request :as treq]
   [clojure.edn :as edn]))

(def config (edn/read-string (slurp "config.edn")))

(def consumer
  (oauth/make-consumer
   (:api-key config)
   (:api-secret config)
   "https://api.twitter.com/oauth/request_token"
   "https://api.twitter.com/oauth/access_token"
   "https://api.twitter.com/oauth/authorize"
   :hmac-sha1))

(defn get-last-tweet [tcreds sname]
  (let [tid (get-in (tapi/users-show :oauth-creds tcreds :params {:screen_name sname}) [:body :status :id])]
    (str "https://twitter.com/" sname "/status/" tid)))

(defn post-tweet [creds tweet]
  (let [tcreds (tauth/make-oauth-creds (:api-key config) (:api-secret config) (:oauth_token creds) (:oauth_token_secret creds))
        file (cards/get-image-byte-array (:quote tweet) (:name tweet) (:source tweet))
        file-part (FilePart. "media[]" (ByteArrayPartSource. "image.jpg" file) "image/jpeg" "UTF-8")
        tweet-part (treq/status-body-part (:status tweet))
        sname (:screen_name creds)]
    (try
      (tapi/statuses-update-with-media :oauth-creds tcreds :body [file-part tweet-part])
      (catch Exception e (println "Error: " e)))
    (response/redirect (get-last-tweet tcreds sname))))

(defroutes main-routes
  (route/files "/public")  
  (GET "/" req (response/file-response "public/index.html"))
  (GET "/image" req
       (let [quote (get-in req [:params :quote] "")
             name (get-in req [:params :name] "")
             source (get-in req [:params :source] "")
             status (get-in req [:params :status] "")
             bytes (cards/get-image-bytes quote name source)]
         (nsess/put! :tweet {:quote quote :name name :source source :status status})
         (-> (response/response bytes)
             (response/header "Content-Length" (.available bytes))
             (response/content-type "image/jpeg"))))
  (GET "/app/post" req
       (post-tweet (nsess/get :twitter-oauth) (nsess/get :tweet)))
  (GET "/oauth_response" req
       (let [request-token (nsess/get :request-token)
             resp (oauth/access-token consumer request-token (get-in req [:params :oauth_verifier]))]
         (nsess/put! :twitter-oauth resp)
         (response/redirect (nsess/get :redirect-to "/app/post")))))

(defn callback-uri [request-token]
  (oauth/user-approval-uri consumer (:oauth_token request-token)))

(defn get-request-token []
  (oauth/request-token consumer (str (:url config) "/oauth_response")))

(defn wrap-oauth [handler]
  (fn [request]
    (if (and (re-matches #"/app.*" (:uri request))
             (nil? (nsess/get :twitter-oauth)))
      (let [request-token (get-request-token)
            auth-url (callback-uri request-token)]
        (nsess/put! :redirect-to (:uri request))
        (nsess/put! :request-token request-token)
        (response/redirect auth-url))
      (handler request))))
              
(def app
  (-> (handler/site main-routes)
      (wrap-oauth)
      (nsess/wrap-noir-session)))
