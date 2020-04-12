(ns component.flyway-migrator.core
  (:require
   [component.flyway-migrator.component :as component]))

(defn create
  ([] (create {}))
  ([parameters] (component/map->FlywayMigrator parameters)))
