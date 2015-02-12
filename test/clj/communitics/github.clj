(ns communitics.github
  (:require [clojure.test :refer [testing is deftest]]))


(comment
  (System/setProperty "https.proxyHost", "proxy.int.world.socgen")
  (System/setProperty "https.proxyPort", "8080")
  (System/setProperty "http.proxyHost", "proxy.int.world.socgen")
  (System/setProperty "http.proxyPort", "8080")
  (System/setProperty "http.nonProxyHosts", "*.socgen"))

(testing "hey"
  (is (= "hey" "hey")))

(comment
  (with-url "https://sgithub.fr.world.socgen/api/v3/"
            (tentacles.users/user "gphilip"))

  (with-url "https://sgithub.fr.world.socgen/api/v3/"
            (tentacles.repos/all-repos)))
