(require '[datomic.api :as d])

(def db-uri "datomic:dev://localhost:4334/mbrainz-1968-1973")

(def conn (d/connect db-uri))
(def db (d/db conn))

;; TODO before we deep dive in more queries samples, let's see how query-stats could help you to
;; understand queries and create a more performant queries
;; Let's imagine that the next query you're seen an increase in the latency of the following query
;; Try to use query-stats to see how could you improve it
;; We know that John Lennon has only 21 releases at this range of time, it should be faster
;; Why is it taking so long??
(d/q '[:find [(pull $ ?release [*])]
       :in $ ?artist-name
       :where
       [?release :release/artists ?artist]
       [?artist :artist/name ?artist-name]
       [?release :release/name ?release-name]]
     db "John Lennon")

;; Review some basic concepts
;; Bindings
;; https://docs.datomic.com/pro/query/query.html#bindings

;; There are different shape of bindings you can use

;; ?a => scalar ;; What means scalar?
;; [?a ?b] => tuple ;; What means a tuple?
;; [?a ...] => collection
;; [[?a ?b]] => relation


;; Tuple Binding
;; It is using a tuple of bindings
;; It is useful for refer a binding where both must be present
;; Useful when asking questions like "AND"
;; i.e. What releases are associated with the artist named John Lennon and named Mind Games?
(d/q '[:find [(pull $ ?release [*])]
       :in $ [?artist-name ?release-name]
       :where [?artist :artist/name ?artist-name]
       [?release :release/artists ?artist]
       [?release :release/name ?release-name]]
     db
     ["John Lennon" "Mind Games"])

(d/query {:query '[:find (pull $ ?release
                               #_[*]
                               [:release/name :release/artistCredit :release/year {:release/script [*]}
                                {:release/media [{:medium/tracks [:track/name]}]}])
                   :in $ ?artist-name
                   :where
                   [?artist :artist/name ?artist-name]
                   [?release :release/artists ?artist]]
          :args  [db "Elis Regina"]})

;; Collection Binding
;; It requires only one of the conditions to be true
;; It is useful to ask questions like "OR"
;; i.e. what releases are associated with either Paul McCartney or George Harrison?
(d/q '[:find ?release-name
       :in $ [?artist-name ...]
       :where [?artist :artist/name ?artist-name]
       [?release :release/artists ?artist]
       [?release :release/name ?release-name]]
     db
     ["Paul McCartney" "George Harrison"])

;; Relation Binding
;; A relation binding is fully general, binding multiple variables positionallion
;; It binds to a relation (collection of tuples) passed in
;; This can be used to ask "OR" questions between AND's, involving multiple variables
;; i.e. what releases are associated with either John Lennon's Mind Games or Paul McCartney's Ram?
(d/q '[:find ?release
       :in $ [[?artist-name ?release-name]]
       :where [?artist :artist/name ?artist-name]
       [?release :release/artists ?artist]
       [?release :release/name ?release-name]]
     db
     [["John Lennon" "Mind Games"]
      ["Paul McCartney" "Ram"]])


;; Find specifications
;; https://docs.datomic.com/pro/query/query.html#find-specifications
;; Where bindings control inputs, find specifications control results
;; It means, what do you want to return?

;;find ?a ?b => relation =>  list of lists
;;find [?a ...] => collection
;;find [?a ?b] single tuple => list
;;find ?a. => single scalar

;; It is important for you control, what do you want to return in the output
;; The relation find spec is the most common, and the most general
(d/q '[:find ?artist-name ?release-name
       :in ?artist-name
       :where [?release :release/name ?release-name]
       [?release :release/artists ?artist]
       [?artist :artist/name ?artist-name]]
     db
     "elvis presley")

;; ERROR. some datomic error handling are smoth and beatiful and give you the answer :)
;; Did you forget to pass the database as argument?
;; Yes!
;; Caused by datomic.impl.Exceptions$IllegalArgumentExceptionInfo
;;    :db.error/invalid-data-source Nil or missing data source. Did you
;;    forget to pass a database argument?

;; try again, here is the relation as result
(d/q '[:find ?artist-name ?release-name
       :in $ ?artist-name                                   ;; db is represented with $ in the input
       :where [?release :release/name ?release-name]
       [?release :release/artists ?artist]
       [?artist :artist/name ?artist-name]]
     db
     "Elvis Presley")


;; The collection find spec is useful when you are only interested in a single variable
(d/q '[:find [?release-name ...]
       :in $ ?artist-name
       :where [?artist :artist/name ?artist-name]
       [?release :release/artists ?artist]
       [?release :release/name ?release-name]]
     db
     "John Lennon")

;; The single tuple find is useful when you are interested in multiple variables, but only a single value
(d/q '[:find [?year ?month ?day]
       :in $ ?name
       :where [?artist :artist/name ?name]
       [?artist :artist/startDay ?day]
       [?artist :artist/startMonth ?month]
       [?artist :artist/startYear ?year]]
     db
     "John Lennon")

;; The scalar find spec is useful when you want to return a single value of a single variable
(d/q '[:find ?year .
       :in $ ?name
       :where [?artist :artist/name ?name]
       [?artist :artist/startYear ?year]]
     db
     "John Lennon")

;; Return maps cause the query to return maps instead tuples
;; Each entry in the :keys~/:strs~/~:syms~ clause become a key mapped
;; to the corresponding item in the find clause

;; :keys => kewords keys
;; :strs => string keys
;; :syms => symbol keys

;; The :keys artist and release are used to construct a map for each row returned
(d/q '[:find ?artist-name ?release-name
       :keys artist release
       :in $ ?artist-name
       :where [?release :release/name ?release-name]
       [?release :release/artists ?artist]
       [?artist :artist/name ?artist-name]]
     db
     "Paul McCartney")

;; Return maps also preserve the order of the :find clause.
;; Implement clojure.lang.indexed
;; support nth
;; support vector style destructuring

;; positional destructure
(let [[artist release]
      (first (d/q '[:find ?artist-name ?release-name
                    :keys artist release
                    :in $ ?artist-name
                    :where [?release :release/name ?release-name]
                    [?release :release/artists ?artist]
                    [?artist :artist/name ?artist-name]]
                  db
                  "Paul McCartney"))]
  artist)

;; key destructure
(let [{:keys [artist release]}
      (first (d/q '[:find ?artist-name ?release-name
                    :keys artist release
                    :in $ ?artist-name
                    :where [?release :release/name ?release-name]
                    [?release :release/artists ?artist]
                    [?artist :artist/name ?artist-name]]
                  db
                  "Paul McCartney"))]
  artist)

(defn teste
  [?yeat]
  (< ?yeat 1600)
  )

;; Predicate expressions
;; If no bindings are provided, the function is presumed to be a predicate
;; It filter the result but not is considered a clause
(d/q '[:find ?name ?year
       :where [?artist :artist/name ?name]
       [?artist :artist/startYear ?year]
       [(user/teste ?year)]]
     db)

;; Function expressions
;; Behave similarly, except that their return values are used not as predicates
;; but to bind other variables
(d/q '[:find ?track-name ?minutes
       :in $ ?artist-name
       :where [?artist :artist/name ?artist-name]
       [?track :track/artists ?artist]
       [?track :track/duration ?millis]
       [(quot ?millis 60000) ?minutes]                      ;;built-in expression quot converts track lengths from milliseconds to minutes
       [?track :track/name ?track-name]]
     db
     "John Lennon")

;; references of other built-in expression functions and predicates see,
;; https://docs.datomic.com/pro/query/query.html#built-in-expressions

(d/q '[:find ?id ?type ?gender
       :in $ ?name
       :where
       [?e :artist/name ?name]
       [?e :artist/gid ?id]
       [?e :artist/type ?teid]
       [?teid :db/ident ?type]
       [?e :artist/gender ?geid]
       [?geid :db/ident ?gender]]
     db
     "Janis Joplin")

;; aggregations
;; (min ?xs)
;; (max ?xs)
;; (count ?xs)
;; (count-distinct ?xs)
;; (sum ?xs)
;; (avg ?xs)
;; (median ?xs)
;; (variance ?xs)
;; (stddev ?xs)

;; Min, Max.  The following query finds the smallest and largest track lengths
(d/q '[:find [(min ?dur)]
       :where [?e :track/duration ?dur]]
     db)

;; Sum. The following query uses sum to find the total number of tracks on all media in the database.
(d/q '[:find (sum ?count) .
       :with ?medium
       :where [?medium :medium/trackCount ?count]]
     db)

;; Counts. The following query uses count to report the total number of artists names
;; and count-distinct to report the total nuimber of unique artist names
(d/q '[:find (count ?name) (count-distinct ?name)
       :with ?artist
       :where [?artist :artist/name ?name]]
     db)

;; Statistics
;; Are musicians becoming more verbose when naming songs? The following query reports the median, avg, and stddev of song title lengths (in characters), and includes year in the find set to break out the results by year.

(d/q '[:find ?year (median ?namelen) (avg ?namelen) (stddev ?namelen)
       :with ?track
       :where [?track :track/name ?name]
       [(count ?name) ?namelen]
       [?medium :medium/tracks ?track]
       [?release :release/media ?medium]
       [?release :release/year ?year]]
     db)

;; Custom Aggregates
;; You may call an arbitrary Clojure function as an aggregation function as follows:
(defn mode
  [vals]
  (->> (frequencies vals)
       (sort-by (comp - second))
       ffirst))

;; With mode in hand, you can answer the question "What is the most common release medium length, in tracks?"
(d/q '[:find (user/mode ?track-count) .                     ;; reminder, you need specify the full qualified name!
       :with ?media
       :where [?media :medium/trackCount ?track-count]]
     db)


;; Ok, here we go!
;; Let's go more deeper on queries!

;; Lets start with a simple query
;; Enabled io-stats in the query and compare performance.

(d/query {:query      '[:find ?title
                        :in $ ?artist-name
                        :where
                        [?a :artist/name ?artist-name]
                        [?r :release/artists ?a]
                        [?t :track/artists ?a]
                        [?t :track/name ?title]]
          :args       [db "John Lennon"]
          :io-context :q/tracks-by-artist-name})

;; The results itself are returned as a java.util.HashSet
;; => #{["Baby's Heartbeat"] ["Nutopian International Anthem"] ["John & Yoko"] ["Tight A$"] ["Give Peace a Chance"] ["My Mummy's Dead"] ["How?"] ...

;; Executing the query multiple times, will show slower api_ms than the first time executed
;; Datomic has a local cache in each Peer, which warms up as we execute queries.

;; Most of these results are after the break up of the Beatles in 1970. Can we limit the results to only include releases before 1970?

;; Yes. Bind first the release for the artists

;; The binds are provided by the reference of EAVT index.
;; Run and, see query-stats results
(-> (d/query {:query       '[:find ?name
                             :in $ ?artist-name
                             :where
                             [?a :artist/name ?artist-name]
                             [?r :release/artists ?a]
                             [?r :release/year ?year]
                             [(< ?year 1970)]
                             [?r :release/name ?name]
                             ;;[?t :track/artists ?a]
                             ;;[?t :track/name ?title]
                             ]
              :args        [db "John Lennon"]
              ;;:io-context :q/tracks-by-artist-name
              :query-stats true})
    :query-stats)

;; Changing the clause order will be more performatic?
(-> (d/query {:query       '[:find ?name
                             :in $ ?artist-name
                             :where
                             [?a :artist/name ?artist-name]
                             [?r :release/artists ?a]
                             [?r :release/year ?year]
                             [?r :release/name ?name]
                             [(< ?year 1970)]
                             ;;[?t :track/artists ?a]
                             ;;[?t :track/name ?title]
                             ]
              :args        [db "John Lennon"]
              ;;:io-context :q/tracks-by-artist-name
              :query-stats true})
    :query-stats)

;; Bind track names from releases before 1970.
;; A return from query stats indicating bad selectivity will looks like that
;; ... [?t :track/artists ?a], :rows-in 1, :rows-out 125,  ...
;; The recommendation is that the most selectivity clause come first
;; We have data filtered in memory but it is not being considered in the followed clause.
(-> (d/query {:query       '[:find ?title
                             :in $ ?artist-name
                             :where
                             [?a :artist/name ?artist-name]
                             [?r :release/artists ?a]
                             [?r :release/year ?year]
                             [?r :release/name ?name]
                             [(< ?year 1970)]
                             [?t :track/artists ?a]
                             [?t :track/name ?title]
                             ]
              :args        [db "John Lennon"]
              ;;:io-context :q/tracks-by-artist-name
              :query-stats true})
    :query-stats)

;; Try again, checking schema relationship of references and query-stats
(-> (d/query {:query       '[:find ?title
                             :in $ ?artist-name
                             :where
                             [?a :artist/name ?artist-name]
                             [?r :release/artists ?a]
                             [?r :release/year ?year]
                             [?r :release/name ?name]       ;; Not necessary
                             [(< ?year 1970)]
                             [?r :release/media ?m]
                             [?m :medium/tracks ?t]
                             [?t :track/name ?title]
                             ]
              :args        [db "John Lennon"]
              ;;:io-context :q/tracks-by-artist-name
              :query-stats true})
    :query-stats)

;; Here we go, track from releases lower than 1970
(-> (d/query {:query       '[:find ?title
                             :in $ ?artist-name
                             :where
                             [?a :artist/name ?artist-name]
                             [?r :release/artists ?a]
                             [?r :release/year ?year]
                             [(< ?year 1970)]
                             [?r :release/media ?m]
                             [?m :medium/tracks ?t]
                             [?t :track/name ?title]
                             ]
              :args        [db "John Lennon"]
              ;;:io-context :q/tracks-by-artist-name
              :query-stats true})
    :query-stats)


;; Checked in query stats, found optimal performance.
;; Code ready for production, lets remove query stats
;; Query stats let you write query with more confidance.
;; Here is the final productive code!
(-> (d/query {:query '[:find ?title ?album ?year
                       :in $ ?artist-name
                       :where
                       [?a :artist/name ?artist-name]
                       [?r :release/artists ?a]
                       [?r :release/year ?year]
                       [?r :release/name ?album]
                       [(< ?year 1970)]
                       [?r :release/media ?m]
                       [?m :medium/tracks ?t]
                       [?t :track/name ?title]
                       ]
              :args  [db "John Lennon"]}))

;; Talking about clause order. It matters.

;; Queries and Peer Memory
;; Since queries run within the Peer with application-local memory, application designers need to consider the memory requirements for their queries.


;; Datomic's Datalog is set-oriented and eager, and does not spill large results to disk. Queries are designed to be able to run over datasets much larger than memory. However, each intermediate representation step of a query must fit into local memory. Datomic doesn't spool intermediate representations to disk like some server-based RDBMS's
;; https://docs.datomic.com/pro/query/query-executing.html#clause-order



;; Rules
;; https://docs.datomic.com/pro/query/query.html#rules
;; To reuse query logic across many queries, you can create and use rules like the following

(def track-release
  '[;; Given ?t bound to track entity-ids, binds ?r to the corresponding
    ;; set of album release entity-ids
    [(track-release ?t ?r)
     [?m :medium/tracks ?t]
     [?r :release/media ?m]]])

track-release                                               ;; pure clojure code

;; You can invoke the rules by name. It is idiomatic to use round brackets for rule invocations, and square brackets for other clauses.
(d/query {:query '[:find ?title ?album ?year
                   :in $ % ?artist-name                     ;; the rule set goes in as %
                   :where
                   [?a :artist/name ?artist-name]
                   [?t :track/artists ?a]
                   [?t :track/name ?title]
                   (track-release ?t ?r)
                   [?r :release/name ?album]
                   [?r :release/year ?year]]
          :args  [db track-release "John Lennon"]})

;; Datomic datalog allows you to package up sets of :where clauses into named rules.
;; These rules make query logic reusable, and also composable, meaning that you can bind portions
;; of a query's logic at query time.

;; A rule is a named group of clauses that can be plugged into the :where section of your query.
;; For example, here is a rule from the Seattle example dataset that tests whether a community is a twitter feed""


;; As with transactions and queries, rules are described using data structures. A rule is a list of lists. The first list in the rule is the head.


;; EXERCISE! Lets create some rules!

;; Create a rule that given a track return a complete track-info
;; To start, lets return the track-name and artist-name
;; Track name
"Yer Blues"

(def track-artist
  "Given a track it returns the bind for artist's basic info"
  '[[(track-artist ?t ?artist-name)
     [?t :track/artists ?a]
     [?a :artist/name ?artist-name]]])

(def track-release
  "Given a track it returns the bind for release's basic info"
  '[[(track-release ?t ?release-name ?release-year)         ;; Remember, it is the head clause
     [?m :medium/tracks ?t]
     [?r :release/media ?m]
     [?r :release/name ?release-name]
     [?r :release/year ?release-year]]])

(d/query {:query '[:find ?track-name ?artist-name ?release-name ?release-year
                   :in $ % ?track-name
                   :where
                   [?t :track/name ?track-name]
                   (track-artist ?t ?artist-name)
                   (track-release ?t ?release-name ?release-year)
                   ]
          :args  [db (concat track-artist
                             track-release) "Yer Blues"]})

;; It is possible also to have rules composition.

(def track-info
  "Given a track return detailed track info"
  (concat
    track-artist
    track-release
    '[[(track-info ?t ?artist-name ?release-name ?release-year)
       (track-artist ?t ?artist-name)
       (track-release ?t ?release-name ?release-year)]]))


(d/query {:query '[:find ?track-name ?artist-name ?release-name ?release-year
                   :in $ % ?track-name
                   :where
                   [?t :track/name ?track-name]
                   (track-info ?t ?artist-name ?release-name ?release-year)]
          :args  [db track-info "Yer Blues"]})


;; Pattern inputs
;; https://docs.datomic.com/pro/query/query.html#pattern-inputs

;; Pull expressions can used in a :find clause. A pull expression takes the form
;; (pull ?entity-var pattern)

;; query
;; The release entity is binded by the ?e
;; It navigates through the release entity and get the :release/name attribute
(d/query {:query '[:find (pull ?e [:release/name])
                   :in $ ?artist-name
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin"]})

;; The pull expression pattern can also be bound dynamically as an :in parameter to query
(d/query {:query '[:find (pull ?e pattern)
                   :in $ ?artist-name pattern               ;; note that there is no ? (question mark) in the name, because it refer to a symbol, see query grammar for reference
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})


;; A pull expression can only be applied to any specific ?entity-var a single time. The following forms are legal:
(d/query {:query '[:find (pull ?e [:release/name])
                   :in $ ?artist-name pattern
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})

(d/query {:query '[:find (pull ?e [:release/name]) (pull ?a [*])
                   :in $ ?artist-name pattern
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})

(d/query {:query '[:find (pull ?e [:release/name
                                   :release/artists])
                   :in $ ?artist-name pattern
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})


;; But the following expression would be invalid; ERROR
;; invalid pull expression in query
;; because you are pulling the same binding ?e two times
(d/query {:query '[:find (pull ?e [:release/name]) (pull ?e [:release/artists])
                   :in $ ?artist-name pattern
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})

;; Caused by java.lang.ArrayIndexOutOfBoundsException
;; Index 1 out of bounds for length 1

;; It is possible to navigate in chield refs using pull pattern
(d/query {:query '[:find (pull ?e [:release/name
                                   {:release/artists [*]}])
                   :in $ ?artist-name pattern
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})

;; You can add more an attribute ref
(d/query {:query '[:find (pull ?e [:release/name
                                   {:release/artists [*]}
                                   {:release/country [*]}])
                   :in $ ?artist-name pattern
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})

;; And go deeper in a specific entity
;; Use [*] to get all attributes
;; Be careful, pull [*] with no restriction can return huge volume of data
;; It can cause poor performance in queries
(d/query {:query '[:find (pull ?e [:release/name
                                   {:release/artists [*
                                                      {:artist/country [*]}]}
                                   {:release/country [*]}])
                   :in $ ?artist-name pattern
                   :where
                   [?a :artist/name ?artist-name]
                   [?e :release/artists ?a]]
          :args  [db "Led Zeppelin" [:release/name]]})
