(ns component.migrator.flyway.core-test
  (:require
   [clojure.test :refer :all]
   [clojure.java.jdbc :as jdbc]
   [com.stuartsierra.component :as component]
   [component.migrator.flyway.core :as flyway-migrator]
   [component.migrator.flyway.configuration :as configuration]
   [configurati.component :as conf-comp]
   [configurati.core :as conf])
  (:import
   [org.postgresql.ds PGSimpleDataSource]))

(defn data-source [& {:as overrides}]
  (let [{:keys [host port user password database-name]}
        (merge
          {:host          "localhost"
           :port          5434
           :user          "admin"
           :password      "super-secret"
           :database-name "some-database"}
          overrides)]
    (doto (PGSimpleDataSource.)
      (.setServerNames (into-array String [host]))
      (.setPortNumbers (int-array [port]))
      (.setUser user)
      (.setPassword password)
      (.setDatabaseName database-name))))

(defn clear-schema [db-spec schema]
  (jdbc/execute! db-spec (str "DROP SCHEMA " schema " CASCADE"))
  (jdbc/execute! db-spec (str "CREATE SCHEMA " schema)))

(defn list-tables-in-schema [db-spec schema]
  (jdbc/query db-spec
    (str
      "SELECT * "
      "FROM pg_catalog.pg_tables "
      "WHERE schemaname = "
      "'" schema "';")
    {:as-array true}))

(defn with-started-component [component f]
  (let [container (atom component)]
    (try
      (do
        (swap! container component/start)
        (f @container))
      (finally
        (swap! container component/stop)))))

(deftest runs-migrations-by-default-on-start
  (let [data-source {:datasource (data-source)}
        configuration {}
        schema "public"]
    (clear-schema data-source schema)
    (with-started-component
      (flyway-migrator/component
        {:configuration configuration
         :data-source   data-source})
      (fn [_]
        (let [tables (list-tables-in-schema data-source schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"users" "flyway_schema_history"})))))))

(deftest does-not-run-migrations-when-requested
  (let [data-source {:datasource (data-source)}
        configuration {:migrate-on-start false}
        schema "public"]
    (clear-schema data-source schema)
    (with-started-component
      (flyway-migrator/component
        {:configuration configuration
         :data-source   data-source})
      (fn [_]
        (let [tables (list-tables-in-schema data-source schema)]
          (is (= (count tables) 0)))))))

(deftest allows-migrations-to-be-run-on-demand
  (let [data-source {:datasource (data-source)}
        configuration {:migrate-on-start false}
        schema "public"]
    (clear-schema data-source schema)
    (with-started-component
      (flyway-migrator/component
        {:configuration configuration
         :data-source   data-source})
      (fn [component]
        (flyway-migrator/migrate component)
        (let [tables (list-tables-in-schema data-source schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"users" "flyway_schema_history"})))))))

(deftest uses-the-provided-migration-locations
  (let [data-source {:datasource (data-source)}
        configuration {:locations ["classpath:database/migrations"]}
        schema "public"]
    (clear-schema data-source schema)
    (with-started-component
      (flyway-migrator/component
        {:configuration configuration
         :data-source   data-source})
      (fn [_]
        (let [tables (list-tables-in-schema data-source schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"events" "flyway_schema_history"})))))))

(deftest uses-the-provided-migration-table
  (let [data-source {:datasource (data-source)}
        configuration {:table "migration_history"}
        schema "public"]
    (clear-schema data-source schema)
    (with-started-component
      (flyway-migrator/component
        {:configuration configuration
         :data-source   data-source})
      (fn [_]
        (let [tables (list-tables-in-schema data-source schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"users" "migration_history"})))))))

(deftest configures-component-using-default-specification
  (let [data-source {:datasource (data-source)}
        configuration
        {:locations        ["classpath:db/migrations"]
         :table            "migrations"
         :migrate-on-start false}
        component (flyway-migrator/component
                    {:data-source data-source})
        component (conf-comp/configure component
                    {:configuration-source
                     (conf/map-source configuration)})]
    (is (= configuration (:configuration component)))))

(deftest allows-specification-to-be-overridden
  (let [configuration
        {:table            "migrations"
         :migrate-on-start false}
        specification
        (conf/configuration-specification
          (conf/with-parameter :locations :default ["filesystem:db/migrations"])
          (conf/with-parameter configuration/table-parameter)
          (conf/with-parameter configuration/migrate-on-start-parameter))
        component (flyway-migrator/component
                    {:configuration-specification specification})
        component (conf-comp/configure component
                    {:configuration-source (conf/map-source configuration)})]
    (is (= {:locations        ["filesystem:db/migrations"]
            :table            "migrations"
            :migrate-on-start false}
          (:configuration component)))))

(deftest allows-default-source-to-be-provided
  (let [default-source
        (conf/map-source
          {:locations ["filesystem:db/migrations"]
           :table     "migrations"})
        configure-time-source
        (conf/map-source
          {:locations        ["classpath:db/migrations"]
           :migrate-on-start false})
        component (flyway-migrator/component
                    {:configuration-source default-source})
        component (conf-comp/configure component
                    {:configuration-source configure-time-source})]
    (is (= {:locations        ["classpath:db/migrations"]
            :table            "migrations"
            :migrate-on-start false}
          (:configuration component)))))

(deftest allows-configuration-lookup-key-to-be-provided
  (let [data-source {:datasource (data-source)}
        configuration-source
        (conf/map-source
          {:migrator-locations        ["classpath:db/migrations"]
           :migrator-table            "migrations"
           :migrator-migrate-on-start false})
        component (flyway-migrator/component
                    {:data-source data-source
                     :configuration-lookup-prefix :migrator})
        component (conf-comp/configure component
                    {:configuration-source configuration-source})]
    (is (= {:locations        ["classpath:db/migrations"]
            :table            "migrations"
            :migrate-on-start false}
          (:configuration component)))))
