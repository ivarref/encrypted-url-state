(ns com.github.ivarref.pretty-stuff
  (:require [clj-commons.pretty.repl :as pretty]
            [clojure.java.io :as io]
            [clojure.test :as t])
  (:import (java.io File)))

(set! *warn-on-reflection* true)

(pretty/install-pretty-exceptions)

(def default-frame-rules
  "The set of rules that forms the default for [[*default-frame-rules*]], and the
  basis for [[*default-frame-filter*]], as a vector of vectors.

 Each rule is a vector of three values:

 * A function that extracts the value from the stack frame map (typically, this is a keyword such
 as :package or :name). The value is converted to a string.
 * A string or regexp used for matching.  Strings must match exactly.
 * A resulting frame visibility (:hide, :omit, :terminate, or :show).

 The default rules:

 * omit everything in `clojure.lang`, `java.lang.reflect`, and the function `clojure.core/apply`
 * hide everything in `sun.reflect`
 * terminate at `speclj.*`, `clojure.main/main*`, `clojure.main/repl/read-eval-print`, or `nrepl.middleware.interruptible-eval`
 "
  [[:package "clojure.lang" :omit]
   [:package #"sun\.reflect.*" :hide]
   [:package "java.lang.reflect" :omit]
   [:name #"speclj\..*" :terminate]
   [:name "clojure.core/apply" :omit]
   [:name #"clojure.*" :omit]
   [:name #".*short\-stacktrace$" :omit]
   [:package #"java.*" :omit]
   [:name #"cognitect\.test.*" :omit]
   [:name #"nrepl\.middleware\.interruptible-eval/.*" :terminate]
   [:name #"clojure\.main/repl/read-eval-print.*" :terminate]
   [:name #"clojure\.main/main.*" :terminate]])

(defn new-testing-vars-str
  [m]
  (let [{:keys [file line]} m]
    (str
      ;; Uncomment to include namespace in failure report:
      ;;(ns-name (:ns (meta (first *testing-vars*)))) "/ "
      (reverse (map #(:name (meta %)) t/*testing-vars*))
      (str ". (" file ":" line ")"))))

(def is-intellij
  (.exists ^File (io/file ".idea/")))

(defn short-stacktrace [f]
  (let [org-val t/testing-vars-str]
    (try
      (when is-intellij
        (alter-var-root #'t/testing-vars-str (constantly new-testing-vars-str)))
      (binding [clj-commons.format.exceptions/*default-frame-rules* default-frame-rules]
        (f))
      (finally
        (when is-intellij
          (alter-var-root #'t/testing-vars-str (constantly org-val)))))))
