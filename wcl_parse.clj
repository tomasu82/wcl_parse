(require '[clojure.data.json :as json])
(require '[clj-http.client :as client])
(require '[clojure.string :as str])
; this curl command is an example of getting a particular report.
; I have had problems getting a list of report based on guild name.
                                        ;curl "https://www.warcraftlogs.com/v1/report/tables/damage-done/kVcHmYFapK4njyCgkVcHmYFapK4njyCg?api_key=<api_key>&start=21&end=278663" > test.js

(def input (slurp "test.js"))
(def reports (str/split-lines (slurp "reports.txt")))

(defn get_damage_report_url
  [report api_key start end]
  (str "https://www.warcraftlogs.com/v1/report/tables/damage-done/"
       report
       "?api_key="
       api_key
       "&start="
       start
       "&end="
       end))

(defn get_fight_url
  [report api_key]
  (str "https://www.warcraftlogs.com/v1/report/fights/"
       report
       "?api_key="
       api_key))


                                        ; given a report you:
                                        ; get fight url and get all the kills
                                        ; for each kill, get damage report and top damage dealer

; parse response body
(defn kills
  [report]
  (filter #(= (:kill %1) true)
          report))

(defn download_fights
  [report api_key]
  (let [fight_info_json (-> (client/get (get_fight_url report api_key))
                            :body)
        fight_info (-> fight_info_json
                       (json/read-str :key-fn keyword)
                       :fights
                       kills)]
    (spit (str "/home/thomas/development/wcl_parse/parses/fight_" report ".json")
          fight_info_json)
    (map
     #(let [curr_fight %1]
        (spit (str "/home/thomas/development/wcl_parse/parses/damage_"
                   report "_" (str/replace (:name curr_fight) #" " "_") ".json")
              (-> (client/get (get_damage_report_url
                               report
                               api_key
                               (:start_time curr_fight)
                               (:end_time curr_fight)))
                  :body)))
     fight_info)
    ))

(def test_fight
  (-> (slurp "/home/thomas/development/wcl_parse/parses/fight_27bTvtVKcrGNYqpJ.json")
      (json/read-str :key-fn keyword)
      :fights
      kills))


  (def report "27bTvtVKcrGNYqpJ" )



(def downloaded_fights
  (map
   #(download_fights %1 api_key)
   reports))

(defn get_top_ranks
  [report api_key]
  (let [fight_info (get_fight_info report api_key)]
    (reduce
     #(let [curr_fight %2
            dmg_report (get_dmg_report report
                                       api_key
                                       (:start_time curr_fight)
                                       (:end_time curr_fight))]

            (conj %1 (:name (apply max-key :total
                   (-> dmg_report first val))))
            )
     []
     fight_info)))

(defn get_dmg_report
  [report api_key start end]
  (-> (client/get (get_damage_report_url
                   report
                   api_key
                   start
                   end))
      :body
      (json/read-str :key-fn keyword)))

(defn get_top_ranks
  [report api_key]
  (let [fight_info (-> (client/get (get_fight_url report api_key))
                       :body
                       (json/read-str :key-fn keyword)
                       :fights
                       kills)]
    fight_info
    ))
(defn get_fight_info
  [report api_key]
  (-> (client/get (get_fight_url report api_key))
      :body
      (json/read-str :key-fn keyword)
      :fights
      kills))


(defn dps [total_dmg length]
  (* 1000 (/ total_dmg length)))



(def all_ranks
  (map
   #(get_top_ranks %1 api_key)
   reports))
(def tops
  (flatten all_ranks))

(def top_counts
  (reduce
   #(assoc
     %1
    (keyword %2) (inc ((keyword %2) %1 0)))
   {}
   tops))

(def sorted_tops
  (reverse (sort-by val top_counts)))
