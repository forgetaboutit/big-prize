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

;; is-turn-of :team
;;
(def app-state
  (atom
    {:teams [{:id 1 :points 0}]
     :categories [{:name "Allgemeinwissen"
                   :challenges [{:text "Hallo Welt!" :taken false}
                                {:text "Hello World!" :taken false}]}
                  {:name "Sport"
                   :challenges [{:text "Michael Schumacher" :taken false}
                                {:text "Blarg!" :taken false}]}]}))

(def team-id
  (let [id (atom 1)]
    (fn [] (do
             (swap! id inc)
             @id))))

(defn create-team []
  (let [id (team-id)]
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
              (om/transact! app :teams
                (fn [xs]
                  (if (< (count xs) 8)
                    (conj xs (create-team))
                    xs)))
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

;; ;;     om/IInitState
;; ;;     (init-state [_]
;; ;;       {:delete (chan)})
;; ;;     om/IWillMount
;; ;;     (will-mount [_]
;; ;;       (let [delete (om/get-state owner :delete)]
;; ;;         (go (loop []
;; ;;               (let [contact (<! delete)]
;; ;;                 (om/transact! app :contacts
;; ;;                   (fn [xs] (into [] (remove #(= contact %) xs))))
;; ;;                 (recur))))))
;; ;;     om/IRenderState
;; ;;     (render-state [this {:keys [delete]}]
;; ;;       (dom/div nil
;; ;;         (dom/h1 nil "Contact list")
;; ;;         (apply dom/ul nil
;; ;;           (om/build-all contact-view (:contacts app)
;; ;;             {:init-state {:delete delete}}))
;; ;;         (dom/div nil
;; ;;           (dom/input #js {:type "text" :ref "new-contact"})
;; ;;           (dom/button #js {:onClick #(add-contact app owner)} "Add contact"))))))

;; ;; (def app-state
;; ;;   (atom
;; ;;     {:contacts
;; ;;      [{:first "Ben" :last "Bitdiddle" :email "benb@mit.edu"}
;; ;;       {:first "Alyssa" :middle-initial "P" :last "Hacker" :email "aphacker@mit.edu"}
;; ;;       {:first "Eva" :middle "Lu" :last "Ator" :email "eval@mit.edu"}
;; ;;       {:first "Louis" :last "Reasoner" :email "prolog@mit.edu"}
;; ;;       {:first "Cy" :middle-initial "D" :last "Effect" :email "bugs@mit.edu"}
;; ;;       {:first "Lem" :middle-initial "E" :last "Tweakit" :email "morebugs@mit.edu"}]}))

;; ;; (defn middle-name [{:keys [middle middle-initial]}]
;; ;;   (cond
;; ;;     middle (str " " middle)
;; ;;     middle-initial (str " " middle-initial ".")))

;; ;; (defn display-name [{:keys [first last] :as contact}]
;; ;;   (str last ", " first (middle-name contact)))

;; ;; (defn parse-contact [contact-str]
;; ;;   (let [[first middle last :as parts] (string/split contact-str #"\s+")
;; ;;         [first last middle] (if (nil? last) [first middle] [first last middle])
;; ;;         middle (when middle (string/replace middle "." ""))
;; ;;         c (if middle (count middle) 0)]
;; ;;     (when (>= (reduce + (map #(if % 1 0) parts)) 2)
;; ;;       (cond-> {:first first :last last}
;; ;;         (== c 1) (assoc :middle-initial middle)
;; ;;         (>= c 2) (assoc :middle middle)))))

;; ;; (defn add-contact [app owner]
;; ;;   (let [input-field (om/get-node owner "new-contact")
;; ;;         new-contact (-> input-field
;; ;;                       .-value
;; ;;                       parse-contact)]
;; ;;     (when new-contact
;; ;;       (om/transact! app :contacts conj new-contact)
;; ;;       (set! (.-value input-field) ""))))

;; ;; (defn contact-view [contact owner]
;; ;;   (reify
;; ;;     om/IRenderState
;; ;;     (render-state [this {:keys [delete]}]
;; ;;       (dom/li nil
;; ;;         (dom/span nil (display-name contact))
;; ;;         (dom/button #js {:onClick (fn [e] (put! delete @contact))}
;; ;;           "Delete")))))

;; ;; (defn contacts-view [app owner]
;; ;;   (reify
;; ;;     om/IInitState
;; ;;     (init-state [_]
;; ;;       {:delete (chan)})
;; ;;     om/IWillMount
;; ;;     (will-mount [_]
;; ;;       (let [delete (om/get-state owner :delete)]
;; ;;         (go (loop []
;; ;;               (let [contact (<! delete)]
;; ;;                 (om/transact! app :contacts
;; ;;                   (fn [xs] (into [] (remove #(= contact %) xs))))
;; ;;                 (recur))))))
;; ;;     om/IRenderState
;; ;;     (render-state [this {:keys [delete]}]
;; ;;       (dom/div nil
;; ;;         (dom/h1 nil "Contact list")
;; ;;         (apply dom/ul nil
;; ;;           (om/build-all contact-view (:contacts app)
;; ;;             {:init-state {:delete delete}}))
;; ;;         (dom/div nil
;; ;;           (dom/input #js {:type "text" :ref "new-contact"})
;; ;;           (dom/button #js {:onClick #(add-contact app owner)} "Add contact"))))))

;; ;; (om/root
;; ;;   app-state
;; ;;   contacts-view
;; ;;   (.getElementById js/document "game-root"))
