(ns component.flyway-migrator.core
  (:require
   [component.flyway-migrator.component :as component]
   [component.flyway-migrator.flyway :as flyway]))

(defn create
  ([] (create {}))
  ([parameters] (component/map->FlywayMigrator parameters)))

(defn migrate [component]
  (flyway/migrate (:client component)))
