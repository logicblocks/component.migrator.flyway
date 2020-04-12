(ns component.flyway-migrator.flyway
  (:import
   [org.flywaydb.core Flyway]))

(defn client [{:keys [data-source]}]
  (-> (Flyway/configure)
    (.dataSource data-source)
    (.load)))

(defn migrate [^Flyway flyway]
  (.migrate flyway))
