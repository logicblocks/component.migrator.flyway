(ns component.migrator.flyway.configuration
  (:require
   [configurati.core :as conf]))

(def locations-parameter
  (conf/parameter :locations
    {:default ["classpath:database/migrations"]}))
(def table-parameter
  (conf/parameter :table
    {:default "schema_version"}))
(def migrate-on-start-parameter
  (conf/parameter :migrate-on-start
    {:type :boolean :default true}))

(def specification
  (conf/configuration-specification
    (conf/with-parameter locations-parameter)
    (conf/with-parameter table-parameter)
    (conf/with-parameter migrate-on-start-parameter)))
