(ns clob.test-util.util)

(defn with-tempfile [cb]
  (let [file (java.io.File/createTempFile "clob-test-" ".txt")
        f (.getAbsolutePath file)
        _ (.deleteOnExit file)]
    (cb f)))

(defn with-tempfile-content [cb]
  (with-tempfile
    (fn [f]
      (cb f)
      (slurp f))))

(def null-file
  (if (-> (System/getProperty "os.name")
          (.toLowerCase)
          (.indexOf "win")
          (pos?))
    "nul"
    "/dev/null"))

(defn create-fake-writer []
  (java.io.ByteArrayOutputStream.))

(defn get-fake-writer [writer]
  (java.io.PrintStream. writer))

(defn str-fake-writer [writer]
  (str writer))

(defmacro with-async [& body]
  `(clojure.test/async done#
                       (->
                        ~@body
                        (.catch (fn [err#] (clojure.test/is (nil? err#))))
                        (.then done#))))
