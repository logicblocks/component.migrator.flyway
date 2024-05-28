(ns component.migrator.flyway.core
  (:require
   [component.migrator.flyway.component :as component]
   [component.migrator.flyway.api :as api]))

(defn component
  ([]
   (component/map->FlywayMigrator {}))
  ([configuration data-source]
   (component/map->FlywayMigrator
     {:configuration configuration
      :data-source   data-source})))

(defn migrate [component]
  (api/migrate (:instance component)))
