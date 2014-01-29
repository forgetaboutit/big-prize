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
                   :challenges [{:points 20 :type :text :text "Hello World!" :taken false}
                                {:points 40 :type :text :text "Hello World!" :taken false}
                                {:points 60 :type :text :text "Hello World!" :taken false}
                                {:points 80 :type :text :text "Hello World!" :taken false}
                                {:points 100 :type :text :text "Hello World!" :taken false}]}
                  {:name "Sport"
                   :challenges [{:points 20 :type :text :text "Hello World!" :taken false}
                                {:points 40 :type :text :text "Hello World!" :taken false}
                                {:points 60 :type :text :text "Hello World!" :taken false}
                                {:points 80 :type :text :text "Hello World!" :taken false}
                                {:points 100 :type :text :text "Hello World!" :taken false}]}
                  {:name "Rekorde"
                   :challenges [{:points 20 :type :text :text "Hello World!" :taken false}
                                {:points 40 :type :text :text "Hello World!" :taken false}
                                {:points 60 :type :text :text "Hello World!" :taken false}
                                {:points 80 :type :text :text "Hello World!" :taken false}
                                {:points 100 :type :text :text "Hello World!" :taken false}]}
                  {:name "Musik"
                   :challenges [{:points 20 :type :text :text "Hello World!" :taken false}
                                {:points 40 :type :text :text "Hello World!" :taken false}
                                {:points 60 :type :text :text "Hello World!" :taken false}
                                {:points 80 :type :text :text "Hello World!" :taken false}
                                {:points 100 :type :text :text "Hello World!" :taken false}]}
                  {:name "Filme"
                   :challenges [{:points 20 :type :text :text "Hello World!" :taken false}
                                {:points 40 :type :text :text "Hello World!" :taken false}
                                {:points 60 :type :text :text "Hello World!" :taken false}
                                {:points 80 :type :text :text "Hello World!" :taken false}
                                {:points 100 :type :text :text "Hello World!" :taken false}]}
                  {:name "Feiertage"
                   :challenges [{:points 20 :type :text :text "Hello World!" :taken false}
                                {:points 40 :type :text :text "Hello World!" :taken false}
                                {:points 60 :type :text :text "Hello World!" :taken false}
                                {:points 80 :type :text :text "Hello World!" :taken false}
                                {:points 100 :type :text :text "Hello World!" :taken false}]}]}))

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
    om/IRender
    (render [this]
      (dom/div #js {:className (str "team team-" (:id app)
                                 (when (true? (:turn? app))
                                   " turn"))}
        (dom/span nil (:points app))))))

(defn teams-panel [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [add-team]}]
      (dom/div #js {:className "teams-panel"}
        (dom/button #js {:className "add-team"
                         :onClick #(put! add-team :add-team)}
          "+")
        (om/build-all team (:teams app))))))

(defn challenge-header [points partial]
  (dom/div #js {:className "challenge-box"}
    (dom/div #js {:className "challenge-header"} points)
    partial))

(defn text-challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [close-challenge]}]
      (challenge-header (:points app)
        (dom/div #js {:className ""}
          (:text app))))))

(defn choose-challenge [type]
  (case type
    :text text-challenge))

(defn challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-field]}]
      (dom/div #js {:className "challenge"
                    :onClick #(put! select-field @app)}
        (:points app)))))

(defn category [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-field]}]
      (.log js/console app)
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

(defn game [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:add-team (chan)
       :select-field (chan)})
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
                (recur))))))
    om/IRenderState
    (render-state [this {:keys [add-team select-field]}]
      (let [route (:route app)]
        (let [partial
              (if (= :all route)
                (om/build categories app
                  {:init-state {:select-field select-field}})
                (om/build (choose-challenge (:type route)) route))]
          (dom/section #js {:className "game"}
            (om/build teams-panel app
              {:init-state {:add-team add-team}})
            partial))))))

(om/root
  app-state
  game
  (.getElementById js/document "game-root"))
