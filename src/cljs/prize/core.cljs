(ns prize.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.data :as data]
            [clojure.string :as string]
            [secretary.core :as secretary]
            [clojure.browser.repl :as repl])
  (:import [goog History]
           [goog.history EventType]))

(repl/connect "http://localhost:9000/repl")

(enable-console-print!)

(def app-state
  (atom
    {:route :all
     :teams [{:id 1 :points 0 :turn? true}
             {:id 2 :points 0 :turn? false}]
     :categories [{:name "Allgemein"
                   :challenges [{:id 0 :type :text :taken false :points 20
                                 :text "Wie heißt der höchste Berg Europas?"}
                                {:id 1 :type :text :taken false :points 40
                                 :text "Wie heißt der Erfinder des Dynamits?"}
                                {:id 2 :type :text :taken false :points 60
                                 :text "Wie viele Sitze hat der Bundestag im Moment?"}
                                {:id 3 :type :text :taken false :points 80
                                 :text "Auf welchem Kontinent findet man die schwarze Mamba?"}
                                {:id 4 :type :text :taken false :points 100
                                 :text "Welches DIN-Format besitzt die Maße 841x1189mm?"}]}
                  {:name "Rekorde"
                   :challenges [{:id 5 :type :text :taken false :points 20
                                 :text "Wie viele Leute passen in einen Smart?"}
                                {:id 6 :type :text :taken false :points 40
                                 :text "Wie viele Sekunden beträgt der Weltrekord im Krabbeln über eine Distanz von 100 Meter?"}
                                {:id 7 :type :text :taken false :points 60
                                 :text "Wie viele gelbe und rote Karten wurden insgesamt im Viertelfinale der WM 2006 im Spiel Portugal gegen die Niederlande verteilt?"}
                                {:id 8 :type :text :taken false :points 80
                                 :text "Wie viele Runden dauerte der längste Boxkampf der Welt?"}
                                {:id 9 :type :text :taken false :points 100
                                 :text "Wie viele Flaschen Wein à 0.75 Liter hätte Jesus bei der Hochzeit von Kana füllen können?"}]}
                  {:name "Musik"
                   :challenges [{:id 10 :type :sync :taken false :points 20
                                 :video "/assets/videos/grenade.mp4"
                                 :sound1 "/assets/music/behind-blue-eyes.mp3"
                                 :sound2 "/assets/music/smells-like-teen-spirit.mp3"}
                                {:id 11 :type :sync :taken false :points 40
                                 :video "/assets/videos/get-lucky.m4v"
                                 :sound1 "/assets/music/sandstorm.mp3"
                                 :sound2 "/assets/music/dickes-b.mp3"}
                                {:id 12 :type :sync :taken false :points 60
                                 :video "/assets/videos/feel.mp4"
                                 :sound1 "/assets/music/just-like-a-pill.mp3"
                                 :sound2 "/assets/music/pokerface.mp3"}
                                {:id 13 :type :sync :taken false :points 80
                                 :video "/assets/videos/thrift-shop.mp4"
                                 :sound1 "/assets/music/tik-tok.mp3"
                                 :sound2 "/assets/music/in-the-club.mp3"}
                                {:id 14 :type :sync :taken false :points 100
                                 :video "/assets/videos/smooth-criminal.mp4"
                                 :sound1 "/assets/music/gangstas-paradise.mp3"
                                 :sound2 "/assets/music/jump-around.mp3"}]}
                  {:name "Floppies"
                   :challenges [{:id 15 :type :sound :taken false :points 20
                                 :sound "/assets/floppies/james-bond-theme.mp4"}
                                {:id 16 :type :sound :taken false :points 40
                                 :sound "/assets/floppies/hes-a-pirate.mp4"}
                                {:id 17 :type :sound :taken false :points 60
                                 :sound "/assets/floppies/gangnam-style.mp4"}
                                {:id 18 :type :sound :taken false :points 80
                                 :sound "/assets/floppies/party-rock-anthem.mp4"}
                                {:id 19 :type :sound :taken false :points 100
                                 :sound "/assets/floppies/rudolph-the-rednosed-reindeer.m4v"}]}
                  {:name "Produkt/Stadt"
                   :challenges [{:id 20 :type :image :taken false :points 20
                                 :image "/assets/cities/koeln.jpg"}
                                {:id 21 :type :image :taken false :points 40
                                 :image "/assets/cities/zuffenhausen.jpg"}
                                {:id 22 :type :image :taken false :points 60
                                 :image "/assets/cities/muenchen.jpg"}
                                {:id 23 :type :image :taken false :points 80
                                 :image "/assets/cities/dublin.jpg"}
                                {:id 24 :type :image :taken false :points 100
                                 :image "/assets/cities/mailand.jpg"}]}]}))

(def team-id
  (let [id (atom 2)]
    (fn [] (do
             (swap! id inc)
             @id))))

(defn create-team []
  (let [id (team-id)]
    {:id id :points 0 :turn? false}))

(defn team [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-team]}]
      (dom/div #js {:className (str "team team-" (:id app)
                                 (when (true? (:turn? app))
                                   " turn"))
                    :onDoubleClick #(put! select-team app)}
        (dom/span nil (:points app))))))

(defn teams-panel [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [add-team select-team]}]
      (dom/div #js {:className "teams-panel"}
        (dom/button #js {:className "add-team"
                         :onClick #(put! add-team :add-team)}
          "+")
        (om/build-all team (:teams app)
          {:init-state {:select-team select-team}})))))

(defn challenge-header [points partial]
  (dom/div #js {:className "challenge-box"}
    (dom/div #js {:className "challenge-header"} points)
    partial))

;;; Start video (muted) + 2 audio
;;;
(defn sync-challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [close-challenge]}]
      (challenge-header (:points app)
        (dom/div #js {:className ""}
          (dom/video #js {:height "480" :width "853" :data-muted "muted"
                          :controls true :autoPlay true}
            (dom/source #js {:type "video/mp4" :muted true :src (:video app)}))
          (dom/audio #js {:autoPlay true :controls true :loop true}
            (dom/source #js {:type "audio/mpeg" :src (:sound1 app)}))
          (dom/audio #js {:autoPlay true :controls true :loop true}
            (dom/source #js {:type "audio/mpeg" :src (:sound2 app)})))))))

(defn background-image-style [url]
  #js {:backgroundImage (str "url(" url ")")})

;;; Show image
(defn image-challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [close-challenge]}]
      (challenge-header (:points app)
        (dom/div #js {:className "challenge-image"
                      :style (background-image-style (:image app))})))))

;;; Start video (no image)
(defn sound-challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [close-challenge]}]
      (challenge-header (:points app)
        (dom/div #js {:className ""}
          (dom/video #js {:height "10" :width "500" :controls true :autoPlay true}
            (dom/source #js {:type "video/mp4" :src (:sound app)})))))))

;;; Show text
(defn text-challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [close-challenge]}]
      (challenge-header (:points app)
        (dom/div #js {:className ""}
          (:text app))))))

(defn choose-challenge [type]
  (case type
    :text text-challenge
    :sound sound-challenge
    :image image-challenge
    :sync sync-challenge))

(defn challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-field]}]
      (dom/div #js {:className (str "challenge" (when (true? (:taken app))
                                                  " taken"))
                    :onClick (when (false? (:taken app))
                               #(put! select-field @app))}
        (:points app)))))

(defn category [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-field]}]
      (dom/div #js {:className "category"}
        (dom/div #js {:className "category-name"} (:name app))
        (om/build-all challenge (:challenges app)
          {:init-state {:select-field select-field}})))))

(defn categories [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-field]}]
      (dom/div #js {:className "categories"}
        (om/build-all category (:categories app)
          {:init-state {:select-field select-field}})))))

(defn chall-id-map [id]
  [(quot id 5) (mod id 5)])

(defn reward-team [app team]
  (when (not= :all (:route @app))
    (om/update! app
      (fn [app]
        (let [points (get-in app [:route :points])
              team-count (count (:teams app))
              tid (dec (:id @team))
              turn-tid (dec (:id (first (filter #(= (:turn? %) true) (:teams app)))))
              nid (mod (inc turn-tid) team-count)
              [cat-id chall-id] (chall-id-map (get-in app [:route :id]))]
          ;; (.log js/console team-count)
          ;; (.log js/console tid)
          ;; (.log js/console turn-tid)
          ;; (.log js/console cat-id)
          ;; (.log js/console chall-id)
          ;; (.log js/console (get-in app [:categories cat-id :challenges chall-id :taken])
          (-> app
            (update-in [:teams tid :points] #(+ % points))
            (assoc-in [:teams tid :turn?] false)
            (assoc-in [:teams turn-tid :turn?] false)
            (assoc-in [:teams nid :turn?] true)
            (assoc-in [:route] :all)
            (assoc-in [:categories cat-id :challenges chall-id :taken] true)))))))

(defn game [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:add-team (chan)
       :select-field (chan)
       :select-team (chan)})
    om/IWillMount
    (will-mount [_]
      (let [add-team (om/get-state owner :add-team)]
        (go (loop []
              (<! add-team)
              (om/transact! app :teams
                (fn [xs]
                  (if (< (count xs) 8)
                    (conj xs (create-team))
                    xs)))
              (recur))))
      (let [select-field (om/get-state owner :select-field)]
        (go (loop []
              (let [challenge (<! select-field)]
                ;; Display the selected challenge
                (om/transact! app :route swap! :route challenge)
                (recur)))))
      (let [select-team (om/get-state owner :select-team)]
        (go (loop []
              (let [selected-team (<! select-team)]
                (reward-team app selected-team))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [add-team select-field select-team]}]
      (let [route (:route app)]
        (let [partial
              (if (= :all route)
                (om/build categories app
                  {:init-state {:select-field select-field}})
                (om/build (choose-challenge (:type route)) route))]
          (dom/section #js {:className "game"}
            (om/build teams-panel app
              {:init-state {:add-team add-team :select-team select-team}})
            partial))))))

(om/root
  app-state
  game
  (.getElementById js/document "game-root"))

;;; Allgemein (Text)
;;; Wie heißt der höchste Berg Europas? - Mont Blanc
;;;
;;;
;;;
;;;

;;; Rekorde (Text)
;; Schätzfragen
;;; Wie viele Leute passen in einen Smart - 20
;;; Schnellste Zeit auf 100m; im Krabbeln - 16,87s
;;; Wie viele Flaschen Wein hätte Jesus bei der Hochzeit von Kanaa gefüllt - 908
;;; Wie lange dauerte der längste Boxkampf der Welt - 42 Runden
;;; Wie viele Karten (rot + gelb) gab es im Viertelfinale der WM 2006 (Portugal - Niederlande) - 20, 4x rot, 16x gelb

;;; Musik (Video + 2x Sound)
;; Musikvideo + zwei parallele Tracks
;;; Bruno Mars - Wonderwall - Behind blue eyes
;;; Daft Punk - Sandstorm - Dickes B
;;; Robbie Williams - Just like a pill - Pokerface
;;; Macklemore - Tik tok - In the club
;;; Michael Jackson - Gangsta's Paradise - Jump around

;;; Floppies (Sound)
;; Musiktitel von Floppy Disk erraten
;;; James Bond
;;; He's a pirate
;;; Gangnam style
;;; Party rock anthem
;;; Rudolph the rednosed reindeer

;;; Produkt/Stadt (Bild)
;; Finde Heimatstandort der Marke
;;; Kölsch Wasser - Köln
;;; Porsche - Zuffenhausen
;;; Siemens - München
;;; Guinness - Dublin
;;; Prada - Mailand
