(ns clob.readline.line-reader-class)

(gen-class
  :name "clob.readline.line_reader_class.ClobLineReaderImpl"
  :extends org.jline.reader.impl.LineReaderImpl
  :exposes {size {:get getSize}
            reading {:get getReading}
            post {:set setPost}}
  :exposes-methods {selfInsert pubSelfInsert})
