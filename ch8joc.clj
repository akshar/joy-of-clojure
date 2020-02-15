(ns ch8joc)

;;times of Clojure: read time, macro-expansion time, compile time, and runtime. Macros perform the bulk of their work at compile time.

;;With Clojure, there’s no distinction between the textual form and the actual form of a program. When a program is the data that composes the program, then you can write programs to write programs.

(defn contextual-eval [ctx expr]
  (eval
   `(let [~@(mapcat (fn [[k v]] [k `'~v]) ctx)]
      ~@expr)))

(contextual-eval '{a 1, b 2} '(+ a b)) ;; => 2


(let [x 9, y '(- x)]
  (println `y)
  (println ``y)
  (println ``~y)
  (println ``~~y)
  (contextual-eval {'x 36} ``~~y))


(defmacro do-until [& clauses]
  (when clauses
    (list 'clojure.core/when (first clauses)
          (if (next clauses)
            (second clauses)
            (throw (IllegalArgumentException.
                    "do until requires even number of forms"))))))

(macroexpand-1 '(do-until true (prn 1) false (prn 2)))


;;The most obvious advantage of macros over higher-order functions is that the former manipulate compile-time forms, transforming them into runtime forms. This allows your programs to be written in ways natural to your problem domain, while still maintaining runtime efficiency


(defmacro do-until [& clauses]
  (prn clauses)
  `(when clauses
     (when (first clauses)
       (if (next clauses)
         (second clauses)
         (throw (IllegalArgumentException.
                 "do until requires even number of forms")))
       (do-until (next clauses)))))


(macroexpand-1 '(do-until true (prn 1) false (prn 2)))

(defmacro unless [con & body]
  `(when (not ~con)
     ~@body))


(macroexpand-1 '(unless (even? 3) (prn "foo")))



(unless false (prn "foo"))

;; Macros combining forms

(defmacro def-watched [name & value]
  `(do
     (def ~name ~@value)
     (add-watch (var ~name)
                :re-bind
                (fn [~'key ~'r old# new#]
                  (println old# " -> " new#)))))


(def-watched x (* 10 10))


(def x 0)

;;; using macro to change forms

(defmacro domain [name & body]
  `{:tag :domain
    :attrs {:name (str '~name)}
    :content [~@body]})


(declare handle-things)

(defmacro grouping [name & body]
  `{:tag :grouping
    :attrs {:name (str '~name)}
    :content [~@(handle-things body)]})

(declare grok-attrs grok-props)

(defn handle-things [things]
  (for [t things]
    {:tag :thing,
     :attrs (grok-attrs (take-while (comp not vector?) t))
     :content (if-let [c (grok-props (drop-while (comp not vector?) t))]
                [c]
                [])}))


(defn grok-attrs [attrs]
  (into {:name (str (first attrs))}
        (for [a (rest attrs)]
          (cond
            (list? a) [:isa (str (second a))]
            (string? a) [:comment a]))))



(defn grok-props [props]
  (when props
    {:tag :properties, :attrs nil,
     :content (apply vector (for [p props]
                              {:tag :property,
                               :attrs {:name (str (first p))},
                               :content nil}))}))

(def x 9)

(defmacro resolution [] `x)


(macroexpand '(resolution)) ;; => ch8joc/x


(let [x 109] (resolution))

;;anaphora

(defmacro awhen [expr & body]
  `(let [~'it ~expr]
     (if ~'it
       (do ~@body))))

(awhen [1 2 3] (it 2))


(defmacro with-resource [binding close-fn & body]
  `(let ~binding
     (try
       (do ~@body)
       (finally
         (~close-fn ~(binding 0))))))


(let [stream (joc-www)]
  (with-resource [page stream]
    #(.close %)
    (.readLine page)))

(contract doubler
          [x]
          (:require
           (pos? x))
          (:ensure
           (= (* 2 x ) %)))

(declare collect-bodies)








(declare build-contract)


(defn build-contract [c]
  (let [args (first c)]
    (list
     (into '[f] args)
     (apply merge
            (for [con (rest c)]
              (cond (= (first con) 'require)
                    (assoc () :pre (vec (rest con)))
                    (= (first con) 'ensure)
                    (assoc () :post (vec (rest con)))
                    :else (throw (Exception.
                                  (str "unknown args"))))))
     (list* 'f args))))

(defn collect-bodies [forms]
  (for [form (partition 3 forms)]
    (build-contract form)))

(defmacro contract [name & forms]
  (list* `fn name (collect-bodies forms)))

(fn doubler
  ([f x]
   {:post [(= (* 2 x) %)],
    :pre [(pos? x)]}
   (f x)))

(def doubler-contractor
  (contract doubler
            [x]
            (require
             (pos? x))
            (ensure
             (= (* 2  x) %))))




























