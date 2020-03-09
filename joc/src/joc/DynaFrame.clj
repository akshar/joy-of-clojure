(ns joc.DynaFrame
  (:gen-class
   :name         joc.DynaFrame
   :prefix       "df-"
   :extends      javax.swing.JFrame
   :implements   [clojure.lang.IMeta]
   :state        state
   :init         init
   :constructors {[String] [String]}
   :methods      [[display [java.awt.Container] void]
                  ^{:static true} [version [] String]])
  (:import (javax.swing JFrame JPanel)
           (java.awt BorderLayout Container))
  (:require [joc.socks :as component]))

(defn df-init [title]
  [[title] (atom {::title title})])


(defn df-meta [this] @(.state this))


(defn version [] "1.0")

(meta (joc.DynaFrame. "3rd")) ;; => #:joc.DynaFrame{:title "3rd"}


(defn df-display [this pane]
  (doto this
    (-> .getContentPane .removeAll)
    (.setContentPane (doto (JPanel.)
                       (.add pane BorderLayout/CENTER)))
    (.pack)
    (.setVisible true)))


(def gui (joc.DynaFrame. "4th"))

(.display gui
          (component/splitter
           (component/button "Procrastinate" #(component/alert "Eat Cheetos"))
           (component/button "Move It" #(component/alert "Couch to 5k"))))


;;What exactly does the :gen-class directive provide in terms of generated class files?

;;The :gen-class directive creates a class that’s a delegate for the vars (prefixed as specified with df-) located in the corresponding namespace, contains the state, and also holds any static methods










