(ns quotwit.cards
  (:use [clojure.string :only [split join]])
  (:import (java.awt Color Font Graphics2D RenderingHints)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File ByteArrayInputStream ByteArrayOutputStream FileInputStream)
           (javax.imageio ImageIO)))

(defn make-graphics [image]
  (let [rh (RenderingHints. RenderingHints/KEY_TEXT_ANTIALIASING, RenderingHints/VALUE_TEXT_ANTIALIAS_GASP)]
    (doto
      (.createGraphics image)
      (.setRenderingHints rh)
      (.setColor Color/BLACK)
      (.fillRect 0 0 (.getWidth image) (.getHeight image))
      (.setColor (Color. 220 220 220))
      ;(.setFont (Font/createFont Font/TRUETYPE_FONT (FileInputStream. "/tmp/Tinos-Regular.ttf"))))))
      (.setFont (Font. "Times" Font/PLAIN 55)))))

(defn make-image [width height]
  (BufferedImage. width, height, BufferedImage/TYPE_INT_RGB))

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
    (doseq [index (range (count lines))]
      (.drawString graphics (nth lines index) padding (+ (* index lineheight) yoffset)))))

(defn write-name-source [graphics qname source width height]
  (let [fm (.getFontMetrics graphics)]
    (doto graphics
      (.setFont (Font. "Helvetica" Font/BOLD 25))
      (.drawString (.toUpperCase qname) 50 (int (- height (* 1.5 (.getHeight fm)))))
      (.setFont (Font. "Helvetica" Font/PLAIN 23))
      (.drawString source 50 (- height (.getHeight fm))))))
  
(defn run [quote qname source]
  (let [image (make-image 999 599)
        width (.getWidth image)
        height (.getHeight image)
        graphics (make-graphics image)]
    (write-quote graphics quote width height)
    (write-name-source graphics qname source width height)
    image))

(defn get-image-bytes [quote qname source]
  (let [image (run quote qname source)]
    (with-open [bs (ByteArrayOutputStream.)]
      (ImageIO/write image "jpg" bs)
      (.flush bs)
      (ByteArrayInputStream. (.toByteArray bs)))))

(defn get-image-byte-array [quote qname source]
  (let [image (run quote qname source)]
    (with-open [bs (ByteArrayOutputStream.)]
      (ImageIO/write image "jpg" bs)
      (.flush bs)
      (.toByteArray bs))))
