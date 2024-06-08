(ns component.migrator.flyway.component
  (:require
   [com.stuartsierra.component :as component]
   [component.migrator.flyway.api :as api]
   [component.migrator.flyway.configuration :as configuration]
   [component.support.logging :as comp-log]
   [configurati.component :as conf-comp]
   [configurati.core :as conf]))

(defrecord FlywayMigrator
  [configuration-specification
   configuration-source
   configuration-lookup-prefix
   configuration
   data-source
   logger
   instance]

  conf-comp/Configurable
  (configure [component opts]
    (comp-log/with-logging logger :component.migrator.flyway
      {:phases  {:before :configuring :after :configured}}
      (let [source
            (conf/multi-source
              (:configuration-source opts)
              configuration-source)
            configuration
            (conf/configuration
              (conf/with-lookup-prefix configuration-lookup-prefix)
              (conf/with-specification
                (or configuration-specification configuration/specification))
              (conf/with-source source))]
        (assoc component :configuration (conf/resolve configuration)))))

  component/Lifecycle
  (start [component]
    (comp-log/with-logging logger :component.migrator.flyway
      {:phases  {:before :starting :after :started}
       :context {:configuration configuration}}
      (let [instance
            (api/instance
              (merge
                {:data-source (:datasource data-source)}
                configuration))
            migrate-on-start
            (get configuration :migrate-on-start true)]
        (when migrate-on-start
          (api/migrate instance))
        (assoc component :instance instance))))

  (stop [component]
    (comp-log/with-logging logger :component.migrator.flyway
      {:phases  {:before :stopping :after :stopped}
       :context {:configuration configuration}}
      (assoc component :instance nil))))
