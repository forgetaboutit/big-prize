(ns big.prize
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.data :as data]
            [clojure.string :as string]
            [secretary.core :as secretary])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(def app-state
  (atom
    {:teams []
     :categories [{:name "Allgemeinwissen"
                   :challenges [{:text "Hallo Welt!" :taken false}
                                {:text "Hello World!" :taken false}]}
                  {:name "Sport"
                   :challenges [{:text "Michael Schumacher" :taken false}
                                {:text "Blarg!" :taken false}]}]}))

(def team-id
  (let [id (atom 0)]
    (fn [] (do
             (swap! id inc)
             @id))))

(defn create-team []
  (let [id (team-id)]
    (.log js/console (str id))
    {:id id :points 0}))

(defn team [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "team team-" (:id app))}
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

(defn challenge [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-field]}]
      (dom/div #js {:className "challenge"
                    :onClick #(put! select-field @app)}
        (:text app)))))

(defn category [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [select-field]}]
      (.log js/console app)
      (dom/div #js {:className "category"}
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
              (om/transact! app :teams conj (create-team))
              (recur))))
      (let [select-field (om/get-state owner :select-field)]
        (go (loop []
              (.log js/console (<! select-field))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [add-team select-field]}]
      (dom/section #js {:className "game"}
        (om/build teams-panel app
          {:init-state {:add-team add-team}})
        (om/build categories app
          {:init-state {:select-field select-field}})))))

(om/root
  app-state
  game
  (.getElementById js/document "game-root"))

