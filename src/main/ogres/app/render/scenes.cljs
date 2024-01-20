(ns ogres.app.render.scenes
  (:require [clojure.string :refer [replace]]
            [ogres.app.hooks :refer [use-dispatch use-query]]
            [ogres.app.render :refer [icon]]
            [uix.core :refer [defui $]]))

(def ^:private filename-re #"\d+x\d+|[^\w ]|.[^.]+$")

(defn ^:private render-scene-name [camera]
  (if-let [label (:camera/label camera)]
    label
    (if-let [filename (-> camera :camera/scene :scene/image :image/name)]
      (replace filename filename-re "")
      "Untitled scene")))

(defn ^:private render-remove-prompt [camera]
  (str "Are you sure you want to remove '" (render-scene-name camera) "'?"))

(def ^:private query
  [:local/camera
   {:local/cameras
    [:db/id
     :camera/label
     {:camera/scene
      [{:scene/image [:image/name]}]}]}])

(defui scenes []
  (let [dispatch (use-dispatch)
        result   (use-query query)]
    ($ :ul.scenes
      {:role "tablist" :aria-label "Select an existing scene to view."}
      (for [camera (:local/cameras result)
            :let [id (:db/id camera)
                  key (str "scene" id)
                  label (render-scene-name camera)
                  selected (= id (:db/id (:local/camera result)))]]
        ($ :li {:key id}
          ($ :input
            {:id key
             :role "tab"
             :type "radio"
             :name "scene"
             :value id
             :checked selected
             :aria-selected selected
             :on-change (fn [event] (dispatch :scenes/change (js/Number (.. event -target -value))))})
          ($ :label {:for key} label)
          ($ :button
            {:type "button"
             :title (str "Remove '" label "'.")
             :tabIndex -1
             :on-click
             (fn []
               (if (js/confirm (render-remove-prompt camera))
                 (dispatch :scenes/remove id)))}
            ($ icon {:name "x" :size 21}))))
      ($ :li.scenes-create
        ($ :button
          {:type "button"
           :title "Create a new scene."
           :on-click #(dispatch :scenes/create)}
          ($ icon {:name "plus" :size 19}))))))
