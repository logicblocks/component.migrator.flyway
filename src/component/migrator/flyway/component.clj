(ns component.migrator.flyway.component
  (:require
   [cartus.core :as log]
   [com.stuartsierra.component :as component]
   [component.migrator.flyway.api :as flyway-api]))

(defn- with-logging-fn [logger target opts action-fn]
  (let [init-ms (System/currentTimeMillis)]
    (when logger
      (log/debug logger (keyword (name target)
                          (name (get-in opts [:phases :before])))
        (:context opts)))
    (let [result (action-fn)]
      (when logger
        (log/info logger (keyword (name target)
                           (name (get-in opts [:phases :after])))
          (merge {:elapsed-ms (- (System/currentTimeMillis) init-ms)}
            (:context opts))))
      result)))

(defmacro ^:private with-logging [logger target opts & body]
  `(with-logging-fn ~logger ~target ~opts
     (fn [] ~@body)))

(defrecord FlywayMigrator
  [configuration data-source logger instance]

  component/Lifecycle
  (start [component]
    (with-logging logger :component.migrator.flyway
      {:phases  {:before :starting :after :started}
       :context {:configuration configuration}}
      (let [instance
            (flyway-api/instance
              (merge
                {:data-source (:datasource data-source)}
                configuration))
            migrate-on-start
            (get configuration :migrate-on-start true)]
        (when migrate-on-start
          (flyway-api/migrate instance))
        (assoc component :instance instance))))

  (stop [component]
    (with-logging logger :component.migrator.flyway
      {:phases  {:before :stopping :after :stopped}
       :context {:configuration configuration}}
      (assoc component :instance nil))))
