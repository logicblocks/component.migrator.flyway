(ns component.flyway-migrator.flyway
  (:import
   [org.flywaydb.core Flyway]))

(defn client [{:keys [data-source locations table]}]
  (let [configuration (Flyway/configure)
        configuration (.dataSource configuration data-source)
        configuration (if-not (nil? locations)
                        (.locations configuration
                          ^"[Ljava.lang.String;" (into-array String locations))
                        configuration)
        configuration (if-not (nil? table)
                        (.table configuration table)
                        configuration)]
    (.load configuration)))

(defn migrate [^Flyway flyway]
  (.migrate flyway))
