(ns component.migrator.flyway.component
  (:require
   [com.stuartsierra.component :as component]
   [component.migrator.flyway.api :as flyway-api]
   [component.support.logging :as comp-log]))

(defrecord FlywayMigrator
  [configuration data-source logger instance]

  component/Lifecycle
  (start [component]
    (comp-log/with-logging logger :component.migrator.flyway
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
    (comp-log/with-logging logger :component.migrator.flyway
      {:phases  {:before :stopping :after :stopped}
       :context {:configuration configuration}}
      (assoc component :instance nil))))
