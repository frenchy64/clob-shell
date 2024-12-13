(ns clob.zero.parser
  (:require #?(:cljs [clob.zero.parser-spec :as parser]
               :clj [clob.zero.parser-squarepeg :as parser])))

(def parse parser/parse)
