(ns com.github.ivarref.encrypted-url-state-test
  (:require [com.github.ivarref.pretty-stuff :as pretty-stuff]
            [clojure.test :as t]))

(do
  (set! *warn-on-reflection* true)
  (require '[com.github.ivarref.encrypted-url-state :as eus]))

(t/use-fixtures :once pretty-stuff/short-stacktrace)

(defn d
  ([x]
   (println ">>" (pr-str x))
   x)
  ([prefix x]
   (println prefix (pr-str x))
   x))


(t/deftest error-handling-argument-types-encrypt
  (t/is (thrown? IllegalArgumentException (eus/encrypt nil 1 "message")))
  (t/is (thrown? IllegalArgumentException (eus/encrypt "my-key" nil "message")))
  (t/is (thrown? IllegalArgumentException (eus/encrypt "my-key" "123" "message"))))

(t/deftest error-handling-argument-types-decrypt
  (let [encrypted-sample (eus/encrypt "my-key" 1 "message")]
    (t/is (thrown? IllegalArgumentException (eus/decrypt "my-key" nil encrypted-sample)))
    (t/is (thrown? IllegalArgumentException (eus/decrypt "my-key" 1 nil)))
    (t/is (thrown? IllegalArgumentException (eus/decrypt "my-key" 1 123)))
    (t/is (thrown? IllegalArgumentException (eus/decrypt nil 1 encrypted-sample)))
    (t/is (thrown? IllegalArgumentException (eus/decrypt ["a" "b"] 1 encrypted-sample)))))

(t/deftest round-trip-happy-case
  (t/is (= 60 (count (eus/encrypt "my-key" 1 "message"))))
  (let [enc-1 (eus/encrypt "my-key" 1 "message")
        enc-2 (eus/encrypt "my-key" 1 "message")]
    (t/is (not= enc-1 enc-2))
    (t/is (= {:expired? false, :state "message", :error? false, :error-message nil}
             (eus/decrypt "my-key" 1 enc-1)))
    (t/is (= {:expired? false, :state "message", :error? false, :error-message nil}
             (eus/decrypt "my-key" 1 enc-2)))
    (t/is (= {:expired? true, :state nil, :error? false, :error-message "Expired"}
             (eus/decrypt "my-key" 2 enc-2)))))

(t/deftest round-trip-happy-case-defaults
  (t/is (>= (count (eus/encrypt "my-key" "message")) 64))
  (let [enc-1 (eus/encrypt "my-key" "message")
        enc-2 (eus/encrypt "my-key" "message")]
    (t/is (not= enc-1 enc-2))
    (t/is (= {:expired? false, :state "message", :error? false, :error-message nil}
             (eus/decrypt "my-key" enc-1)))
    (t/is (= {:expired? false, :state "message", :error? false, :error-message nil}
             (eus/decrypt "my-key" enc-2)))
    (t/is (= {:expired? true, :state nil, :error? false, :error-message "Expired"}
             (binding [eus/-*current-epoch-time* (fn []
                                                   (+ 3601
                                                      (long (/ (System/currentTimeMillis)
                                                               1000))))]
               (eus/decrypt "my-key" enc-2))))))
