(ns ch10
  (:import [java.util.concurrent Executors]))

;; co-ordinated: Read/Write to multiple refs in such a way that guarantees no race conditons.
;; Asynchronus: Request to update happens is queued to happen in another thread some time later.
;; Thread local: thread safety is achieved by isolating changes to state to a single thread.
;; Retriable: work done to update a ref value is speculative and may have to be repeated.


(def thread-pool
  (Executors/newFixedThreadPool
   (+ 2 (.availableProcessors (Runtime/getRuntime)))))

(defn dothreads! [f & {thread-count :threads
                       exec-count :times
                       :or {thread-count 1 exec-count 1}}]
  (dotimes [t thread-count]
    (.submit thread-pool
             #(dotimes [_ exec-count] (f)))))

(dothreads! #(.print System/out "Hi ") :threads 2 :times 2)


;;;;;;;;;;;;;;;;;;;;;;; refs:

;;A ref is a reference type allowing synchronous, coordinated change to its contained value

(defn neighbors
  ([size yx] (neighbors [[-1 0] [1 0] [0 -1] [0 1]]
                        size
                        yx))

  ([deltas size yx]
   (filter (fn [new-yx]
             (every? #(< -1 % size) new-yx))
           (map #(vec (map + yx %))
                deltas))))

(def initial-board
  [[:- :k :-]
   [:- :- :-]
   [:- :k :-]])

(defn board-map [f board]
  (vec
   (map #(vec (for [s %] (f s)))
        board)))

(board-map ref initial-board)
;; => [[#object[clojure.lang.Ref 0x4bfdfe9 {:status :ready, :val :-}] #object[clojure.lang.Ref 0x1be34b02 {:status :ready, :val :k}] #object[clojure.lang.Ref 0x7cb54fdc {:status :ready, :val :-}]] [#object[clojure.lang.Ref 0x4a2c7adf {:status :ready, :val :-}] #object[clojure.lang.Ref 0x3096a4e4 {:status :ready, :val :-}] #object[clojure.lang.Ref 0x312c52af {:status :ready, :val :-}]] [#object[clojure.lang.Ref 0x67b120f9 {:status :ready, :val :-}] #object[clojure.lang.Ref 0x4e624452 {:status :ready, :val :k}] #object[clojure.lang.Ref 0x4bd668fe {:status :ready, :val :-}]]]


(defn reset-board!
  []
  (def board (board-map ref initial-board))
  (def to-move (ref [[:k [2 1]] [:k [0 1]]]))
  (def num-moves (ref 0)))



(def king-moves
  (partial neighbors
           [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]] 3
           ))




