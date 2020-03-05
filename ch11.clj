(ns ch11
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [ch10 :refer [dothreads!]]
            [clojure.core.reducers :as r])
  (:import [java.util.regex Pattern]))


;;concurrency was about the design of a system, parallelism is about the execution model

;;Clojure includes two reference types supporting parallelism: futures and promises.


;; Futures:

(time (let [x (future
                (do
                  (Thread/sleep 5000) (+ 41 1)))]
        [@x @x]));;"Elapsed time: 5005.118675 msecs"

;;The processing time of the do block is only experienced on the first dereference of the future x. Futures represent expressions that have yet to be computed.


;; futures as callbacks:

;;https://www.youtube.com/watch?v=5Nm56YvTKZY

(-> [[1 2] [3 4 5] 6]
    (zip/vector-zip) ;; ;; => [[[1 2] [3 4 5] 6] nil]
    (zip/down);;;; => [[1 2] {:l [], :pnodes [[[1 2] [3 4 5] 6]], :ppath nil, :r ([3 4 5] 6)}]
    (zip/right));; => [[1 2] {:l [], :pnodes [[[1 2] [3 4 5] 6]], :ppath nil, :r ([3 4 5] 6)}]





(defn- title [entry]
  (some->> entry
           :content
           (some #(when (= :title (:tag %)) %))
           :content
           first))


(defn- feed->zipper [uri-str]
  (->> (xml/parse uri-str)
       zip/xml-zip))

(defn- normalize [feed]
  (if (= :feed (:tag (first feed)))
    feed
    (zip/down feed)))

(defn- feed-children [uri-str]
  (->> uri-str
       feed->zipper
       normalize
       zip/children
       (filter (comp #{:item :entry} :tag))))

(defn- count-text-task [extractor txt feed]
  (let [items (feed-children feed)
        re (Pattern/compile (str "(?i)" txt))]
    (->> items
         (map extractor)
         (mapcat #(re-seq re %))
         count)))

(count-text-task title "things" "http://blog.fogus.me/feed")



(def feeds  #{"https://feeds.feedburner.com/ElixirLang"
              "http://blog.fogus.me/feed/"})

(let [results (for [feed feeds]
                (future
                  (count-text-task title "things" feed)))] ;; building seq of futures
  (reduce + (map deref results)))

;;or build as-futures macro

;;NOT REQUIRED
(defmacro as-futures [[a args] & body]
  (let [parts (partition-by #{'=>} body)
        _ (prn parts)
        [acts _ [res]] (partition-by #{:as} (first parts))
        [_ _ task] parts]
    `(let [~res (for [~a ~args] (future ~@acts))]
       ~@task)))



(defn occurrences [extractor tag & feeds]
  (as-futures [feed feeds]
              (count-text-task extractor tag feed)
              :as results
              =>
              (reduce + (map deref results))))

(occurrences title "released"
             "http://blog.fogus.me/feed/"
             "http://feeds.feedburner.com/ElixirLang"
             "http://www.ruby-lang.org/en/feeds/news.rss")

;; PROMISES


;;Promises are similar to futures, in that they represent a unit of computation to be performed on a separate thread. Likewise, the blocking semantics when dereferencing an unfinished promise are the same


(def x (promise))

(def y (promise))

(def z (promise))

(dothreads!
 #(deliver z (+ @x @y)))

(dothreads!
 #(do (Thread/sleep 4000) (deliver x 52)))

(dothreads!
 #(do (Thread/sleep 4000) (deliver y 86)))

;;(time @z)  ;; running this will block because values of x & y are not delivered.


;;Each promise must be delivered once and only once for a dereference to return a value. Promises are write-once; any further attempt to deliver will throw an exception.


(defmacro with-promises [[n tasks _ as] & body]
  (when as
    `(let [tasks# ~tasks
           n# (count tasks#)
           promises# (take n# (repeatedly promise))]
       (dotimes [i# n#]
         (dothreads!
          (fn []
            (deliver (nth promises# i#)
                     ((nth tasks# i#))))))
       (let [~as promises#]
         ~@body))))


(defrecord TestRun [run passed failed])

(defn pass [] true)
(defn fail [] false)

(defn run-tests [& all-tests]
  (with-promises
    [tests all-tests :as results]
    (into (TestRun. 0 0 0)
          (reduce #(merge-with + %1 %2)
                  {}
                  (for [r results]
                    (if @r
                      {:run 1 :passed 1}
                      {:run 1 :failed 1}))))))


(run-tests pass fail fail fail pass) ;; => #ch11.TestRun{:run 5, :passed 2, :failed 3}




;; promises + callback


(defn feed-items [k feed] ;; k=callback
  (k
   (for [item (filter (comp #{:entry :item} :tag)
                      (feed-children feed))]
     (-> item :content first :content))))

(feed-items count "http://blog.fogus.me/feed/") 


(let [p (promise)]
  (feed-items #(deliver p (count %))
              "http://blog.fogus.me/feed/")
  @p) ;; blocking


;; Transforming a callback-based function to a blocking call

(defn cps->fn [f k] ;;k= callback
  (fn [& args]
    (let [p (promise)]
      (apply f (fn [x] (deliver p (k x))) args)
      @p)))

(def count-items (cps->fn feed-items count))

(count-items "http://blog.fogus.me/feed/") ;=> 5

;; Deterministic deadlock

(def kant (promise))
(def hume (promise))

;;
;; (dothreads!
;;  #(do (println "Kant has" @kant) (deliver hume :thinking)))

;; (dothreads!
;;  #(do (println "Hume is" @hume) (deliver kant :fork)))


;;derefing any promise will cause deadlock. THIS DEADLOCK WILL HAPPEN EVERYTIME

;;it’s deterministic rather than dependent on odd thread timings or the like. Deadlocks are never nice, but deterministic deadlocks are better than nondeterministic.

;; Parallel operations:

;;pvalues = as-futures

(pvalues 1 2 (+ 1 2)) ;; in that it executes an arbitrary number of expressions in parallel.

;;The important point to remember when using pvalues is that the return type is a lazy sequence, meaning your access costs might not always present themselves as expected:

(defn sleeper [s thing] (Thread/sleep (* 1000 s)) thing)

(defn pvs []
  (pvalues
   (sleeper 2 :1st)
   (sleeper 3 :2nd)
   (keyword "3rd")))

(-> (pvs)
    first
    time);; => :1st "Elapsed time: 2002.774899 msecs"

;;The total time cost of accessing the first value in the result of pvs is only the cost of its own calculation. But accessing any subsequent element costs as much as the most expensive element before it, which you can verify by accessing the last element:

(-> (pvs)
    last
    time);; => :3rd"Elapsed time: 3002.39365 msecs"

;;pmap parallel version of map fn

(->> [1 2 3]
     (pmap (comp inc (partial sleeper 2)))
     doall
     time) ;; => (2 3 4) "Elapsed time: 2004.867348 msecs"


;;the application of the function to each matching element happens in parallel

;;The total cost of realizing the result of mapping a costly increment function is again limited by the most costly execution time in the aforementioned sliding window

;; replace map with  pmap everywhere ?

;; A definite cost is associated with keeping the resulting sequence result coordinated, and to indiscriminately use pmap might incur that cost unnecessarily, leading to a performance penalty.

;;pcalls:

;;pcalls function that takes an arbitrary number of functions taking no arguments and calls them in parallel, returning a lazy sequence of the results.


(-> (pcalls
     #(sleeper 2 :first)
     #(sleeper 3 :second)
     #(keyword "3rd"))
    doall
    time) 
;; => (:first :second :3rd) "Elapsed time: 3004.76381 msecs"


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; There’s currently no magical formula for determining which parts of an application can be parallelized—the onus is on you to determine your application’s parallel potential. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; Reducer/Fold.

;; weakness of data structures that only support sequential access, like Lisp’s cons-cell-based lists and Clojure’s lazy seq: they’re incapable of supporting efficient parallelization because you can’t get more input data without         walking linearly through the data to get there. Similarly, he called out foldl (called reduce in Clojure) and foldr (which has no native Clojure counterpart) as providing ordering guarantees that thwart parallelization.


;; Specifically, parallelizing a workload generally involves splitting the work into small parts, doing work on all those parts simultaneously, and then combining the results of those parts to compute the single final result. But reduce can’t work this way because it promises to call the given reducing function on each input item in order, with the first parameter representing the reduction of everything that has come before it.


;; But many times when reduce is used, these guarantees aren’t required by the reducing function being used. For example, addition is associative, which means if you want the sum of three numbers, you’ll get the same answer whether you compute it as (+ (+ a b) c) or as (+ a (+ b c)).

;;ENTER FOLD

(def big-vec (vec (range (* 1000 1000))))

(time (reduce + big-vec)) ;; => 499999500000
;;"Elapsed time: 24.5343 msecs"

(time (r/fold + big-vec))
;;"Elapsed time: 20.956897 msecs"
