(ns ch14
  (:require [ch7 :refer [convert]]
            [clojure.edn :as edn]
            [clojure.set :as sql]))

;;A place oriented programming (Hickey 2012) model is one where data representing entity state is updated in place over the data that existed previously, thus obliterating any notion of time and seriously subverting object equality. As it stands, the PLOP model is prevalent in many popular programming languages. But functional languages in general, and Clojure in particular, eschew the widespread use of PLOPing and instead advocate a purer model where values take center stage



;;Many programming languages make a stark distinction between program data and the code that operates on it. That is, accessing raw data in a language like Java has an alien feel: the code is here, and the data is over there.

;;;;;;;;;;;;;;;;;;;;;;;;benefits of viewing any given datum strictly as a value


{:a 1, :b 2} ;; SIMPLE MAP

;; Map<String,Integer> stuff = new HashMap<String,Integer>() {{
;;     put("a", 1);
;;     put("b", 2);
;; }};  The creation of the data occurs as a result of executing the code


;; data fabricators
(def ascii (map char (range 65 (+ 65 26))))

(defn rand-str [sz alphabets]
  (apply str (repeatedly sz #(rand-nth alphabets))))

(rand-str 10 ascii)

(def rand-sym #(symbol (rand-str %1 %2)))

(def rand-key #(keyword (rand-str %1 %2)))


(rand-key 10 ascii) ;; => :ACQENIQNKT

(rand-sym 10 ascii) ;; => OHBRZYYPBY


;; build composite structure


(defn rand-vec [& generators]
  (into [] (map #(%) generators)))


(rand-vec #(rand-sym 5  ascii)
          #(rand-key 10 ascii)
          #(rand-int 1024)) ;; => [VSQHH :KZEVBVDFAB 199]


(defn rand-map  [sz kgen vgen]
  (into {}
        (repeatedly sz #(rand-vec kgen vgen))))

(rand-map 3 #(rand-key 5 ascii) #(rand-int 100))  ;; => {:CEZXW 57, :DWWHY 60, :UAZIU 87}


;;;;;;;;;;;;;;;;;Generative testing:



(defn filter-rising [segments]
  (clojure.set/select
   (fn [{:keys [p1 p2]}]
     (> 0
        (/ (- (p2 0) (p1 0))
           (- (p2 1) (p1 1)))))
   segments))

(filter-rising #{{:p1 [0 0]  :p2 [1 1]}
                 {:p1 [4 15] :p2 [3 21]}}) ;; => #{{:p1 [4 15], :p2 [3 21]}} wrong


;; can be easily debugged by feeding data points to filter-rising.

;;Values are language independent.


;;;;;;;Tagged literals

;;Clojure now includes numerous tagged literals, including universally unique identifiers (UUIDs) and a time-instance form that can be used as follows:

(java.util.Date.) ;; => #inst "2020-03-06T13:17:56.171-00:00"

;;instance literal starts with # followed by inst


;;JSON is a fixed format and doesn’t support extensibility of the base literal types as XML does. With the addition of the tagged-literal feature, Clojure also allows extension of its literal notation. An informal standardization effort is underway to formalize the use of Clojure data literals and its tagged extensions as Extensible Data Notation (EDN, pronounced “Eden”).


(def distance-reader
  (partial convert
           {:m  1
            :km 1000,
            :cm 1/100,
            :mm [1/10 :cm]}))


;;But before you can start using distance-reader, you need to tell Clojure to use the function whenever it encounters the #unit/length tag. To do that, you need only create a file called data_readers.clj at the root of your classpath (for example, in our-project-directory/src/) with a simple map of symbol to symbol, like so:




;;a new namespace clojure.edn that provides the safe EDN data-read functions clojure.edn/read and clojure.edn/read-string




(class #uuid "dae78a90-d491-11e2-8b8b-0800200c9a66")

(edn/read-string "#uuid \"dae78a90-d491-11e2-8b8b-0800200c9a66\"") ;; => #uuid "dae78a90-d491-11e2-8b8b-0800200c9a66"

;; reading uuid tagged literal.

(edn/read-string "42") ;; => 42

(edn/read-string "{:a 42, \"b\" 36, [:c] 9}");; => {:a 42, "b" 36, [:c] 9}

;;;;;;;;;;;;;;;;;;;Data programmable engines

;;;;;;;;;;;;;;;;;;;
;; specification ;;;;;;;;;>>>   Data-programmable Engine ;;;;;;;;;;;>>>   something awesome
;;;;;;;;;;;;;;;;;;;


;; Event sourcing

;;Event sourcing is an interesting architectural model that defines system state solely in terms of system events


;; events as data

;; Simple event-sourced model

;; snapshot of some state at-bats , hits and average
{:ab 6  
 :h  2
 :avg 0.400}

(defn valid? [event]
  (boolean (:result event)))

(valid? {}) ;; => false
(valid? {:result 42}) ;; => true

(defn effect [{:keys [ab h] :or {ab 0 h 0}} event]
  (let [ab (inc ab)
        h (if (= :hit (:result event))
            (inc h)
            h)
        avg (double (/ h ab))]
    {:ab ab :h h :avg avg}))

(defn apply-effect [state event]
  (if (valid? event)
    (effect state event)
    state))

(apply-effect {:ab 600 :h 180 :avg 0.3}
              {:result :hit}) ;; => {:ab 601, :h 181, :avg 0.3011647254575707}

(def effect-all #(reduce apply-effect %1 %2))

(effect-all {:ab 0 :h 0} [{:result :hit}
                          {:result :out}
                          {:result :hit}
                          {:result :out}]) ;; => {:ab 4, :h 2, :avg 0.5}

(def events (repeatedly 100
                        (fn []
                          (rand-map 1
                                    #(-> :result)
                                    #(if (< (rand-int 10) 3)
                                       :hit
                                       :out)))))


(effect-all {} events) ;; => {:ab 100, :h 23, :avg 0.23} ;;snapshot of state

;;Rewinding is as simple as applying only a subset of the events:

(effect-all {} (take 50 events)) ;; => {:ab 50, :h 16, :avg 0.32}



(def fx-timeline #(reductions apply-effect %1 %2))   ;; WOW

(fx-timeline {}  (take 3 events))
;; => ({}
;;     {:ab 1, :h 1, :avg 1.0}
;;     {:ab 2, :h 1, :avg 0.5}
;;     {:ab 3, :h 2, :avg 0.6666666666666667})

;;You could use fx-timeline to infer trends, build histograms, and many other useful actions.




(def PLAYERS #{{:player "Nick", :ability 32/100}
               {:player "Matt", :ability 26/100}
               {:player "Ryan", :ability 19/100}})

(defn lookup [db name]
  (first (sql/select
          #(= name (:player %))
          db)))

(lookup PLAYERS "Nick")


(defn update-stats [db event]
  (let [player (lookup db (:player event))
        less-db (sql/difference db #{player})]
    (conj less-db
          (merge player (effect player event)))))

(update-stats PLAYERS {:player "Nick" :result :hit})


;; => #{{:player "Matt", :ability 26} {:player "Ryan", :ability 19}
;;      {:player "Nick", :ability 32, :ab 1, :h 1, :avg 1.0}}

(defn commit-event [db event]
  (dosync (alter db update-stats event)))

(commit-event (ref PLAYERS) {:player "Nick", :result :hit})


(defn rand-event [{:keys [ability]}]
  (let [able (numerator ability)
        max (denominator ability)]
    (rand-map 1
              #(-> :result)
              #(if (< (rand-int max) able)
                 :hit
                 :out))))

(defn rand-events [total player]
  (take total
        (repeatedly #(assoc (rand-event player)
                            :player
                            (:player player)))))


(rand-events 3 {:player "Nick" :ability 32/100})
;; => ({:result :out, :player "Nick"}
;;     {:result :out, :player "Nick"}
;;     {:result :out, :player "Nick"})



(def agent-for-player 
  (memoize
   (fn [player-name]
     (let [a (agent [])]
       (set-error-handler! a #(println "Error :" %1 %2))
       a))))


(defn feed [db event]
  (let [a (agent-for-player (:player event))]
    (send a
          (fn [state]
            (commit-event db event)
            (conj state event)))))

(defn feed-all [db events]
  (doseq [event events]
    (feed db event))
  db)


(let [db (ref PLAYERS)]
  (feed-all db (rand-events 100 {:player "Nick" :ability 32/100}))
  db)


(count @(agent-for-player "Nick"))


(effect-all {} @(agent-for-player "Nick"))



(defn simulate [total players]
  (let [events (apply interleave
                      (for [player players]
                        (rand-events total player)))
        results (feed-all (ref players) events)]
    (apply await (map #(agent-for-player (:player %)) players))
    @results))  ;; :flushed:

(simulate 2 PLAYERS)
;; => #{{:player "Nick", :ability 8/25, :ab 2, :h 0, :avg 0.0}
;;      {:player "Matt", :ability 13/50, :ab 2, :h 1, :avg 0.5}
;;      {:player "Ryan", :ability 19/100, :ab 2, :h 0, :avg 0.0}}


(simulate 300 PLAYERS)
;; => #{{:player "Ryan",
;;       :ability 19/100,
;;       :ab 300,
;;       :h 52,
;;       :avg 0.1733333333333333}
;;      {:player "Matt", :ability 13/50, :ab 300, :h 84, :avg 0.28}
;;      {:player "Nick",
;;       :ability 8/25,
;;       :ab 300,
;;       :h 103,
;;       :avg 0.3433333333333333}}


;;By virtue of storing the events, you can easily make changes to live systems. That is, if a system’s state is the product of the event stream, then you should be able to re-create it at any time by re-running the events into a fresh system

;;;;;;;;;;;;CODE AS DATA AS CODE:


;;Clojure code is composed of data elements. This condition of Lisp is known as homoiconicity

(defn meters->feet [m] (* m 3.28083989501312))
(defn meters->miles [m] (* m 0.000621))

(meters->feet 1609.344)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (Our base unit of distance is the :meter          ;;
;;      [There are 1000 :meters in a :kilometer]     ;;
;;      [There are 100 :centimeters in a :meter]     ;;
;;      [There are 10 :millimeters in a :centimeter] ;;
;;      [There are 3.28083 :feet in a :meter]        ;;
;;      [There are 5280 :feet in a :mile])           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn relative-units [context unit]
  (if-let [spec (get context unit)]
    (if (vector? spec)
      (convert context spec)
      spec)
    (throw (RuntimeException. (str "Undefined unit " unit)))))


(defmacro defunits-of [name base-unit & conversions]
  (let [magnitude (gensym)
        unit (gensym)
        units-map (into `(~base-unit 1)
                        (map vec (partition 2 conversions)))]

    `(defmacro ~(symbol (str "unit-of-" name ))
       [~magnitude ~unit]
       `(* ~magnitude
           ~(case ~unit
              ~@(mapcat
                 (fn [[u# & r#]]
                   `[~u# ~(relative-units units-map u#)])
                 units-map))))))

(def x  (defunits-of distance :m
          :km 1000
          :cm 1/100
          :mm [1/10 :cm]
          :ft 0.3048
          :mile [5280 :ft]))



