(ns communitics.github-test
  (:require [clojure.test :refer [testing is deftest]]
            [communitics.github :refer [github->datomic remove-entries-with-nil-value]]))

(deftest github->datomic-conversion
  (is (= (->> [{:html_url "http://localhost/bob"
                :gravatar_id "ac0c4949f8df628b3f77a42a3b814c4d"
                :subscriptions_url "http://localhost/api/v3/users/bob/subscriptions"}
               {:html_url "http://localhost/jane"
                :gravatar_id nil
                :followers_url "http://localhost/api/v3/users/jane/followers"
                :subscriptions_url nil}]
              (github->datomic)
              (map remove-entries-with-nil-value))
         [{:user/html_url "http://localhost/bob",
           :user/gravatar_id "ac0c4949f8df628b3f77a42a3b814c4d",
           :user/subscriptions_url "http://localhost/api/v3/users/bob/subscriptions"}
          {:user/html_url "http://localhost/jane",
           :followers_url "http://localhost/api/v3/users/jane/followers"}])))

(deftest remove-entries
  (is (= {:c "qix", :b "bar"}
         (remove-entries-with-nil-value {:a nil :b "bar" :c "qix"}))))


