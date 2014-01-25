(ns big.prize
  (:require-macros [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(def app-state (atom {}))
