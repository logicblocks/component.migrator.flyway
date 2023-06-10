(defproject io.logicblocks/component.flyway-migrator "0.1.2-RC4"
  :description "A component to run flyway migrations on system start."
  :url "https://github.com/logicblocks/component.flyway-migrator"

  :license {:name "The MIT License"
            :url  "https://opensource.org/licenses/MIT"}

  :dependencies [[org.flywaydb/flyway-core "9.19.1"]
                 [com.stuartsierra/component "1.1.0"]]

  :plugins [[lein-cloverage "1.2.4"]
            [lein-shell "0.5.0"]
            [lein-cprint "1.3.3"]
            [lein-ancient "0.7.0"]
            [lein-changelog "0.3.2"]
            [lein-eftest "0.6.0"]
            [lein-codox "0.10.8"]
            [lein-cljfmt "0.9.2"]
            [lein-kibit "0.1.8"]
            [lein-bikeshed "0.5.2"]
            [jonase/eastwood "1.4.0"]]

  :profiles
  {:shared
   ^{:pom-scope :test}
   {:dependencies [[org.clojure/clojure "1.11.1"]
                   [org.clojure/java.jdbc "0.7.12"]
                   [com.impossibl.pgjdbc-ng/pgjdbc-ng "0.8.9"]
                   [nrepl "1.0.0"]
                   [eftest "0.6.0"]]
    :resource-paths ["test_resources"]}
   :dev
   [:shared {:source-paths ["dev"]
             :eftest       {:multithread? false}}]
   :test
   [:shared {:eftest {:multithread? false}}]

   :prerelease
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["deploy"]]}

   :release
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["codox"]
     ["changelog" "release"]
     ["shell" "sed" "-E" "-i.bak" "s/\"[0-9]+\\.[0-9]+\\.[0-9]+\"/\"${:version}\"/g" "README.md"]
     ["shell" "rm" "-f" "README.md.bak"]
     ["shell" "git" "add" "."]
     ["vcs" "commit" "Release version %s [skip ci]"]
     ["vcs" "tag"]
     ["deploy"]
     ["change" "version" "leiningen.release/bump-version" "patch"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["vcs" "push"]]}}

  :cloverage
  {:ns-exclude-regex [#"^user"]}

  :codox
  {:namespaces  [#"^component\.flyway-migrator\."]
   :metadata    {:doc/format :markdown}
   :output-path "docs"
   :doc-paths   ["docs"]
   :source-uri  "https://github.com/logicblocks/component.flyway-migrator/blob/{version}/{filepath}#L{line}"}

  :cljfmt {:indents ^:replace {#".*" [[:inner 0]]}}

  :eastwood {:config-files ["config/linter.clj"]}

  :deploy-repositories
  {"releases"  {:url "https://repo.clojars.org" :creds :gpg}
   "snapshots" {:url "https://repo.clojars.org" :creds :gpg}})
