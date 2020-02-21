(ns udp
  (:refer-clojure :exclude [get cat]))

;;ch9 cont..
;;The UDP is a reusable blueprint for designing software to fit any situation by building on a simple data (maps) and lookup model.

;;The UDP is built on the notion of a map or map-like object


(defn beget [this proto]
  (assoc this ::prototype proto))

(beget {:sub 0} {:super 1}) ;; => {:sub 0, :udp/prototype {:super 1}}

(:a {:a nil :b 1}) ;; => nil indicates key not present or value of :a is nil ?

(find {:a nil :b 1} :a) ;; => [:a nil] ;; implies :a has nil value

(defn get [m k]
  (when m
    (if-let [[_ v] (find m k)]
      v
      (recur (::prototype m) k))))

(get (beget {:sub 0} {:super 1}) :super) ;; => 1

(def cat {:likes-dogs true, :ocd-bathing true})


(def morris (beget {:likes-9live-cat true} cat)) ;; => #'udp/morris
(def put assoc)

morris


;; => {:likes-9live-cat true,
;;     :udp/prototype {:likes-dogs true, :ocd-bathing true}}

(def post-traumatic-morris (beget {:likes-dogs nil} morris))

post-traumatic-morris
;; => {:likes-dogs nil,
;;     :udp/prototype
;;     {:likes-9live-cat true,
;;      :udp/prototype {:likes-dogs true, :ocd-bathing true}}}


(get cat :likes-dogs);; => true

(get morris :likes-dogs);; => true

(get post-traumatic-morris :likes-dogs);; => nil

;; above implementation of the UDP contains no notion of self-awarness via an implicit  this or self reference.

;;;;;;;;;;;;;;;;;;;;;;;;;;

;; multimethods:

(defmulti compiler :os)

(defmethod compiler ::unix [m] (get m :c-compiler))

(defmethod compiler ::osx [m] (get m ::llvm-compiler))

(def clone (partial beget {}))


(def unix {:os ::unix :c-compiler "cc" :home "/home" :dev "/dev"})


(def osx (-> (clone unix)
             (put :os ::osx)
             (put ::llvm-compiler "clang")
             (put :home "/users")))

(compiler unix) ;; => "cc"

(compiler osx) ;; => "clang"


;; ad-hoc hierarchies

(defmulti home :os)

(defmethod home ::unix [m] (get m :home))

(home unix) ;; => "/home"

;;Clojure allows you to define a relationship stating “::osx is a ::unix” and have the derived function take over the lookup behavior using Clojure’s derive function:

(derive ::osx ::unix)


(home osx) ;; => "/users"

(parents ::osx);; => #{:udp/unix}

(ancestors ::osx) ;; => #{:udp/unix}

(descendants ::unix) ;; => #{:udp/osx}

(isa? ::osx ::unix);; => true

(isa? ::unix ::osx);; => false

;; conflicting hierarchies


(derive ::osx ::bsd)


(defmethod home ::bsd [m] "/home")


(home osx) ;;exception

(prefer-method home ::unix ::bsd)


(home osx)  ;; => "/users"


(remove-method home ::bsd)

(home osx) ;; => "/users"


;; defnining custom hierarchies

;; (derive (make-hierarchy) ::osx ::unix)
;; => {:parents #:udp{:osx #{:udp/unix}},
;;     :ancestors #:udp{:osx #{:udp/unix}},
;;     :descendants #:udp{:unix #{:udp/osx}}}




;; arbitary dispatch

(defmulti compile-cmd (juxt :os compiler))

(defmethod compile-cmd [::osx "gcc"] [m]
  (str "/usr/bin" (get m :c-compiler)))

(defmethod compile-cmd :default [m]
  (str "unsure to locate" (get m :c-compiler)))


(compile-cmd osx) 




;; JUXT

(def each-match (juxt + * - /))

(each-match 2 3) ;; => [5 6 -1 2/3]

((juxt take drop) 3 (range 9)) ;; => [(0 1 2) (3 4 5 6 7 8)]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; types, protocols & records.

;; Records:

(defrecord TreeNode [val l r])

(TreeNode. 5 nil nil) ;; => #udp.TreeNode{:val 5, :l nil, :r nil}

;; (ns my-cool-ns
;;   (:import udp.TreeNode)) importing it in other namespaces


;; A record is created more quickly, consumes less memory, and looks up keys in itself more quickly than the equivalent array map or hash map. 


(defn xconj-record [t v]
  (cond
    (nil? t) (TreeNode. v nil nil)
    (< v (:val t)) (TreeNode.  (:val t)
                               (xconj-record (:l t) v)
                               (:r t)) 
    :else (TreeNode.  (:val t)
                      (:l t)
                      (xconj-record (:r t) v))))

(defn xseq [t]
  (when t
    (concat (xseq (:l t)) [(:val t)] (xseq (:r t)))))

(def sample-tree (reduce xconj-record nil [3 5 2 4 6]))

(prn  sample-tree)

;;#udp.TreeNode{:val 3, :l #udp.TreeNode{:val 2, :l nil, :r nil}, :r #udp.TreeNode{:val 5, :l #udp.TreeNode{:val 4, :l nil, :r nil}, :r #udp.TreeNode{:val 6, :l nil, :r nil}}}


(xseq sample-tree) ;; => (2 3 4 5 6);; => 




;;Perhaps more surprisingly, dissocing a key given in the record works but returns a regular map rather than a record.

(def t (TreeNode. 5 nil nil))
(def t2 (xconj-record t 10))



;; => #udp.TreeNode{:val 5,
;;                  :l nil,
;;                  :r #udp.TreeNode{:val 10, :l nil, :r nil}}


;; protocols

;; A protocol in Clojure is a set of function signatures, each with at least one parameter, that are given a collective name.

;;a class that claims to implement a particular protocol should provide specialized implementations of each of the functions in that protocol.


;; stack(filo) & queues(fifo) protocol

(defprotocol FIXO
  (fixo-push [fixo value])
  (fixo-pop [fixo])
  (fixo-peek [fixo]))


(extend-type TreeNode
  FIXO
  (fixo-push [node value]
    (xconj-record node value))  )



(xseq (fixo-push sample-tree 5/2)) ;; => (2 5/2 3 4 5 6)



(extend-type clojure.lang.IPersistentVector
  FIXO
  (fixo-push [vector value]
    (conj vector value)))

(fixo-push [2 3 4 5 6] 5/2) ;; => [2 3 4 5 6 5/2]

;; Here you’re extending FIXO to an interface instead of a concrete class. This means fixo-push is now defined for all classes that inherit from IPersistentVector


;; clojure style mixins


(use 'clojure.string)

(defprotocol StringOps
  (rev [s])
  (upp [s]))


(extend-type String
  StringOps
  (rev [s]
    (clojure.string/reverse s)))


(extend-type String
  StringOps
  (upp [s]
    (clojure.string/upper-case s)))

(rev "abc") ;; => "cba"

(upp "foo") ;; => "FOO"

(rev "Works?") ;; IllegalArgumentException No implementation of method: :rev
                                        ;   of protocol: #'user/StringOps found for
                                        ;     class: java.lang.String


;;The reason for this exception is that for a protocol to be fully populated (all of its functions callable), it must be extended fully, per individual type.


(def rev-mixin {:rev clojure.string/reverse})

(def upp-mixin {:upp (fn [this] (.toUpperCase this))})

(def fully-mixed (merge upp-mixin rev-mixin))

(extend String
  StringOps
  fully-mixed)

(-> "Works" upp rev) ;; => "SKROW"


;;Mixins in Clojure refer to the creation of discrete maps containing protocol function implementations that are combined in            such a way as to create a complete implementation of a protocol.

;;What we’ve just done is impossible with Java interfaces or C++ classes, at least in the order we did it. With either of those         languages, the concrete type (such as TreeNode or vector) must name at the time it’s defined all the interfaces or classes it’s going to implement. Here we went the other way around—both TreeNode and vectors were defined before the FIXO protocol even existed, and we easily extended FIXO to each of them.



;;Clojure polymorphism lives in the protocol functions, not in the classes


(reduce fixo-push nil [3 5 2 4 6 0]);; =>  exception cuz the first argument should be protocol dispatch type.

(extend-type nil
  FIXO
  (fixo-push [t v]
    (TreeNode. v nil nil)))

(xseq (reduce fixo-push nil [3 5 2 4 6 0])) ;; => (0 2 3 4 5 6)

(extend-type TreeNode
  FIXO
  (fixo-push [node value]
    (xconj-record node value))
  (fixo-peek [node]
    (if (:l node)
      (recur (:l node))
      (:val node)))
  (fixo-pop [node]
    (if (:l node)
      (TreeNode. (:val node) (fixo-pop (:l node)) (:r node))
      (:r node))))

(extend-type clojure.lang.IPersistentVector
  FIXO
  (fixo-push [vector value]
    (conj vector value))
  (fixo-peek [vector]
    (peek vector))
  (fixo-pop [vector]
    (pop vector)))

(fixo-push [1 2 3] 4) ;; => [1 2 3 4]
(fixo-pop [1 2 3]) ;; => [1 2]

(fixo-peek sample-tree)


;;Clojure doesn’t encourage implementation inheritance, so although it’s possible to inherit from concrete classes as needed         for Java interoperability,[7] there’s no way to use extend to provide a concrete implementation and then build another class on top of that


;; refiy

(defn fixed-fixo
  ([limit] (fixed-fixo limit []))
  ([limit vector]
   (reify FIXO
     (fixo-push [this value]
       (if (< (count vector) limit)
         (fixed-fixo limit (conj vector value))
         this))
     (fixo-peek [_]
       (peek vector))
     (fixo-pop [_]
       (pop vector))))) ;;factory method to run fixo instances with limit. reify to statisy the protocol.


;; protocol and interface implementation directly inside defrecord form.

(defrecord TreeNode [val l r]
  FIXO
  (fixo-push [node value]
    (xconj-record node value))

  (fixo-peek [node]
    (if l
      (recur l)
      val))
  (fixo-pop [node]
    (if l
      (TreeNode. val (fixo-pop l) r)
      r)))

;;Putting method definitions inside the defrecord form also allows you to implement Java interfaces and extend java.lang.Object, which isn’t possible using any extend form

;; deftype

(deftype InfiniteConstant [i]
  clojure.lang.ISeq
  (seq [this]
    (lazy-seq (cons i (seq this)))))

(take 3 (InfiniteConstant. 5))


(.i (InfiniteConstant. 5))


;; fluent builder:

(defn build-move [& pieces]
  (apply hash-map pieces))

(build-move :from "e7" :to "e8" :promotion \Q) ;; => {:from "e7", :promotion \Q, :to "e8"}

;; overriding toString

(defrecord Move [from to castle? promotion]
  Object
  (toString [this]
    (str "Move " (:from this)
         " to " (:to this)
         (if (:castle? this) " castle"
             (if-let [p (:promotion this)]
               (str " promote to " p) "")))))

(str  (Move. "e2" "e4" nil nil)) ;; => "Move e2 to e4"





