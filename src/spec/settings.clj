(ns spec.settings
  "Organizes and validates your settings with clojure.spec and spec-tools."
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]))

(defmacro for-map
  "Like for, but returns a map. Expects the result to be a pair or map."
  {:style/indent 1
   :private true}
  [& body]
  `(->> (for ~@body)
        (into {})))

(s/def ::keyword-map (s/map-of keyword? any?))
(s/def ::spec s/spec?)
(s/def ::doc string?)
(s/def ::default any?)
(s/def ::settings-config
  (s/map-of keyword? (s/keys :req-un [::spec]
                             :opt-un [::doc
                                      ::default])))

(comment

  (require '[spec-tools.spec :as spec])
  (def settings-config
    {:debug {:spec spec/boolean?
             :doc "When true, debug options are turned on."
             :default false}
     :num-processes {:spec spec/integer?
                     :doc "Number of processes to map over"
                     :default 4}
     :fav-color {:spec spec/string?
                 :doc "Your favorite color."}
     :foo  {:spec spec/string?
            :doc "Foo; not Bar, Baz, or Qux"
            :default "foo"}})

  )

(defn env
  "SampleAPI settings from the environment"
  [& [filter-prefix]]
  (for-map [[s v] (System/getenv)
            :when (if filter-prefix
                    (str/starts-with? s filter-prefix)
                    true)]
    {(-> (str/lower-case s)
         (str/replace "sampleapi_" "")
         (str/replace "_" "-")
         keyword)
     v}))

(s/fdef env
  :args (s/cat :filter-prefix (s/? string?))
  :ret ::keyword-map)

(defn props
  "SampleAPI settings from properties"
  [& [filter-prefix]]
  (for-map [[s v] (System/getProperties)
            :when (if filter-prefix
                    (str/starts-with? s filter-prefix)
                    true)]
    {(-> (str/lower-case s)
         (str/replace-first "sampleapi." "")
         (str/replace "." "-")
         keyword)
     v}))

(s/fdef props
  :args (s/cat :filter-prefix (s/? string?))
  :ret ::keyword-map)

(defn defaults
  "SampleAPI settings defaults"
  [& [settings-config]]
  (for-map [[setting {:keys [default] :as config}] settings-config
            :when (not= (config :default ::not-set) ::not-set)]
    {setting default}))

(s/fdef defaults
  :args (s/cat :settings-config (s/? ::settings-config))
  :ret ::keyword-map)

(defn all-settings
  "SampleAPI settings. If given a filter prefix, only return keys from environment/props matching that prefix"
  ([]                (all-settings nil nil))
  ([settings-config] (all-settings settings-config nil))
  ([settings-config filter-prefix]
   (merge (env (when filter-prefix (str/upper-case filter-prefix)))
          (props (when filter-prefix (str/lower-case filter-prefix)))
          (defaults settings-config))))

(s/fdef all-settings
  :args (s/cat :settings-config (s/? ::settings-config)
               :filter-prefix (s/? string?))
  :ret ::keyword-map)

(defn validate-settings-fn
  "Validates all-settings meet the settings-spec."
  [settings-spec all-settings]
  (st/decode settings-spec all-settings st/string-transformer))

(s/fdef validate-settings-fn
  :args (s/cat :settings-spec s/spec?
               :all-settings ::keyword-map)
  :ret (s/or :invalid s/invalid?
             :valid   ::keyword-map))

(defn validate-settings!
  "Validates all-settings meet the settings-spec. Exits the process with explanation on failure."
  ([settings-spec]
   (validate-settings! settings-spec (all-settings)))
  ([settings-spec all-settings]
   (let [settings (validate-settings-fn settings-spec all-settings)]
     (if (s/invalid? settings)
       (do
         (println "Invalid settings configuration. See spec output:")
         (println
          (with-out-str
            (st/explain settings-spec all-settings st/string-transformer)))
         (System/exit 1))
       settings))))

(defn settings-spec
  ([spec-name settings-config] (settings-spec spec-name settings-config false))
  ([spec-name settings-config lenient?]
   (->> {:spec (for-map [[setting {:keys [spec]}] settings-config]
                 {setting spec})
         :name ::settings}
        ds/spec)))

(s/fdef settings-spec
  :args (s/cat :spec-name s/spec?
               :settings-config ::settings-config)
  :ret s/spec?)

(comment

  (use 'clojure.repl)
  (require '[orchestra.spec.test :as stest])
  (stest/instrument)

  ;; specify your config
  (s/def ::settings (settings-spec ::settings settings-config))

  ;; you'd normally set these via environment or at app startup
  (System/setProperty "sampleapp.debug" "true")
  (System/setProperty "sampleapp.num.processes" "2")
  (System/setProperty "sampleapp.fav.color" "blue")

  (System/setProperty "sampleapp.bar" "pikachu")

  ;; validate your settings
  (= (validate-settings! ::settings (all-settings settings-config "sampleapp"))
     {:debug true
      :num-processes 2
      :fav-color "blue"
      :foo "foo"})

  )
