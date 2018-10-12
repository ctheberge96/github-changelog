(ns github-changelog.cli
  (:gen-class)
  (:require [clojure
             [edn :as edn]
             [string :as str]]
            [clojure.tools.cli :as cli]
            [github-changelog.core :as core]
            [github-changelog.formatters.markdown :as md]))

(def cli-options
  [["-l" "--last LAST" "Generate changes only for the last n tags"
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be a positive number"]]
   ["-s" "--since TAG", "Generate changes only after TAG tag"]
   ["-u" "--until TAG", "Generate changes only until (including) TAG tag"]
   ["-c" "--collapse-tags", "Do not include tags, only group by change type"]
   ["-h" "--help"]])

(defn- join-lines [lines]
  (str/join \newline (flatten lines)))

(defn- exit [status msg]
  (when (seq msg)
    (println msg))
  (System/exit status))

(defn- error-msg [errors]
  (join-lines ["The following errors occurred while parsing your command:"
               ""
               errors]))

(defn- read-config [file]
  (-> (if (= file "-") *in* file)
      (slurp)
      (edn/read-string)))

(defn- generate [file options]
  (let [all-tags (core/changelog (read-config file))
        tags     (core/filter-tags all-tags options)]
    (md/format-tags tags options)))

(defn- usage [options-summary]
  (join-lines ["Usage: program-name [options] <config.edn>"
               ""
               "Options:"
               options-summary]))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)
        {:keys [help]}                             options
        config-file                                (first arguments)]
    (cond
      help               (exit 0 (usage summary))
      (empty? arguments) (exit 1 (usage summary))
      errors             (exit 1 (error-msg errors)))

    (println (generate config-file options))

    (exit 0 nil)))
