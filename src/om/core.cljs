(ns om.core
  (:require React
            [om.dom :as dom :include-macros true]))

(def refresh-queued false)

(defn root [value f target]
  (let [state (if (instance? Atom value)
                value
                (atom value))
        rootf (fn []
                (set! refresh-queued false)
                (let [path []]
                  (dom/render
                    (dom/pure #js {:value @state :state state :path path}
                      (f (with-meta @state {::state state ::path path})))
                    target)))]
    (add-watch state ::root
      (fn [_ _ _ _]
        (when-not refresh-queued
          (set! refresh-queued true)
          (if (exists? js/requestAnimationFrame)
            (js/requestAnimationFrame rootf)
            (js/setTimeout rootf 16)))))
    (rootf)))

(defn render
  ([f data] (render f data nil))
  ([f data sorm]
    (cond
      (nil? sorm)
      (let [m (meta data)]
        (dom/pure #js {:value data :state (::state m) :path (::path m)}
          (f data)))

      (sequential? sorm)
      (let [data'  (get-in data sorm)
            m      (meta data)
            m'     (update-in m [::path] into sorm)
            mdata' (with-meta data' m')]
        (dom/pure #js {:value data' :state (::state m) :path (::path m')}
          (f mdata')))

      :else
      (let [{:keys [path key opts]} sorm
            dataf  (get sorm :fn)
            data'  (get-in data path)
            data'  (if-not (nil? dataf)
                     (dataf data')
                     data')
            rkey   (get data' key)
            m      (meta data)
            m'     (update-in m [::path] into path)
            mdata' (with-meta data' m')]
        (dom/pure #js {:value data' :state (::state m) :path (::path m') :key rkey}
          (if (nil? opts)
            (f mdata')
            (f mdata' opts)))))))

(defn replace!
  ([data v]
    (let [m (meta data)]
      (swap! (::state m) assoc-in (::path m) v)))
  ([data ks v]
    (let [m (meta data)]
      (swap! (::state m) assoc-in (into (::path m) ks) v))))

(defn update!
  ([data ks f]
    (let [m (meta data)]
      (swap! (::state m) update-in (into (::path m) ks) f)))
  ([data ks f a]
    (let [m (meta data)]
      (swap! (::state m) update-in (into (::path m) ks) f a)))
  ([data ks f a b]
    (let [m (meta data)]
      (swap! (::state m) update-in (into (::path m) ks) f a b)))
  ([data ks f a b c]
    (let [m (meta data)]
      (swap! (::state m) update-in (into (::path m) ks) f a b c)))
  ([data ks f a b c d]
    (let [m (meta data)]
      (swap! (::state m) update-in (into (::path m) ks) f a b c d)))
  ([data ks f a b c d & args]
    (let [m (meta data)]
      (apply swap! (::state m) update-in (into (::path m) ks) f a b c d args))))
