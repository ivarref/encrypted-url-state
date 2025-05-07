(ns com.github.ivarref.encrypted-url-state
  (:require [taoensso.nippy :as nippy])
  (:import
    (java.util Base64)))

(def ^:dynamic ^:private -*current-epoch-time*
  (fn []
    (long (/ (System/currentTimeMillis)
             1000))))

(defn curr-epoch-time-plus-seconds [seconds]
  (+ seconds (-*current-epoch-time*)))

(defn encrypt
  ([secret-key state]
   (when (not (string? secret-key))
     (throw (IllegalArgumentException. "secret-key must be a string")))
   (encrypt secret-key (curr-epoch-time-plus-seconds 3600) state))
  ([secret-key exp-epoch-seconds state]
   (when (not (string? secret-key))
     (throw (IllegalArgumentException. "secret-key must be a string")))
   (when (not (number? exp-epoch-seconds))
     (throw (IllegalArgumentException. "exp-epoch-seconds must be a number")))
   (.encodeToString (Base64/getUrlEncoder) (nippy/freeze [(long exp-epoch-seconds) state] {:password [:cached secret-key]}))))

(defn decrypt
  ([secret-key encrypted-str-b64-url]
   (when (not (string? secret-key))
    (throw (IllegalArgumentException. "secret-key must be a string")))
   (when (not (or (bytes? encrypted-str-b64-url) (string? encrypted-str-b64-url)))
     (throw (IllegalArgumentException. "encrypted-str-b64-url must be a string or bytes")))
   (decrypt secret-key (-*current-epoch-time*) encrypted-str-b64-url))
  ([secret-key epoch-seconds-now encrypted-str-b64-url]
   (when (not (string? secret-key))
     (throw (IllegalArgumentException. "secret-key must be a string")))
   (when-not (number? epoch-seconds-now)
     (throw (IllegalArgumentException. "epoch-seconds-now must be a number")))
   (when (not (string? encrypted-str-b64-url))
     (throw (IllegalArgumentException. "encrypted-str-b64-url must be a string")))
   (let [[error payload] (try
                           [false (nippy/thaw (if (string? encrypted-str-b64-url)
                                                (.decode (Base64/getUrlDecoder) ^String encrypted-str-b64-url)
                                                encrypted-str-b64-url)
                                              {:password [:cached secret-key]})]
                           (catch Exception e
                             [true (or (ex-message e) "Empty error message")]))]
     (if (true? error)
       {:expired? false :state nil :error? true :error-message payload}
       (do
         (assert (and (vector? payload)
                      (= 2 (count payload))
                      (int? (first payload))))
         (let [[expiry data] payload
               expired? (> (long epoch-seconds-now) expiry)]
           (if expired?
             {:expired? true :state nil :error? false :error-message "Expired"}
             {:expired? false :state data :error? false :error-message nil})))))))
