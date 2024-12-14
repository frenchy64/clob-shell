(ns clob.readline.line-reader-class)

(gen-class
  :name "clob.readline.jline-api.RebelLineReaderImpl"
  :extends org.jline.reader.impl.LineReaderImpl
  :exposes {size {:get getSize}
            reading {:get getReading}
            post {:set setPost}}
  :exposes-methods {selfInsert pubSelfInsert})
