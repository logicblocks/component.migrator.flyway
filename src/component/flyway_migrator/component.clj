(ns component.flyway-migrator.component
  (:require
   [com.stuartsierra.component :as component]))

(defrecord FlywayMigrator
           [data-source configuration]
  component/Lifecycle

  (start [component]
    component)
  (stop [component]
    component))
