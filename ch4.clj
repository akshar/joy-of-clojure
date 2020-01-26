(ns ch4)

(class 3.14159265358979323846264338327950288419716939937M)


;; Promotion

(def clueless 9)

(class clueless) ;; => java.lang.Long

(class (+ clueless 90000000000)) ;; => java.lang.Long

(class (+ clueless 900000000000000000000000000)) ;; promotes to BigInt => clojure.lang.BigInt

(+ (Long/MAX_VALUE) (Long/MAX_VALUE)) ;; Integer overflow exception

(unchecked-add (Long/MAX_VALUE) (Long/MAX_VALUE)) ;; => -2
;;Use the unchecked functions only when overflow is desired.


(+ 0.1M 0.1M 0.1M 0.1 0.1M 0.1M 0.1M 0.1M 0.1M 0.1M)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; keywords

(:foo {:foo 1}) ;; can be used as functions

;; can be used as a directive to fn/macro.


;; Qualifying your keywords

::not-in-ns ;; => :ch4/not-in-ns

;; double colons indicates qualified, prefixed keyword


:user/in-ch4 ;; => :user/in-ch4  ;; create keyword "inc-ch4" with prefix ":user"

;; Always use the default NS as prefix. (recommended)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Symbolic Resolution:

;; symbols are identifiers that refer to other things.

;; symbol can be referred directly using quote function or ' special operator.

;;The identical? function in Clojure only ever returns true when the symbols are the same object


(let [x 'goat y x]
  (identical? x y)) ;; => true

(identical? 'goat 'goat) ;; => false

;; metadata

;; clojure lets you attach metadata to various objects.

(let [x (with-meta 'goat {:ornery true})
      y (with-meta 'goat {:ornery false})]
  [(= x y)
   (identical? x y)
   (meta x)
   (meta y)])
;; => [true false {:ornery true} {:ornery false}]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; symbols and namespaces.

(def foo 'where-ami-i)

foo ;; => #'ch4/foo

(resolve 'foo) ;; => #'ch4/foo

`foo ;; => ch4/foo


