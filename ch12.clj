(ns ch12
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [com.sun.net.httpserver HttpHandler HttpExchange HttpServer]
           [java.net InetSocketAddress URLDecoder URI]
           [java.io File FilterOutputStream]
           [java.util Comparator Collections ArrayList]
           [java.util.concurrent FutureTask]))

;;(set! *warn-on-reflection* false)

;;Proxy

;;when dealing with Java libraries, you’re at times required to extend concrete classes, and it’s in this circumstance where proxy shines

;;Although extending concrete classes is seen often in Java,doing so in Clojure is considered poor design,[2] leading to fragility, and should therefore be restricted to those instances where interoperability demands it

(def OK java.net.HttpURLConnection/HTTP_OK)

(defn respond
  ([exchange body]
   (respond identity exchange body))
  ([around exchange body]
   (.sendResponseHeaders exchange OK 0)
   (with-open [resp (around (.getResponseBody exchange))]
     (.write resp (.getBytes body)))))


(defn new-server [port path handler]
  (doto
      (HttpServer/create (InetSocketAddress. port) 0)
    (.createContext path handler)
    (.setExecutor nil)
    (.start)))



(defn default-handler [txt]
  (proxy [HttpHandler] ;; extend httphandler
      []
    (handle [exchange]     ;;override
      (respond exchange txt))))

(def p (default-handler "foo bar"))

(def server
  (new-server
   3000
   "/"
   p))


(.stop server 0)


;;Clojure provides a function named update-proxy that takes a proxied instance and a map containing method names to functions that implement their new behaviors.

(update-proxy p {"handle" (fn [this exchange]
                            (respond exchange (str "this is " this)))})

;;log req headers

(def echo-handler
  (fn [_ exchange]
    (let [headers (.getRequestHeaders exchange)]
      (respond exchange (prn-str headers)))))


(update-proxy p {"handle" echo-handler})

;;The question remains—how does update-proxy change the behavior of a previously generated proxy class?


;;Clojure’s proxy function generates the bytecode for an actual class on demand, but it does so in such a way as to provide a more dynamic implementation. Instead of inserting the bytecode for the given method bodies directly into the proxy class, Clojure instead generates a proper proxy in which each method looks up the function implementing the method’s behavior. That is, based on the method name, the corresponding function is retrieved from a map and invoked with the this reference and the remaining argument(s). This trades highly useful dynamic behavior for some runtime cost, but in many cases this is a fair trade.


(defn html-around [o]
  (proxy [FilterOutputStream]
      [o]
    (write [raw-bytes]
      (proxy-super write
                   (.getBytes (str "<html><body>"
                                   (String. raw-bytes)
                                   "</body></html>"))))))

(defn listing [file]
  (-> file .list sort))


(listing (io/file "."))
;; => (".git"
;;     "ch1.clj"
;;     "ch10.clj"
;;     "ch11.clj"
;;     "ch12.clj"
;;     "ch14.clj"
;;     "ch15.clj"
;;     "ch2.clj"
;;     "ch3.clj"
;;     "ch4.clj"
;;     "ch5.clj"
;;     "ch6.clj"
;;     "ch7.clj"
;;     "ch8.clj"
;;     "ch8joc.clj"
;;     "ch9.clj"
;;     "udp.clj")

(defn html-links [root filenames]
  (string/join
   (for [file filenames]
     (str "<a href='"
          (str root
               (if (= "/" root)
                 ""
                 File/separator)
               file)
          "'>"
          file "</a><br>"))))



(html-links "." (listing (io/file ".")))
;; => "<a href='./.#ch12.clj'>.#ch12.clj</a><br><a href='./.git'>.git</a><br><a href='./ch1.clj'>ch1.clj</a><br><a href='./ch10.clj'>ch10.clj</a><br><a href='./ch11.clj'>ch11.clj</a><br><a href='./ch12.clj'>ch12.clj</a><br><a href='./ch14.clj'>ch14.clj</a><br><a href='./ch15.clj'>ch15.clj</a><br><a href='./ch2.clj'>ch2.clj</a><br><a href='./ch3.clj'>ch3.clj</a><br><a href='./ch4.clj'>ch4.clj</a><br><a href='./ch5.clj'>ch5.clj</a><br><a href='./ch6.clj'>ch6.clj</a><br><a href='./ch7.clj'>ch7.clj</a><br><a href='./ch8.clj'>ch8.clj</a><br><a href='./ch8joc.clj'>ch8joc.clj</a><br><a href='./ch9.clj'>ch9.clj</a><br><a href='./udp.clj'>udp.clj</a><br>"



(defn details [file]
  (str (.getName file)
       " is "
       (.length file)
       " bytes"))


(details (io/file "./ch12.clj")) ;; => "ch12.clj is 4395 bytes"


(defn uri->file [root uri]
  (->> uri
       str
       URLDecoder/decode       
       (str root)
       io/file))

(uri->file "." (URI. "/ch12.clj")) ;; => #object[java.io.File 0x2522990e "./ch12.clj"]


(defn fs-handler []
  (fn [_ exchange]
    (let [uri (.getRequestURI exchange)
          file (uri->file "." uri)]
      (if (.isDirectory file)
        (do (.add (.getResponseHeaders exchange)
                  "Content-Type" "text/html")
            (respond html-around
                     exchange
                     (html-links (str uri) (listing file))))
        (respond exchange (details file))))))


(update-proxy p {"handle" fs-handler})

;;Using proxy is powerful, but doing so creates unnamed instances unavailable for later extension. If you instead want to create named classes, you’ll need to use Clojure’s gen-class mechanism.



;;;;;;;;;;gen-class refer joc



;; Java arrays:

;; primitive array:

;;create


(doto (StringBuilder. "abc")
  (.append (into-array [\x \y \z]))) ;; => #object[java.lang.StringBuilder 0x3ff95369 "abc[Ljava.lang.Character;@62e49430"]

;;The problem is that Clojure’s into-array function doesn’t return a primitive array of char[], but instead returns a reference array of Character[], forcing the Clojure compiler to resolve the call to the StringBuilder.append(Object) method instead.


;; That the Array class is a subclass of Object is a constant cause for headache in Java and clearly can be a problem[11] for Clojure as well.



(doto (StringBuilder. "abc")
  (.append (char-array [\x \y \z]))) ;; => #object[java.lang.StringBuilder 0x38c4ac04 "abcxyz"] ;; Object = reference a

;; clj primitive array building functions boolean-array, byte-array, char-array, double-array, float-array, int-array, long-array, short-array.

;;You can also use the make-array and into-array functions to create primitive arrays:


;;You can also use the make-array and into-array functions to create primitive arrays:

(let [ary (make-array Long/TYPE 3 3)]
  (dotimes [i 3]
    (dotimes [j 3]
      (aset ary i j (+ i j))))
  (map seq ary))


(into-array Integer/TYPE [1 2 3]) ;; primitive array #object[I "x034c4ac04] ; I = primitive Integer array"



;;To intentionally create an array of a particular reference type, or of compatible types

(into-array ["a" "b" "c"])

(into-array ["a" "b" 1M]) ;;exception type mismatch


;;The function into-array determines the type of the resulting array based on the first element of the sequence, and each subsequent element type must be compatible (a subclass)


;;Array mutability:

(def ary  (into-array [1 2 3]))
ary ;; => [1, 2, 3]

(def sary (seq ary))


sary;; => (1 2 3)

(aset ary 0 42)

sary;; => (42 2 3)


;;The seq view of an array is that of the live array and therefore subject to concurrent modification. Be cautious when sharing arrays from one function to the next, and especially across threads


(defn asum-sq [xs]
  (let [db1 (amap xs i ret
                  (* (aget xs i)
                     (aget xs i)))]
    (areduce db1 i ret 0
             (+ ret (aget db1 i)))))


(asum-sq (double-array [1 2 3 4 5])) ;; => 55.0


(defmulti what-is class)

(defmethod what-is
  (Class/forName "[Ljava.lang.String;")
  [_]
  "1d string")

(defmethod what-is
  (Class/forName "[[Ljava.lang.Object;")
  [_]
  "2d object")

(defmethod what-is
  (Class/forName "[[[[I")
  [_]
  "Primitive 4d int")


(what-is (into-array ["a" "b"])) ;; => "1d string"


(what-is (to-array-2d [[1 2][3 4]]));; => "2d object"

(what-is (make-array Integer/TYPE 2 2 2 2));; => "Primitive 4d int"


;; 2d array

(defmethod what-is (Class/forName "[[D")
  [a]
  "Primitive 2d double")


(defmethod what-is (Class/forName "[Lclojure.lang.PersistentVector;")
  [a]
  "1d Persistent Vector")


(what-is (into-array (map double-array [[1.0] [2.0]]))) ;; => "Primitive 2d double"

(what-is (into-array [[1.0] [2.0]])) ;; => "1d Persistent Vector"

;;Variadic method/constructor calls


;;There’s no such thing as a variadic constructor or method at the bytecode level, although Java provides syntactic sugar at the language level.

;;Instead, variadic methods expect an array as their final argument, and this is how they should be accessed in Clojure interop scenarios.

(String/format "An int %d and a string %s"
               (to-array [99 "foobar"])) ;; => "An int 99 and a string foobar"



;;All Clojure functions implement:

(class #())





(ancestors (class #())) ;;NICE
;; => #{clojure.lang.AFunction clojure.lang.IFn java.io.Serializable
;;      java.lang.Object clojure.lang.Fn java.util.Comparator
;;      clojure.lang.IObj clojure.lang.IMeta java.lang.Runnable
;;      java.util.concurrent.Callable clojure.lang.AFn}


;;Most of the resulting classes are only applicable to the internals of Clojure, but a few interfaces are useful in interop scenarios: java.util .concurrent.Callable, java.util.Comparator, and java.lang.Runnable.


;;also

(ancestors (class []))
;; => #{clojure.lang.IPersistentCollection clojure.lang.IReduce
;;      clojure.lang.ILookup clojure.lang.IPersistentVector
;;      clojure.lang.IEditableCollection clojure.lang.IHashEq
;;      clojure.lang.IFn java.lang.Comparable java.lang.Iterable
;;      clojure.lang.IPersistentStack java.io.Serializable
;;      clojure.lang.IKVReduce java.lang.Object java.util.RandomAccess
;;      clojure.lang.Sequential clojure.lang.Seqable clojure.lang.IObj
;;      clojure.lang.IMeta clojure.lang.Indexed clojure.lang.Associative
;;      clojure.lang.Reversible java.lang.Runnable
;;      clojure.lang.APersistentVector clojure.lang.Counted
;;      java.util.concurrent.Callable java.util.List java.util.Collection
;;      clojure.lang.AFn clojure.lang.IReduceInit}


;;java.util.Compartor interface


;;The java.util.Comparator interface defines the signature for a single method .compare that takes two objects l and r and returns < 0 if l < r, 0 if l == r, and > 0 if l > r




(defn gimme [] (ArrayList. [1 3 4 8 2]))

(doto (gimme)
  (Collections/sort (Collections/reverseOrder)))
;; => [8 4 3 2 1]

(doto (gimme)
  (Collections/sort
   (reify Comparator
     (compare [this l r]
       (cond
         (> l r) -1
         (- l r) 0
         :else 1)))))
;; => [8 4 3 2 1]


;;Clojure provides a better way by allowing the use of potentially any function as the Comparator directly. You can couple this knowledge with the fact that Clojure already provides numerous functions useful for comparison


(doto (gimme)
  (Collections/sort >));; => [8 4 3 2 1]

(doto (gimme)
  (Collections/sort <)) ;; => [1 2 3 4 8]

(doto (gimme)
  (Collections/sort #(compare %2 %1)))

;; java.lang.Runnable interface


;;Java threads expect an object implementing the java.lang.Runnable interface, meant for computations returning no value.

;; every Clojure function implements the java.lang.Runnable interface


(doto (Thread. #(do (Thread/sleep 5000)
                    (println "haikeeba!")))
  .start)




;;;;The java.util.concurrent.Callable interface

;;The Java interface java.util.concurrent.Callable is specifically meant to be used in a threaded context for computations that return a value



(let [f (FutureTask. #(do (Thread/sleep 5000) 42))]
  (.start (Thread. #(.run f)))
  (.get f))
;; => 42


;; clojure data structure in java apis

;;Clojure sequential collections conform to the immutable parts of the java.util.List interface, which in turn extends the java.util.Collection and java.lang .Iterable interfaces


(.get '[a b c] 1);; => b ;;vectors

(.get (repeat :a) 138) ;;lazy-seq

(.containsAll '[a b c] '[b c]) ;;vectors are collections

(.add '[a b c] 'd) ;;exeception => seqs aren't mutable.



(java.util.Collections/sort [3 4 2 1]) ;;exception

;;;;;;;The java.lang.Comparable interface

;;The interface java.lang.Comparable is the cousin of the Comparator interface. Comparator refers to objects that can compare two other objects, whereas Comparable refers to an object that can compare itself to another object:

(.compareTo [:a] [:a]) ;; => 0

(.compareTo [:a :b] [:a]) ;; => 1


(.compareTo [:a :b] [:a :b :c]);; => -1


;; Clojure’s vector implementation is currently the only collection type that implements the java.lang.Comparable interface providing the .compareTo method


;;;;The java.util.RandomAccess interface

;;the java.util.RandomAccess interface is used to indicate that the data type provides constant-time indexed access to its elements

(.get '[a b c] 2) ;; => c


;;;;;;The java.util.Collection interface


;; The java.util.Collection interface lies at the heart of the Java Collections Framework, and classes implementing it can play in many of Java’s core collections APIs. A useful idiom that takes advantage of this fact is the use of a Clojure sequence as a model to build a mutable sequence for use in the Java Collections API

(defn shuffle [coll]
  (seq (doto (java.util.ArrayList. coll)
         Collections/shuffle)))


(shuffle (range 10)) ;; => (6 1 9 0 4 3 5 8 7 2)

;;Java.Util.Map interface

;;Like most of the Clojure collections, its maps are analogous to Java maps in that they can be used in nonmutating contexts. But immutable maps have the added advantage of never requiring defensive copies, and they act exactly the same as unmodifiable Java maps


(Collections/unmodifiableMap
 (doto (java.util.HashMap.)
   (.put :a 1))) ;; => {:a 1}



(into {} (doto (java.util.HashMap.)
           (.put :a 1))) ;; => {:a 1}



;;;;;;;;;; java.util.Set interface


(def x (java.awt.Point. 0 0))
(def y (java.awt.Point. 0 42))
(def points #{x y})

points
;; => #{#object[java.awt.Point 0x6b17eae4 "java.awt.Point[x=0,y=0]"]
;;      #object[java.awt.Point 0x74bb44b1 "java.awt.Point[x=0,y=42]"]}

;;mutating

(.setLocation y 0 0)

points
;; => #{#object[java.awt.Point 0x6b17eae4 "java.awt.Point[x=0,y=0]"]
;;      #object[java.awt.Point 0x74bb44b1 "java.awt.Point[x=0,y=0]"]}



;;;;;;;;;;;;definterface macro


;;Types and protocols help to provide a foundation for defining your own abstractions in Clojure, for use in a Clojure context. But when you’re interoperating with Java code, protocols and types won’t always suffice. Therefore, you need to be able to generate interfaces in some interop scenarios, and also for performance in cases involving primitive argument and return types




(definterface ISliceable
  (slice [^long s ^long e])
  (^long sliceCount [])) ;; => ch12.ISliceable


;;create an instance implementing Isliceable

(def dumb
  (reify ch12.ISliceable
    (slice [_ s e]
      [:empty])
    (sliceCount [_] 42)))

(.sliceCount dumb) ;; => 42

(.slice dumb 1 2) ;; => [:empty]


;;definterface works even without AOT compilation.


;;xtend the ISliceable interface to other types using a protocol

(defprotocol Sliceable
  (slice [this s e])
  (sliceCount [this]))

(extend ch12.ISliceable
  Sliceable
  {:slice (fn [this s e] (.slice this s e))
   :sliceCount (fn [this] (.sliceCount this))})


(sliceCount dumb) ;; => 42

(slice dumb 0 0) ;; => [:empty]


;; Extending strings along the Sliceable protocol

(defn calc-slice-count [thing]
  "Calculates the number of possible slices using the formula:
     (n + r - 1)!
     ------------
      r!(n - 1)!
   where n is (count thing) and r is 2"
  (let [! #(reduce * (take % (iterate inc 1)))
        n (count thing)]
    (/ (! (- (+ n 2)  1))
       (* (! 2) (! (- n 1))))))


(extend-type String
  Sliceable
  (slice [this s e] (.substring this s (inc e)))
  (sliceCount [this] (calc-slice-count this)))

(slice "abc" 0 1)

(sliceCount "abc")


;;The advantages of using definterface over defprotocol are restricted entirely to the fact that the former allows primitive types for arguments and returns.




;;;;;Exceptions:

;; When writing Clojure code, use errors to mean can’t continue and exceptions to mean can or might continue.

;;checked exceptions are antithetical to closures and higher-order functions. Checked exceptions require not only that the thrower and the party responsible for handling them should declare interest, but also that every intermediary is forced to participate.


;;These intermediaries don’t have to actively throw or handle exceptions occurring within, but they must declare that they’ll be “passing through.” Therefore, by including the call to a Java method throwing a checked exception within a closure, Clojure has two possible alternatives:

;;1)Provide a cumbersome exception-declaration mechanism on every single function, including closures.
;;2)By default, declare that all functions throw the root Exception or Runtime-Exception.

;;Clojure takes the second approach, which leads to a condition of multilevel wrapping of exceptions as they pass back up the call stack. This is why, in almost any (.printStackTrace *e) invocation, an error’s point of origin is offset by some number of layers of java.lang.RuntimeException

;; runtime vs compile-time exceptions


;; runtime:

;;two types of runtime exception: errors & exceptions.

(defn explode [] (explode))

(try (explode)
     (catch Exception e "stack blown"));;java.lang.StackOverflowError


;;Why can’t you catch the java.lang.StackOverflowError? The reason lies in Java’s exception class hierarchy and the fact that StackOverflowError isn’t a derivative of the Exception class, but instead of the Error class


(try (explode)
     (catch StackOverflowError e "stack blown")) ;; => "stack blown"


(try (explode) (catch Error e "Stack is blown")) ;; => "Stack is blown"


(try (explode) (catch Throwable e "Stack is blown")) ;; => "Stack is blown"

(try (throw (RuntimeException.))
     (catch Throwable e "Catching Throwable is Bad")) ;; => "Catching Throwable is Bad"


;;In Java, catching exceptions at the level of Throwable is considered bad form, and it should generally be viewed the same way in Clojure.


;; compile-time exceptions


(defmacro do-something [x] `(~x))

(do-something 1) ;;java.lang.ClassCastException


;;(for [e (.getStackTrace *e)] (.getClassName e) )
;; => ("ch12$eval8467"
;; "ch12$eval8467"
;; "clojure.lang.Compiler"
;; "clojure.lang.Compiler"
;; "clojure.core$eval"
;; "clojure.core$eval"
;; "clojure.main$repl$read_eval_print__9068$fn__9071"
;; "clojure.main$repl$read_eval_print__9068"
;; "clojure.main$repl$fn__9077"
;; "clojure.main$repl"
;; "clojure.main$repl"
;; "clojure.lang.RestFn"
;; "nrepl.middleware.interruptible_eval$evaluate"
;; "nrepl.middleware.interruptible_eval$evaluate"
;; "nrepl.middleware.interruptible_eval$interruptible_eval$fn__9629$fn__9633"
;; "clojure.lang.AFn"
;; "nrepl.middleware.session$session_exec$main_loop__9710$fn__9714"
;; "nrepl.middleware.session$session_exec$main_loop__9710"
;; "clojure.lang.AFn"
;; "java.lang.Thread")

;;The way to throw a compile-time exception is to make sure your throw doesn’t occur in a syntax-quoted form


(defmacro pairs [& args]
  (if (even? (count args))
    `(partition 2 '~args)
    (throw (Exception.
            (str "pairs requires an even number of args")))))


(pairs 1 2 3) ;;java.lang.Exception: pairs requires an even number of args

(pairs 1 2 3 4) ;; => ((1 2) (3 4))


(fn [] (pairs 1 2 3)) ;; ;;java.lang.Exception: pairs requires an even number of args

;;A runtime exception wouldn’t have been thrown until this function was called, but because the pairs macro threw an exception at compile time, users are notified of their error immediately


;; handling exceptons:


;; nullsafe arrow:

(defmacro -?> [& forms]
  `(try (-> ~@forms)
        (catch NullPointerException _# nil)))


(-?> 25
     (Math/sqrt)
     (+ 100)) ;; => 105.0


(-?> 25
     Math/sqrt
     (and nil)
     (+ 100)) ;; => nil


;; custom exceptions

;;The idiom is for Clojure to throw derivatives of RuntimeException or Error, and thus your code should also strive for this when appropriate.

;;functions ex-data and ex-info, used to attach information to load-bearing runtime exceptions

;;Clojure provides a new exception type ExceptionInfo that holds both a message string and a map. The function ex-info is a convenience function to construct instances of the ExceptionInfo type for throwing.

(defn perform-unclean-act [x y]
  (/ x y))


(try
  (perform-unclean-act 42 0)
  (catch RuntimeException ex
    (println (str "something went wrong"))))


(defn perform-cleaner-act [x y]
  (try
    (/ x y)
    (catch ArithmeticException ex
      (throw (ex-info "You attempted an unclean act"
                      {:args [x y]})))))

(try
  (perform-cleaner-act 10 0)
  (catch RuntimeException ex
    (println (str "received error " (.getMessage ex)))
    (when-let [ctx (ex-data ex)]
      (println "more info" ctx))))


;;The beauty of ex-data is that it can be called on any exception type, but it returns a map only if presented with a Clojure load-bearing kind.
