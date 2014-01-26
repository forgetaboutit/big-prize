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
     :categories [{:name "" :challenges []}]}))

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
      (dom/li #js {:className "team"}
        (dom/span nil (:points app))))))

(defn teams-panel [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [add-team]}]
      (dom/div #js {:className "team-panel"}
        (dom/button #js {:className "add-team"
                         :onClick #(put! add-team :add-team)}
          "+")
        (dom/ul nil
          (om/build-all team (:teams app)))))))

(defn game [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:add-team (chan)})
    om/IWillMount
    (will-mount [_]
      (let [add-team (om/get-state owner :add-team)]
        (go (loop []
              (<! add-team)
              (om/transact! app :teams conj (create-team))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [add-team]}]
      (dom/div #js {:className "game"}
        (om/build teams-panel app
          {:init-state {:add-team add-team}})))))

(om/root
  app-state
  game
  (.getElementById js/document "game-root"))

