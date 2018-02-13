(require '[clojure.data.json :as json])
; this curl command is an example of getting a particular report.
; I have had problems getting a list of report based on guild name.
;curl "https://www.warcraftlogs.com/v1/report/tables/damage-done/kVcHmYFapK4njyCg?api_key=<api_key>&start=21&end=278663" > test.js

(def input (slurp "test.js"))

(def input_json (json/read-str input :key-fn keyword))

; names with dmg values parsed out
(def names
  (map #(hash-map :name (:name %1) :total (:total %1)) (-> input_json first val)))

(def sorted_names
 (sort-by :total > names))

(def fight_length
  (:totalTime input_json))

(defn dps [total_dmg length]
  (* 1000 (/ total_dmg length)))
