(ns ch1)

(doseq [x [:a :b] y (range 5) :when (odd? y)]
  (prn x y))
