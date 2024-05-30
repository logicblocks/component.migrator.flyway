(ns component.migrator.flyway.api
  (:require
   [clojure.string :as str]
   [pathological.paths :as pp]
   [pathological.files :as pf])
  (:import
   [java.net URI]
   [java.nio.charset StandardCharsets]
   [java.nio.file FileSystems Path]
   [java.util Map]
   [org.flywaydb.core Flyway]
   [org.flywaydb.core.api Location ResourceProvider]
   [org.flywaydb.core.internal.resource.classpath ClassPathResource]))

(defn running-in-native-image? []
  (not (nil? (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(deftype GraalVMResourceProvider [locations]
         ResourceProvider
         (getResource [provider name]
           (let [class-loader (.getClassLoader (.getClass provider))]
             (when-not (nil? (.getResource class-loader name))
               (ClassPathResource.
                 nil name class-loader StandardCharsets/UTF_8))))

         (getResources [provider prefix suffixes]
           (with-open [fs (FileSystems/newFileSystem
                            (URI/create "resource:/")
                            (Map/of))]
             (let [class-loader (.getClassLoader (.getClass provider))
                   suffixes (vec suffixes)
                   paths
                   (map
                     (fn [^Location location]
                       (pp/path fs (.getPath location)))
                     locations)]
               (reduce
                 (fn [acc path]
                   (into acc
                     (pf/walk-file-tree path
                       :initial-value []
                       :visit-file-fn
                       (fn [files file attributes]
                         (let [file-name-string (str (pp/file-name file))]
                           {:control :continue
                            :result
                            (if (and (:regular-file? attributes)
                                  (str/starts-with? file-name-string prefix)
                                  (some
                                    (fn [suffix]
                                      (str/ends-with? file-name-string suffix))
                                    suffixes))
                              (conj files
                                (ClassPathResource.
                                  nil (str file) class-loader
                                  StandardCharsets/UTF_8))
                              files)})))))
                 []
                 paths)))))

(defn instance [{:keys [data-source locations table]}]
  (let [configuration (Flyway/configure)
        configuration (.dataSource configuration data-source)
        configuration (if-not (nil? locations)
                        (.locations configuration
                          ^"[Ljava.lang.String;" (into-array String locations))
                        configuration)
        configuration (if (running-in-native-image?)
                        (.resourceProvider configuration
                          (GraalVMResourceProvider.
                            (vec (.getLocations configuration))))
                        configuration)
        configuration (if-not (nil? table)
                        (.table configuration table)
                        configuration)]
    (.load configuration)))

(defn migrate [^Flyway flyway]
  (.migrate flyway))
