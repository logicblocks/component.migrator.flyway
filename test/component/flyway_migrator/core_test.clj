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
        db-spec {:datasource data-source}]
    (clear-schema db-spec "public")
    (with-started-component
      (flyway-migrator/create {:data-source data-source})
      (fn [_]
        (let [tables
              (jdbc/query db-spec
                (str
                  "SELECT * "
                  "FROM pg_catalog.pg_tables "
                  "WHERE schemaname = 'public';")
                {:as-array true})]
          (is (= (count tables) 2))
          (is (= (set (map :tablename tables))
                #{"users" "flyway_schema_history"})))))))
