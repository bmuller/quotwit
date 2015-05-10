(ns quotwit.core
  (:use clojure.string)
  (:import (java.awt Color Font Graphics2D RenderingHints)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File)))

(defn make-graphics [image]
  (let [rh (RenderingHints. RenderingHints/KEY_TEXT_ANTIALIASING, RenderingHints/VALUE_TEXT_ANTIALIAS_GASP)]
    (doto
      (.createGraphics image)
      (.setRenderingHints rh)
      (.setColor Color/BLACK)
      (.fillRect 0 0 (.getWidth image) (.getHeight image))
      (.setColor (Color. 220 220 220))
      (.setFont (Font. "Times" Font/PLAIN 55)))))

(defn make-image [width height]
  (BufferedImage. width, height, BufferedImage/TYPE_INT_RGB))

(defn save [image fname]
  (ImageIO/write image "jpg" (File. fname)))


(defn line-split-first [words pred]
  (split-at
   (loop [index 1]
    (cond
     (empty? words) index
     (= index (count words)) index
     (pred (take (inc index) words)) (recur (inc index))
     :else index)) words))

(defn line-split [words pred]
  "This will return a lazy sequence of sequences of words for which pred is true."
  (let [[a b] (line-split-first words pred)]
    (cons a (lazy-seq (line-split b pred)))))

(defn wrap-lines [sentence pred]
  (let [words (split sentence #"\s+")]
    (map (partial join " ")
     (take-while not-empty (line-split words pred)))))

(defn write-quote [graphics quote width height]
  (let [padding 50
        fm (.getFontMetrics graphics)
        lineheight (+ (.getHeight fm) 10) ; includes padding
        splitfn #(< (.stringWidth fm (join " " %)) (- width (* padding 2)))
        lines (wrap-lines quote splitfn)
        yoffset (int (/ (- height (* (count lines) lineheight)) 2))]
    (println yoffset)
    (doseq [index (range (count lines))]
      (.drawString graphics (nth lines index) padding (+ (* index lineheight) yoffset)))))

(defn write-name-source [graphics qname source width height]
  (let [fm (.getFontMetrics graphics)]
    (.setFont graphics (Font. "Helvetica" Font/BOLD 25))
    (.drawString graphics (.toUpperCase qname) 50 (int (- height (* 1.5 (.getHeight fm)))))
    (.setFont graphics (Font. "Helvetica" Font/PLAIN 23))
    (.drawString graphics source 50 (- height (.getHeight fm)))))

(defn run [quote qname source fname]
  (let [image (make-image 999 599)
        width (.getWidth image)
        height (.getHeight image)
        graphics (make-graphics image)]
    (write-quote graphics quote width height)
    (write-name-source graphics qname source width height)
    (save image fname)))

(def quotation "I wish I could say that fathers and mothers worry in equal measure. But they don't. Disregard what your two-career couple friends say about going 50-50.")
(run quotation "Judith Shulevitz" "Sunday Review: \"Mom: The Designated Worrier\"" "output.jpg")
