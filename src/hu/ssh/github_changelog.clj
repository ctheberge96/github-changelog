(ns hu.ssh.github-changelog
  (:require
    [tentacles.core :as core]
    [tentacles.repos :as repos]
    [tentacles.pulls :as pulls]
    [clj-semver.core :as semver]))

(defn repo
  "Gets the repository from its name"
  [name]
  (vector "pro" name))

(defn parse-semver
  "Checks for semantic versions with or without v predicate"
  [tag]
  (let [version (:name tag)
        parse #(try (semver/parse %)
                    (catch java.lang.AssertionError _e nil))]
    (if (= \v (first version))
      (parse (apply str (rest version)))
      (parse version))))

(defn changelog
  "Fetches the changelog"
  [user repo]
  (let [tags (map #(assoc % :version (parse-semver %)) (repos/tags user repo))
        pulls (pulls/pulls user repo {:state "closed"})
        commits (repos/commits user repo)]
    (println (first tags))))

(core/with-defaults {:oauth-token "9437b011403cfa07714ef2269a62a3ce211b4b5d" :all_pages true}
                    (changelog "raszi" "node-tmp"))