(ns ch7)

;; FFFFFFFFFFFFFFF Programming

(map [:a :b :c :d :e] #{0 3}) ;; => (:a :d) ; vector as fn


(map (comp
      keyword
      #(.toLowerCase %)
      name)
     '(a B c)) ;; => (:a :b :c)

;;Splitting functions into smaller, well-defined pieces fosters composability and, as a result, reuse


((complement even?) 2) ;; => false

(interpose "," [1 2 3])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;Metadata

;; defn meta

(defn ^:private ^:dynamic sum [nums]
  (map + nums))

;; same as

(defn ^{:private true :dynamic true} sum [nums]
  (map + nums))

;; Higher-order functions

;;Takes one or more functions as arguments
;;Returns a function as a result

(sort [1 5 7 0 -42 13]) ;; => (-42 0 1 5 7 13)

(sort ["z" "x" "a" "aa"]) ;; => ("a" "aa" "x" "z")

(sort [(java.util.Date.) (java.util.Date. 100)])
;; => (#inst "1970-01-01T00:00:00.100-00:00"
;;     #inst "2020-02-06T06:18:05.106-00:00")

(java.util.Date. 100)
;; => #inst "1970-01-01T00:00:00.100-00:00"

(sort > [7 1 4])
;; => (7 4 1)

(sort ["z" "x" "a" "aa" 1 5 8])
;; => exception



(sort-by second [[:a 15], [:c 13], [:b 21]])
;; => ([:c 13] [:a 15] [:b 21])

(sort-by str ["z" "x" "a" "aa" 1 5 8])
;; => (1 5 8 "a" "aa" "x" "z")

(sort-by :age [{:age 99}, {:age 13}, {:age 7}])
;; => ({:age 7} {:age 13} {:age 99})

;;;;;;;Functions as return values

(def plays [{:band "Burial",     :plays 979,  :loved 9}
            {:band "Eno",        :plays 2333, :loved 15}
            {:band "Bill Evans", :plays 979,  :loved 8}
            {:band "Magma",      :plays 2665, :loved 31}])

(defn columns [column-names]
  (fn [row]
    (vec (map row column-names))))

((columns [:plays :loved :band]) {:band "Burial", :plays 979, :loved 9}) ;; => [979 9 "Burial"]


(sort-by (columns [:plays :loved :band]) plays)
;; => ({:band "Bill Evans", :plays 979, :loved 8}
;;     {:band "Burial", :plays 979, :loved 9}
;;     {:band "Eno", :plays 2333, :loved 15}
;;     {:band "Magma", :plays 2665, :loved 31})


(defn slope
  [& {:keys [p1 p2] :or {p1 [0 0] p2 [1 1]}}]
  [p1 p2])


(slope :p1 [4 15] :p2 [3 21]) ;; => [[4 15] [3 21]]

;; use & flag to pass variable args

;; pre/post conditions

(defn slope [p1 p2]
  {:pre [(not= p1 p2) (vector? p1) (vector? p2)]
   :post [(float? %)]}
  (/ (- (p2 1) (p1 1))
     (- (p2 0) (p1 0))))

(slope [10 10] [10 10]) ;; assert failed (not= p1 p2)

(slope [10 10] '(10 5)) ;; assert failed (vector? p2)

;; (set! *assert* false) to disable assertion for current ns

(into {:a 1} {:b 2 :c 3})


;; abstracting constraints

(defn put-things [m]
  (into m {:meat "bar" :veggie "broccoli"}))

(defn vegan-constraints [f m]
  {:pre [(:veggie m)]
   :post [(:veggie %) (nil? (:meat %))]}
  (f m))

(defn balanced-diet [f m]
  {:post [(:veggie %) (:meat %)]}
  (f m))

(vegan-constraints put-things {:veggie "carrot"})

(balanced-diet put-things {:veggie "foo" :meat "bar"})

;;;;;;;;;;;;;;;;closure is a function that has access to locals from the context where it was created:


(def times-two
  (let [x 2]
    (fn [y] (* y x)))) ;; inner fn has access to x defined outside fn body.The function is said to close over the local x

(times-two 5) ;; => 10


(def add-and-get
  (let [ai (java.util.concurrent.atomic.AtomicInteger.)]
    (fn [y] (.addAndGet ai y))))


(add-and-get 2)

(add-and-get 2)

;;A point of note about this technique is that when closing over something mutable, you run the risk of making your functions impure and thus more difficult to test and reason about, especially if the mutable local is shared.


(def bearings [{:x  0, :y  1}    ; north
               {:x  1, :y  0}    ; east
               {:x  0, :y -1}    ; south
               {:x -1, :y  0}])  ; west

(defn forward [x y bearing-num]
  [(+ x (:x (bearings bearing-num)))
   (+ y (:y (bearings bearing-num)))])

;; (north) at 5,5
(forward 5 5 0) ;; => [5 6]

(defn bot [x y bearing-num]
  {:coords  [x y]
   :bearing ([:north :east :south :west] bearing-num)
   :forward (fn [] (bot (+ x (:x (bearings bearing-num)))
                        (+ y (:y  (bearings bearing-num)))
                        bearing-num))

   :turn-right (fn [] (bot x y (mod (+ 1 bearing-num) 4)))
   :turn-left (fn [] (bot x y (mod (- 1 bearing-num) 4)))})


(:coords (bot 5 5 0)) ;; => [5 5]
(:bearing (bot 5 5 0)) ;; => :north

(:coords ((:forward (bot 5 5 0))))

;;;;;; recursion

;; mundane recursion
(defn pow [n p]
  (if (= p 0)
    1
    (* n (pow n (dec p)))))


(pow 2 10)

;; tail recursion

(defn pow [n p]
  (letfn [(kapow [n p acc]
            (if (zero? p)
              acc
              (recur n (dec p) (* acc n))))]
    (kapow n p 1)))


(pow 2 10)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def simple-metric {:meter 1,
                    :km 1000,
                    :cm 1/100,
                    :mm [1/10 :cm]})


(defn convert [context descriptor]
  (reduce (fn [result [mag unit]]
            (+ result
               (let [val (get context unit)]
                 (if (vector? val)
                   (* mag (convert context val))
                   (* mag val)))))
          0
          (partition 2 descriptor)))


(convert simple-metric [1 :meter])

(float (convert simple-metric [3 :km 10 :meter 80 :cm 10 :mm]))


(convert {:bit 1, :byte 8, :nibble [1/2 :byte]} [32 :nibble])


;;https://codurance.com/2017/12/05/on-tail-call-optimisation/

;;;;;;trampoline






(defn elevator [commands]
  (letfn
      [(ff-open [[_ & r]]
         "When elevator is open on the 1st floor it can either close or be done"
         #(case _
            :close (ff-closed r)
            :done true
            false))
       (ff-closed [[_ & r]]
         "When elevator is closed on the 1st floor it can either open or go up"
         #(case _
            :open (ff-open r)
            :up (sf-closed r)
            false))

       (sf-closed [[_ & r]]
         "When elevator is closed on the 1st floor it can either open or go up"
         #(case _
            :down (ff-closed r)
            :open (sf-open r)
            false))

       (sf-open [[_ & r]]
         "When elevator is closed on the 1st floor it can either open or go up"
         #(case _
            :close (sf-closed r)
            :done true
            false))]
    (trampoline ff-open commands)))

(elevator [:close :open :close :up :open :open :done])

(elevator [:close :up :open :close :down :open :done])


;; continuation-passing style - CPS

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Accept —Decides when a computation should terminate     ;;
;; Return —Wraps the return values                         ;;
;; Continuation —Provides the next step in the computation ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fac-cps [n k]
  (letfn [(cont [v]  (k (* v n)))]
    (if (zero? n)
      (k 1)
      (recur (dec n) cont))))


(defn fac [n]
  (fac-cps n identity))

(fac 5)


(defn neighbors
  ([size yx] (neighbors [[-1 0] [1 0] [0 -1] [0 1]]
                        size
                        yx))

  ([deltas size yx]
   (filter (fn [new-yx]
             (every? #(< -1 % size) new-yx))
           (map #(vec (map + yx %))
                deltas))))


;;;;;; A* path finding.

;;             0   1   2   3    4
(def world [[  1   1   1   1    1]    ;0
            [999 999 999 999    1]    ;1
            [  1   1   1   1    1]    ;2
            [  1 999 999 999  999]    ;3
            [  1   1   1   1    1]])  ;4


(neighbors 5 [0 0])

(defn estimate-cost [step-cost-est size y x]
  (* step-cost-est
     (- (+ size size) y x 2)))


(defn path-cost [node-cost cheapest-nbr]
  (+ node-cost
     (or (:cost cheapest-nbr) 0)))

(path-cost 900 {:cost 1})

(defn total-cost [newcost step-cost-est size y x]
  (+ newcost
     (estimate-cost step-cost-est size y x)))











