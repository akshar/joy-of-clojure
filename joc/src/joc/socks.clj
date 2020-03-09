(ns joc.socks
  (:import [joc DynaFrame]
           [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton JOptionPane]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]))


(defn shelf [& components]
  (let [shelf (JPanel.)]
    (.setLayout shelf (FlowLayout.))
    (doseq [c components]
      (.add shelf c))
    shelf))

(defn stacks [& components]
  (let [stack (Box. BoxLayout/PAGE_AXIS)]
    (doseq [c components]
      (.setAlignmentX c Component/CENTER_ALIGNMENT)
      (.add stack c))
    stack))

(defn splitter [top bottom]
  (doto (JSplitPane.)
    (.setOrientation JSplitPane/VERTICAL_SPLIT)
    (.setLeftComponent top)
    (.setRightComponent bottom)))

(defn button [text f]
  (doto (JButton. text)
    (.addActionListener
     (proxy [ActionListener] []  ;;addactionlistener takes an class instance which implements ActionListener and methodactionPerformed
       (actionPerformed [_] (f))))))

(defn txt [cols t]
  (doto (JTextField.)
    (.setColumns cols)
    (.setText t)))

(defn label [txt] (JLabel. txt))

(defn alert
  ([msg] (alert nil msg))
  ([frame msg]
   (javax.swing.JOptionPane/showMessageDialog frame msg)))





