(ns ch2)


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



