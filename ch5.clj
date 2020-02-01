(ns ch5)

;; Seq API for navigating collections:

;; Api consists of two functions frist and rest.

;; first =>  returns the first item or nil if empty.
;; rest => returns the sequence without first item or empty list

;; A seq is any object that implements the seq API, thereby supporting the functions first and rest

;; clojure.core/seq => a function that returns an object implementing the seq API.

;;Clojure classifies each collection data type into one of three logical categories or partitions: sequentials, maps, and sets.
;; These divisions draw clear distinctions between the types and help define equality semantics.



;;If two sequentials have the same values in the same order, = returns true for them, even if their concrete types are different, as shown:

(= [1 2 3] '(1 2 3))
;; => true

;;Conversely, even if two collections have the same exact values, if one is a sequential collection and the other isn’t, = returns false:

(= [1 2 3] #{1 2 3})
;; => false

;; many lisp build their data type on cons-cell abstraction (car & cdr) (first & rest  in clojure)


;;All an object needs to do to be a sequence is to support the two core functions: first and rest.



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Collection types:

;;vector:


[1 2 3] ;; literal syntax

(vec (range 1 5)) ;; out of other type


(into [:a :b] (range 1 3))  ;; using into to merge to vector
;; => [:a :b 1 2]


;; Clojure can store primitive types inside of vectors using the vector-of function

;; takes :int, :long, :float, :double, :byte, :short, :boolean, or :char


(into (vector-of :int) [1 2 3])
;; => [1 2 3]

(vector-of :int 1 2 3)
;; => [1 2 3]

(into (vector-of :char) [100 101 102]);; => [\d \e \f]


;; accessing vector element using nth or get function


(def a-to-j  (vec (map char (range 65 75))))
;; => (\A \B \C \D \E \F \G \H \I \J)

(nth a-to-j 4)
;; => \E

(get a-to-j 100)
;; => \E


(get a-to-j 100 :not-found)
;; => :not-found

(assoc a-to-j 4 "no longer E")
;; => [\A \B \C \D "no longer E" \F \G \H \I \J]

(replace {2 :a, 4 :b} [1 2 3 2 3 4]);; => [1 :a 3 :a 3 :b]



(def matrix
  [[1 2 3]
   [4 5 6]
   [7 8 9]])

(get-in matrix [1 2])
;; => 6

(assoc-in matrix [1 2] 'x)
;; => [[1 2 3] [4 5 x] [7 8 9]]

(update-in matrix [1 2] * 100);; => [[1 2 3] [4 5 600] [7 8 9]]


;;;;vector as stack

(def foo [1 2 3])
;; all the operations on vector happens on the right side.


(conj foo 4) ;; => [1 2 3 4]

(peek foo);; => 3

(pop foo);; => [1 2]

;; using vector instead of reverse.


;;;What vectors aren’t::

;; vectors aren't sparse.You can’t skip some indices and insert at a higher index number.

;;Vectors aren’t queues

;;Vectors aren’t sets

;;Clojure’s contains? is for asking whether a particular key, not value, is in a collection


(contains? [1 2 3 4 5] 5) ;; => false - index 5 - out of range

(contains? [1 2 3 4 5] 4) ;; => true has index 4 - in range

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; LISTS: Clojure’s code-form data structure


;;Lists are almost exclusively used to represent code forms. They’re used literally in code to call functions, macros

;;but what other Lisps call `car` is the same as `first` on a Clojure list. Similarly, `cdr` is the same as `next`. But there are substantial differences as well.

(cons 1 '(2 3))
;; => (1 2 3)

(conj '(2 3) 1)
;; => (1 2 3)

;;In a departure from classic Lisps, the “right” way to add to the front of a list is with conj

;; a list built using conj is homogeneous — all the objects on its next chain are guaranteed to be lists, whereas sequences built with cons only promise that the result will be some kind of seq.

(type (conj nil 1)) ;; => clojure.lang.PersistentList

(type  (cons 1 nil));; => clojure.lang.PersistentList


;;;What lists aren’t

;; Probably the most common misuse of lists is to hold items that will be looked up by index. Clojure has to walk the list from the beginning to find it. Don’t do that

;;Vectors are good at looking things up by index, so use one of those instead


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; persistent sets:

;;Clojure sets work the same as mathematical sets, in that they’re collections of unsorted unique elements.

;;sets as function

(#{:a :b 1 2} :a) ;; => :a

(#{:a :b 1 2} 42) ;; => nil

;;using get

(get #{:a 1 :b 2} 1) ;; => 1

(get #{:a 1 :b 2} :z :nothing-doing) ;; => :nothing-doing


;;  Finding items in a sequence using a set and the some function

(some #{:b} [:a 1 :b 2]) ;; => :b

(some #{1 :b} [:a 1 :b 2]) ;; => 1

;; union/intersection/differences


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Map

;; hashmap

(hash-map :a 1 :b 2) ;; => {:b 2, :a 1}

(:a {:a 1 :b 2});; => 1 ;;hashmap as function


(seq {:a 1 :b 2}) ;; => ([:a 1] [:b 2]) ;; return map entries

(into {} '([:a 1] [:b 2])) ;; => {:a 1, :b 2}

(apply hash-map [:a 1 :b 2]) ;; => {:b 2, :a 1}


;;Another fun way to build a map is to use zipmap to “zip” together two sequences, the first of which contains the desired keys and the second their corresponding values:

(zipmap [:a :b] [1 2]) ;; => {:a 1, :b 2}


;; hasmap does not guarantee order. use sorted-map to maintain order.

(sorted-map "bac" 2 "abc" 9) ;; => {"abc" 9, "bac" 2}

(sorted-map-by #(compare (subs %1 1) (subs %2 1)) "bac" 2 "abc" 9);; => {"bac" 2, "abc" 9}

;; sorted maps don’t generally support heterogeneous keys the same as hash maps, although it depends on the comparison function provided


;; use subseq & rsubseq to walk map forward and backward.



;;Another way that sorted maps and hash maps differ is in their handling of numeric keys.

(assoc {1 :int} 1.0 :float) ;; => {1 :int, 1.0 :float}

(assoc (sorted-map 1 :int) 1.0 :float) ;; => {1 :float}

;; Array map

(seq (hash-map :a 1, :b 2, :c 3)) ;; => ([:c 3] [:b 2] [:a 1])


(seq (array-map :a 1, :b 2, :c 3));; => ([:a 1] [:b 2] [:c 3]) ;; maintains insertion order.

;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 5.7

(defn index [coll]
  (cond
    (map? coll) (seq coll)
    (set? coll) (map vector coll coll)
    :else (map vector (iterate inc 0) coll)))

(index #{:a 1 :b 2 :c 3 :d 4}) ;; => ([1 1] [4 4] [:c :c] [3 3] [2 2] [:b :b] [:d :d] [:a :a])

(index {:a 1 :b 2 :c 3 :d 4}) ;; => ([:a 1] [:b 2] [:c 3] [:d 4])

(index [:a 1 :b 2 :c 3 :d 4]) ;; => ([0 :a] [1 1] [2 :b] [3 2] [4 :c] [5 3] [6 :d] [7 4])

(defn pos [e coll]
  (for [[i v] (index coll)
        :when (= e v)]
    i))

(pos 10 [1 2 3 4 10 7 ]) ;; => (4)

;;We can modify pos only slightly to achieve the ideal level of flexibility.

(defn x-pos [pred coll]
  (for [[i v] (index coll)
        :when (pred v)]
    i))

(x-pos even? [2 3 6 7]) ;; => (0 2)

(x-pos #{3 4} {:a 1 :b 2 :c 3 :d 4}) ;; => (:c :d)
