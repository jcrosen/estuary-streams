; Copyright 2016 Jeremy Crosen

; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at

;     http://www.apache.org/licenses/LICENSE-2.0

; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(set-env!
  :resource-paths #{"src"}
  :dependencies '[[tolitius/boot-check "0.1.6" :scope "test"]
                  [org.clojure/clojure "1.8.0"]
                  [org.clojure/core.async "0.2.391"]
                  [environ "1.1.0"]])

(task-options!
  pom {:project 'estuary-streams
       :version "0.1.0"})

(require '[tolitius.boot-check :as check])

(deftask build
  "Build project and install to repository"
  []
  (comp (pom) (jar) (install)))

(deftask lint []
  (comp
    (check/with-kibit)
    (check/with-eastwood)))

(deftask yagni []
  (comp 
    (check/with-yagni)))

(deftask bikeshed []
  (comp
    (check/with-bikeshed :options {:max-line-length 100})))
