(ns ch10
  (:import [java.util.concurrent Executors]))

;; Time —The relative moments when events occur
;; Identity
;; state: value of identity at given point in time.

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
   [:- :K :-]])

(defn board-map [f board]
  (vec
   (map #(vec (for [s %] (f s)))
        board)))

(board-map ref initial-board)
;; => [[#object[clojure.lang.Ref 0x4bfdfe9 {:status :ready, :val :-}] #object[clojure.lang.Ref 0x1be34b02 {:status :ready, :val :k}] #object[clojure.lang.Ref 0x7cb54fdc {:status :ready, :val :-}]] [#object[clojure.lang.Ref 0x4a2c7adf {:status :ready, :val :-}] #object[clojure.lang.Ref 0x3096a4e4 {:status :ready, :val :-}] #object[clojure.lang.Ref 0x312c52af {:status :ready, :val :-}]] [#object[clojure.lang.Ref 0x67b120f9 {:status :ready, :val :-}] #object[clojure.lang.Ref 0x4e624452 {:status :ready, :val :k}] #object[clojure.lang.Ref 0x4bd668fe {:status :ready, :val :-}]]]


(defn reset-board!
  []
  (def board (board-map ref initial-board))
  (def to-move (ref [[:K [2 1]] [:k [0 1]]]))
  (def num-moves (ref 0)))



(def king-moves
  (partial neighbors
           [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]] 3))

(defn good-move?
  [to enemy-sq]
  (when (not= to enemy-sq)
    to))

(defn choose-move
  [[mover mpos] [_ enemy-pos]]
  (mover (some #(good-move? % enemy-pos)
               (shuffle (king-moves mpos)))))
(reset-board!)

(take 5 (repeatedly #(choose-move @to-move)))


(defn place [from to] to)

(defn move-piece [[piece dest] [_ src] _]
  (alter (get-in board dest) place piece)
  (alter (get-in board src) place :-)

  )

;; SKIPPING REFS because the example is too difficult to understand and follow along

;;;;; Agents:

;; Agents manage asynchronus changes to shared state.
;; Each agent has a queue to hold actions that need to be performed on its value, and each action produces a new value for the agent to hold and pass to the subsequent action

;;Agents are integrated with STM transactions, and within a transaction any actions sent are held until the transaction commits or are thrown away if the transaction retries.


(def joy (agent []))

(send joy conj "foo")

@joy ;; => ["foo"]



(defn slow-conj [coll item]
  (Thread/sleep 1000)
  (conj coll item))


(send joy slow-conj "bar")


@joy ;; => ["foo" "bar"]


;;Erlang takes a distributed, share-nothing approach; Clojure instead promotes shared, immutable data.

;;The key to Clojure’s success is the fact that its composite data structures are immutable, because immutable structures can be freely shared among disparate threads

;; io with agent

(def log-agent (agent 0))

(defn do-log [msg-id message]
  (println msg-id ":" message)
  (Thread/sleep 5000)
  (inc msg-id))


(defn do-step [channel message]
  ;;  (Thread/sleep 3000)
  (send-off log-agent do-log (str channel message)))

(defn three-step [channel]
  (do-step channel " read to begin (step 0)")
  (do-step channel " warming up  (step 1)")
  (do-step channel " really get going now (step 2)")
  (do-step channel " done! (step 3)"))

(defn all-together-now []
  (dothreads! #(three-step "alpha"))
  (dothreads! #(three-step "beta"))
  (dothreads! #(three-step "omega")))

(all-together-now)

(do-step "important: " "this must go out")

(await log-agent)

(send log-agent (fn [_] 1000))

(do-step "epsilon " "near miss")


;;When you use send-off, as in most of the examples so far, only a single action queue is involved: the one managed by the individual agent. Any time the agent has a send-off action queued, it has a thread assigned to it, working through the queue. With send, there’s a second queue: actions still go into the agent’s queue, but then the agent itself queues up waiting for a thread from a fixed-sized pool of threads. The size of this fixed pool is based on the number of processors the JVM is running on, so it’s a bad idea to use send with any actions that might block, tying up one of the limited number of threads.


(defn exercise-agents [send-fn]
  (let [agents (map #(agent %) (range 20))]
    (doseq [a agents]
      (send-fn a (fn [_] (Thread/sleep 1000))))
    (doseq [a agents]
      (await a))))


;; 16 processors

(time (exercise-agents send-off)) ;; "Elapsed time: 1009.479827 msecs"



(time (exercise-agents send));; "Elapsed time: 2009.0396 msecs"



;;So that’s it: send is for actions that stay busy using the processor and not blocking on I/O or other threads, whereas send-off is for actions that might block, sleep, or otherwise tie up the thread


;; Error handling agents

;;two error handling modes: :continue & :fail


;;One common mistake when dealing with agents is to forget that your action function must take at least one argument for the agent’s current state. For example, you might try to reset the log-agent’s current message ID like this:


(send log-agent (fn [] 2000))

@log-agent ;; => 0

(agent-error log-agent);; => #error {
;;     :cause "Wrong number of args (1) passed to: ch10/eval7547/fn--7548"
;;     :via
;;     [{:type clojure.lang.ArityException
;;       :message "Wrong number of args (1) passed to: ch10/eval7547/fn--7548"
;;       :at [clojure.lang.AFn throwArity "AFn.java" 429]}]
;;     :trace
;;     [[clojure.lang.AFn throwArity "AFn.java" 429]
;;      [clojure.lang.AFn invoke "AFn.java" 32]
;;      [clojure.core$binding_conveyor_fn$fn__5739 invoke "core.clj" 2033]
;;      [clojure.lang.AFn applyToHelper "AFn.java" 154]
;;      [clojure.lang.RestFn applyTo "RestFn.java" 132]
;;      [clojure.lang.Agent$Action doRun "Agent.java" 114]
;;      [clojure.lang.Agent$Action run "Agent.java" 163]
;;      [java.util.concurrent.ThreadPoolExecutor runWorker "ThreadPoolExecutor.java" 1149]
;;      [java.util.concurrent.ThreadPoolExecutor$Worker run "ThreadPoolExecutor.java" 624]
;;      [java.lang.Thread run "Thread.java" 748]]}


;;Another way to see whether an agent is stopped is to try to send another action to it:


(send log-agent (fn [_] 3000))

;;; java.lang.RuntimeException: Agent is failed, needs restart

;; restart agent

(restart-agent log-agent 2500 :clear-actions true)

@log-agent ;; => 2500


;;if you specify an :error-handler when you create an agent, that agent’s error mode defaults to :continue. The agent calls the error handler when an action throws an exception and doesn’t proceed to the next action until the handler         returns.

(defn handle-log-error [the-agent the-err]
  (println "An action sent to the log-agent threw " the-err))



(set-error-handler! log-agent handle-log-error)

(set-error-mode! log-agent :continue)


(send log-agent (fn [x] (/ x 0)))   ; incorrect
;; => #<Agent@9be2104: 2500>;; prints exceptionq


;; when not to use agents:

;; But although agents perform beautifully when each one is representing a real identity in your application, they start to show weaknesses when used as a sort of “green thread” abstraction.
