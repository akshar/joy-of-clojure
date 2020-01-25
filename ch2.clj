(ns ch2
  (:require
   [clojure.set :refer [union] :rename {union onion}])  ;; refer & rename vars
  (:import
   [java.util HashMap]
   [java.util.concurrent.atomic AtomicLong])) 


;; Quote '
;; Prevents its argument & subforms from being evaluated.

(quote (cons 1 (2 3))) ; => (cons 1 (2 3))

;; literal list as a data collection without having Clojure try to call a function

(quote (1 2)) ;;> (1 2)

(cons 1 (quote (2 3))) ;; (1 2 3)

(cons 1 '(2 3)) ;;shorthand

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Syntax-quote `

;; same as quote with extra features like:

;; 1) Symbol auto-qualification

`map ;; clojure.core/map

`Integer ;; java.lang.Integer


`reduce
;; => clojure.core/reduce

`random-symbol
;; => ch2/random-symbol evals to current namespace

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Unquote ~

;; when you want some form to be evaluated
`(+ 10 (* 3 2))
;; => (clojure.core/+ 10 (clojure.core/* 3 2))

`(+ 10 ~(* 3 2))
;; => (clojure.core/+ 10 6)

;; Unquote is also used to denote any clojure expression requiring evaluation

(let [x 2]
  `(1 ~x 3))
;; => (1 2 3)

(let [x '(2 3)]
  `(1 ~x))
;; => (1 (2 3))

;; unquote splicing : unpack sequecnce x

(let [x '(2 3)]
  `(1 ~@x))
;; => (1 2 3)

;; Auto-gensym ;;generate unique symbol for a parameter or let local name

`foo#
;; => foo__7364__auto__

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Java interop:

;;Accessing static classes symbols

java.util.Locale/JAPAN
;; => #object[java.util.Locale 0x7097dba7 "ja_JP"]

(Math/sqrt 9) ; => 3

;; Creating instances

(new java.util.HashMap {"foo" 42})

;; creating instances preferred form

(java.util.HashMap. {"foo" 42})

;; to access public field name


(.-x (java.awt.Point. 10 20))
;; => 10

;; to access instance method

(.concat "foo" "bar");; => "foobar"

;; setting instance fields

(let [origin (java.awt.Point. 0 0)]
  (set! (.-x origin) 15)
  (str origin));; => "java.awt.Point[x=15,y=0]"

;; doto macro

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; java.util.HashMap props = new java.util.HashMap();                 ;;
;; props.put("HOME", "/home/me");        /* More java code. Sorry. */ ;;
;; props.put("SRC",  "src");                                          ;;
;; props.put("BIN",  "classes");                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; using doto

(doto (java.util.HashMap.)
  (.put "foo" "bar")
  (.put "answer" 42)) ;; => {"answer" 42, "foo" "bar"}




(onion #{1} #{2}) ;; refer+rename
