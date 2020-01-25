(ns ch3)

;;In Clojure, everything is truthy unless it’s false or nil

;; NIL punning

;; empty collection is truthy. seq can be used as pun to determinie falsity of the collection

(seq []) ;; nil

(defn print-seq [s]
  (when (seq s)
    (prn (first s))
    (recur (rest s))))

(print-seq [1 2 3])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Destructuring:

;; by keys
(let [{:keys [a b]} {:a 1 :b 2}]
  (prn a b))

;; by string
(let [{:strs [a b]} {"a" 1 "b" 2}]
  (prn a b))

(let [{:keys [foo bar baz]
       :or {foo "foo"} }
      {:bar "bar" :baz "baz"}]
  (str foo bar baz))  ;; destructuring with default value

(defn whole-name [& args]
  (let [_ (prn args)
        {:keys [f-name m-name l-name]} args]
    (str l-name ", " f-name " " m-name)))

(whole-name :f-name "Guy" :m-name "Lewis" :l-name "Steele")

;; Associative destructuring :

(let [{first-thing 0, last-thing 3} [1 2 3 4]]
  [first-thing last-thing]) ;; using indices [1 4]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; REPL

;; Experimenting with seqs:

;; generate xor values of corordinates


(defn xors [x y]
  (for [x (range 2)
        y (range 2)]
    [x y (bit-xor x y)]))
;; => ([0 0 0] [0 1 1] [1 0 1] [1 1 0])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; experimenting with java graphics

(def frame (java.awt.Frame.))

frame ;;=> to display frame

;; frame is not visible

;; quickly search through java.awt.Frame methods

(for [meth (.getMethods java.awt.Frame)
      :let [name (.getName meth)]
      :when (re-find #"Vis" name)]
  name) ;; => ("setVisible" "isVisible")

(.isVisible frame)
(.setVisible frame true)

(.setSize frame (java.awt.Dimension. 200 200))

;; draw graphic context into your frame.

(def gfx (.getGraphics frame))

(.fillRect gfx 100 100 50 75)

;; finishing the task

(doseq [[x y xor]  (xors 200 200)]
  (.setColor gfx (java.awt.Color. xor xor xor))
  (.fillRect gfx x y 1 1))

;; (.printStackTrace *e) => print exception stacktrace



