(ns component.flyway-migrator.core-test
  (:require
   [clojure.test :refer :all]

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

(defn with-started-component [component f]
  (let [container (atom component)]
    (try
      (do
        (swap! container component/start)
        (f @container))
      (finally
        (swap! container component/stop)))))

(deftest runs-migrations
  (let [data-source (data-source)]
    (with-started-component (flyway-migrator/create {:data-source data-source})
      (fn [component]
        (is (= 1 1))))))
