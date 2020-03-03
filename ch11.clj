(ns ch11
  (:require [clojure.zip :as zip]
            [clojure.xml :as xml]
            [ch10 :refer [dothreads!]])
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




