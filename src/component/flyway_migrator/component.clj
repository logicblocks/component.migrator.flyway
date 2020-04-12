(ns component.flyway-migrator.component
  (:require
   [com.stuartsierra.component :as component]

   [component.flyway-migrator.flyway :as flyway]))

(defrecord FlywayMigrator
           [data-source configuration]
  component/Lifecycle

  (start [component]
    (let [client (flyway/client {:data-source data-source})]
      (flyway/migrate client)
      (assoc component :client client)))
  (stop [component]
    (assoc component :client nil)))
