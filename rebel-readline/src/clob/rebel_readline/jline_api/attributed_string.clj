(ns clob.rebel-readline.jline-api.attributed-string
  (:refer-clojure :exclude [partition-all])
  (:import
   [org.jline.utils AttributedStringBuilder AttributedString AttributedStyle]))

(defn ^AttributedString astr
  [& args]
  (AttributedString.
   (reduce #(if %2
              (if (vector? %2)
                (.styled ^AttributedStringBuilder %1 ^AttributedStyle (second %2) ^String (first %2))
                (.append ^AttributedStringBuilder %1 ^AttributedString %2))
              %1)
           (AttributedStringBuilder.) args)))

(defn split [^AttributedString at-str regex]
  (let [s (str at-str)
        m (.matcher #"\n" s)]
    (->> (repeatedly #(when (.find m)
                        [(.start m) (.end m)]))
         (take-while some?)
         flatten
         (cons 0)
         (clojure.core/partition-all 2)
         (mapv
          #(.substring at-str (first %) (or (second %) (count s)))))))

(defn partition-all [length ^AttributedString at-str]
  (mapv first
        (take-while some?
                    (rest
                     (iterate (fn [[_ ^String at-str]]
                                (when at-str
                                  (if (<= (count at-str) length)
                                    [at-str nil]
                                    [(.substring at-str 0 (min length (count at-str)))
                                     (.substring at-str length (count at-str))])))
                              [nil at-str])))))

(defn split-lines [^AttributedString at-str]
  (split at-str #"\r?\n"))

(defn join [sep coll]
  (apply astr (interpose sep coll)))

(defn ->ansi [^AttributedString at-str terminal]
  (.toAnsi (.toAttributedString at-str) terminal))

(defn ->ansi-256 [^AttributedString at-str]
  (.toAnsi (.toAttributedString at-str) 256 true))
