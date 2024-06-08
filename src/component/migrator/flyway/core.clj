(ns component.migrator.flyway.core
  (:require
   [component.migrator.flyway.component :as component]
   [component.migrator.flyway.api :as api]))

(defn component
  ([]
   (component/map->FlywayMigrator {}))
  ([{:keys [configuration-specification
            configuration-source
            configuration-lookup-prefix
            configuration
            data-source
            logger]}]
   (component/map->FlywayMigrator
     {:configuration-specification configuration-specification
      :configuration-source        configuration-source
      :configuration-lookup-prefix configuration-lookup-prefix
      :configuration               configuration
      :data-source                 data-source
      :logger                      logger})))

(defn migrate [component]
  (api/migrate (:instance component)))
