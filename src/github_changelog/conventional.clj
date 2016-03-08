(ns github-changelog.conventional
  (:require [clojure.string :as string]
            [github-changelog.schema :refer [map->Change]]))

; https://help.github.com/articles/closing-issues-via-commit-messages/
(def close-keywords ["close" "closes" "closed" "fix" "fixes" "fixed" "resolve" "resolves" "resolved"])

(defn fixes-pattern
  ([pattern] (fixes-pattern pattern close-keywords))
  ([pattern closing-words]
   (re-pattern
    (format "(?i:%s) %s"
            (string/join \| closing-words)
            pattern))))

(def header-pattern #"^(\w*)(?:\((.*)\))?\: (.*)$")

(defn collect-issues [pull pattern link-fn]
  (->> (re-seq (fixes-pattern pattern) (str (:body pull)))
       (map second)
       (map #(vector % (link-fn %)))))

(defn jira-issues [{:keys [jira]} pull]
  (let [base (str jira "/browse/")]
    (collect-issues pull "\\[?([A-Z]+-\\d+)\\]?" (partial str base))))

(defn- parse-int [x] (Integer. (re-find #"[0-9]+" x)))

(defn github-issues [_ pull]
  (let [base (str (get-in pull [:base :repo :html_url]) "/issues/")]
    (collect-issues pull "(#\\d+)" #(str base (parse-int %)))))

(defn parse-issues [config pull]
  (apply concat ((juxt jira-issues github-issues) config pull)))

(defn parse-pull [config {:keys [title] :as pull}]
  (if-let [[_ type scope subject] (re-find header-pattern pull)]
    (map->Change {:type type
                  :scope scope
                  :subject subject
                  :pull-request pull
                  :issues (parse-issues config pull)})))

(defn parse-changes [config {:keys [pulls] :as tag}]
  (->> (map (partial parse-pull config) pulls)
       (remove nil?)
       (assoc tag :changes)))
