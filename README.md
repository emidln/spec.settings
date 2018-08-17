# spec.settings

Data-driven settings declaration and validation using spec.

spec.settings takes a map declaring your settings and conforms your environment/properties to that declaration. 

## Usage

### Including in your project

Include:

```clj
[spec.settings "0.1.0"]
```

in your Leiningen's `project.clj` `:dependencies`.

### Declaring some settings

```clj
(ns sampleapi.settings
  "Settings, specification, and defaults for SampleAPI"
  (:require [clojure.spec.alpha :as s]
            [spec-tools.spec :as spec]
            [spec.settings :as ss]))

(def settings-config {:http-host {:spec spec/string?
                                  :doc "Host to run our http service on (default: localhost)"
                                  :default "localhost"}
                      :http-port {:spec spec/integer?
                                  :doc "Port number to run our http service on (default: 8080)"
                                  :default 8080}})

(s/def ::settings (ss/settings-spec ::settings settings-config))

(defn validate-settings!
  "A thunk for easy settings resolution."
  []
  (ss/validate-settings! ::settings (ss/all-settings settings-config "sampleapi")))

(s/fdef validate-settings!
  :ret ::settings)

```

From here, you now have a function, `sampleapi.settings/validate-settings!` which will validate environment variables beginning with `SAMPLEAPI_` and properties beginning with `sampleapi.` against your settings-config. Defaults are not required; however, in the absence of a default, the value must be provided in the environment or as a property. Any settings that begin with your prefix found in the environment or properties will cause validation to fail (you cannot opt out of this right now; if it's an issue file a bug report).


## License

Copyright © 2018 Brandon Joseph Adams <emidln@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
