(ns ch8)

;;eval is evil
(defn contextual-eval [ctx expr]
  (eval
   `(let [~@(mapcat (fn [[k v]] [k `'~v]) ctx)]
      ~expr)))

(contextual-eval '{a 1, b 2} '(+ a b))


(macroexpand '(when boo
                exp-1
                exp-2)) ;; => (if boo (do exp-1 exp-2))

(defmacro infix
  "Use this macro when you pine for the notation of your childhood"
  [infixed]
  (list (second infixed) (first infixed) (last infixed)))

(infix (1 + 1))

(macroexpand '(infix (1 + 1))) ;; => (+ 1 1)

;;Macro writing is all about building a list for Clojure to evaluate

(defmacro my-print-whoopsie
  [expression]
  (list 'let ['result expression]
        (list 'println 'result)
        'result))

(defmacro code-citric-quote ;;using single quote.
  [bad good]
  (list 'do
        (list 'println
              "foo bad:"
              (list 'quote bad))
        (list 'println
              "foo good:"
              (list 'quote good))))




(defmacro code-citric-squote ;;using syntax quote
  [bad good]
  `(do (println "foo bad:"
                (quote ~bad))
       (println "foo good:"
                (quote ~good))))

(code-citric-squote (1 + 1) (+ 1 1))

;;macros receive unevaluated, arbitrary data structures as arguments and return data structures that Clojure evaluates. When defining your macro, you can use argument destructuring just like you can with functions and let bindings. You can also write multiple-arity and recursive macros.

(defn criticize-code
  [criticism code]
  `(println ~criticism (quote ~code)))

(defmacro code-critic-2
  [bad good]
  `(do
     ~(criticize-code "bad code" bad)
     ~(criticize-code "good code" good)))


(code-critic-2 (1 + 1) (+ 1 1))

(defmacro code-critic
  [bad good]
  `(do ~@(map #(apply criticize-code %)
              [["Great squid of Madrid, this is bad code:" bad]
               ["Sweet gorilla of Manila, this is good code:" good]])))

(code-critic (1 + 1) (+ 1 1)) ;; quote unsplicing ~@ to expand list



;; variable capture.

(def message "Good job!")


(defmacro with-mischief
  [& stuff-to-do]
  (concat (list 'let ['message "oh, big deal!"])
          stuff-to-do))


(with-mischief
  (println "foo:" message)) ;; foo: oh, big deal! ;;wrong


(defmacro with-mischief
  [& stuff-to-do]
  `(let [message "oh,big deal!"]
     ~@stuff-to-do))

(with-mischief
  (println "foo:" message))

(gensym)

(defn without-mischief-gensym
  [& stuff-to-do]
  (let [macro-message  (gensym 'message)]
    `(let [~macro-message "oh, big deal!"]
       ~@stuff-to-do
       (println "I still need to say: " ~macro-message))))


(without-mischief-gensym
 (println "here is i feel about the thing you did" message))


(defmacro report
  [to-try]
  `(let [result# ~to-try]
     (if result#
       (println (quote ~to-try) "was successufl: " result#)
       (println (quote ~to-try) "was not successufl: " result#))))


(report (= 1 1))


;;;;;

(def order-details-validations
  {:name
   ["Please enter a name" not-empty]

   :email
   ["Please enter an email address" not-empty

    "Your email address doesn't look like an email address"
    #(or (empty? %) (re-seq #"@" %))]})

(def order-details
  {:name "Mitchard Blimmons"
   :email "mitchard@blimmonsgmail.com"})



(defn error-messages-for
  "Return a seq of error messages"
  [to-validate message-validator-pairs]
  (map first (filter #(not ((second %) to-validate))
                     (partition 2 message-validator-pairs))))

(error-messages-for "" ["Please enter a name" not-empty])



(defn validate
  "Returns a map with a vector of errors for each key"
  [to-validate validations]
  (reduce (fn [errors validation]
            (let [[fieldname validation-check-groups] validation
                  value (get to-validate fieldname)
                  error-messages (error-messages-for value validation-check-groups)]
              (if (empty? error-messages)
                errors
                (assoc errors fieldname error-messages))))
          {}
          validations))

(validate order-details order-details-validations)

(defmacro if-valid
  "Handle validation more concisely"
  [to-validate validations errors-name & then-else]
  `(let [~errors-name (validate ~to-validate ~validations)]
     (if (empty? ~errors-name)
       ~@then-else)))


(defmacro when-valid
  "Handle validation more concisely"
  [to-validate validations & body]
  `(let [errors# (validate ~to-validate ~validations)]
     (if (empty? errors#)
       (do
         ~@body))))

(when-valid order-details order-details-validations
            (println :success))

(defmacro or*
  ([] false)
  ([x] x)
  ([x & next]
   `(let [or# ~x]
      (if or#
        or#
        (or* ~@next)))))
