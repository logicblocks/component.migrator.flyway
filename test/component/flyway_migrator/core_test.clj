(ns component.flyway-migrator.core-test
  (:require
   [clojure.test :refer :all]
   [clojure.java.jdbc :as jdbc]

   [com.stuartsierra.component :as component]

   [component.flyway-migrator.core :as flyway-migrator])
  (:import [com.impossibl.postgres.jdbc PGDataSource]))

(defn data-source [& {:as overrides}]
  (let [{:keys [host port user password database-name]}
        (merge
          {:host          "localhost"
           :port          5432
           :user          "admin"
           :password      "super-secret"
           :database-name "some-database"}
          overrides)]
    (doto (PGDataSource.)
      (.setHost host)
      (.setPort port)
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
  (let [data-source (data-source)
        configuration {}
        db-spec {:datasource data-source}
        schema "public"]
    (clear-schema db-spec schema)
    (with-started-component
      (flyway-migrator/create {:data-source data-source
                               :configuration configuration})
      (fn [_]
        (let [tables (list-tables-in-schema db-spec schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"users" "flyway_schema_history"})))))))

(deftest does-not-run-migrations-when-requested
  (let [data-source (data-source)
        configuration {:migrate-on-start false}
        db-spec {:datasource data-source}
        schema "public"]
    (clear-schema db-spec schema)
    (with-started-component
      (flyway-migrator/create {:data-source data-source
                               :configuration configuration})
      (fn [_]
        (let [tables (list-tables-in-schema db-spec schema)]
          (is (= (count tables) 0)))))))

(deftest allows-migrations-to-be-run-on-demand
  (let [data-source (data-source)
        configuration {:migrate-on-start false}
        db-spec {:datasource data-source}
        schema "public"]
    (clear-schema db-spec schema)
    (with-started-component
      (flyway-migrator/create {:data-source data-source
                               :configuration configuration})
      (fn [component]
        (flyway-migrator/migrate component)
        (let [tables (list-tables-in-schema db-spec schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"users" "flyway_schema_history"})))))))

(deftest uses-the-provided-migration-locations
  (let [data-source (data-source)
        configuration {:locations ["classpath:database/migrations"]}
        db-spec {:datasource data-source}
        schema "public"]
    (clear-schema db-spec schema)
    (with-started-component
      (flyway-migrator/create {:data-source data-source
                               :configuration configuration})
      (fn [_]
        (let [tables (list-tables-in-schema db-spec schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"events" "flyway_schema_history"})))))))

(deftest uses-the-provided-migration-table
  (let [data-source (data-source)
        configuration {:table "migration_history"}
        db-spec {:datasource data-source}
        schema "public"]
    (clear-schema db-spec schema)
    (with-started-component
      (flyway-migrator/create {:data-source data-source
                               :configuration configuration})
      (fn [_]
        (let [tables (list-tables-in-schema db-spec schema)]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"users" "migration_history"})))))))
