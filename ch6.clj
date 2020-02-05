(ns ch6)

;; Immutability.


;; By providing immutable data structures as a primary language feature, Clojure separates (Braithwaite 2007) the complexity of working with immutable structures from the complexities of their implementation.


;;; structural sharing:

(def baselist (list :barnabas :adam))

(def lst1 (cons :willie baselist))

(def lst2 (cons :phoenix baselist))

lst1
;; => (:willie :barnabas :adam)

lst2;; => (:phoenix :barnabas :adam)

(= (next lst2) (next lst1))
;; => true

(identical? (next lst1) (next lst2))
;; => true



;;;;;;;;;;;;;;;;;;;;;;;;;;;; tree (structural sharing)

{:val 5, :L nil, :R nil} ;; value left-branch right-branch
;;This is exactly the kind of tree you want to return when a single item is added to an empty tree. To represent an empty tree, you’ll use nil


(defn xconj [t v]
  (cond
    (nil? t) {:val v :L nil :R nil}
    (< v (:val t)) {:val (:val t)
                    :L (xconj (:L t) v)
                    :R (:R t) }
    :else {:val (:val t)
           :L (:L t)
           :R (xconj (:R t) v)}))


(def tree1 (xconj nil 5))


(def tree1 (xconj tree1 3))

(def tree1 (xconj tree1 2))

(def tree2 (xconj tree1 7))


(defn xseq [t]
  (when t
    (concat (xseq (:L t)) [(:val t)] (xseq (:R t)))))


(xseq tree1);; => (2 3 5)
(xseq tree2)  ;; => (2 3 5 7)

(:L tree1) ;; => {:val 3, :L {:val 2, :L nil, :R nil}, :R nil}
(:L tree2) ;; => {:val 3, :L {:val 2, :L nil, :R nil}, :R nil}


(identical? (:L tree1) (:L tree2)) ;; => true  structural sharing

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; lazy-seq

(defn rec-step [[x & xs]]
  (if x
    [x (rec-step xs)]
    []))

(rec-step [1 2 3]) ;; => [1 [2 [3 []]]]

(rec-step (range 200000)) ;; stackoverflow

;; Lazy-seq recipe
;; 1) use lazy seq macro at the outermost layer of you lazy-seq producing function
;; 2) Use `rest` (not `next`) while consuming another seq during operations.
;; 3) prefer higher order function when procesing seq
;; 4) Don't hold on to your head.

(take 5 (iterate inc 1))

(def very-lazy (-> (iterate #(do (print \.) (inc %)) 1)
                   rest rest rest))

(def less-lazy (-> (iterate #(do (print \.) (inc %)) 1)
                   next next next))



(defn lazy-rec-steps [coll]
  (lazy-seq
   (if (seq coll)
     [(first coll) (lazy-rec-steps (rest coll))]
     [])))

(lazy-rec-steps [1 2 3]) ;; => (1 (2 (3 ())))


(defn simple-range [i limit]
  (lazy-seq
   (when (< i limit)
     (cons i (simple-range (inc i) limit)))))


(simple-range 0 10)
;; => (0 1 2 3 4 5 6 7 8 9)


(take 2 (simple-range 0 10)) ;; => (0 1)


;; Triangle number:

(defn triangle [n]
  (/ (* n (+ n 1)) 2))

(triangle 10)

(map triangle (range 1 11))

;; infinites seqs fostering declarative solutions:

(def tri-nums (map triangle (iterate inc 1)))

(take 10 tri-nums) ;; first 10

(take 10 (filter even? tri-nums)) ;; first 10 even number

(nth tri-nums 99)

(take 2 (drop-while #(< % 10000) tri-nums)) ;;first 2 greator than 10,000

;;;;;;; quicksort O(nlogn)avg case. O(n^2) worstcase

;; Randomized version



(defn rand-ints [n]
  (take n (repeatedly #(rand-int n))))

(defn sort-parts [work]
  (lazy-seq
   (loop [[part & parts] work]
     (if-let [[pivot & xs] (seq part)]
       (let [smaller? #(< % pivot)]
         (recur (list*
                 (filter smaller? xs)
                 pivot
                 (remove smaller? xs)
                 parts)))
       (when-let [[x & parts] parts]
         (cons x (sort-parts parts)))))))

(defn qsort [xs]
  (sort-parts (list xs)))

(qsort [1 10 16 3 6])


(defn lazy-fibo
  ([] (lazy-fibo 1 1))
  ([x y]
   (lazy-seq
    (cons x (lazy-fibo y (+ x y))))))

(take 5 (lazy-fibo)) ;; => (1 1 2 3 5)

