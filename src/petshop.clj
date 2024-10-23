(require '[datomic.api :as d])

;; =============== Workshop 1 ===============

;; To download and set up a transactor follow the tutorial: https://docs.datomic.com/setup/pro-setup.html

;; A datomic peer dynamically get a connection.
;; It can connect in multiple simutaneously connections
;; It only changes the db uri for connection

;; ddb, cassandra, other protocols just require change the db-uri: https://docs.datomic.com/operation/storage.html
;; Also defines a name for the database, in this case pet-shop
(def db-uri "datomic:dev://localhost:4334/pet-shop")        ;; it requires a running transactor

; Creates the database if it does not exist
(d/create-database db-uri)

;; Connect to it
(def conn (d/connect db-uri))


;; schema is plain data: https://docs.datomic.com/schema/schema-reference.html
(def schema [{:db/ident       :owner/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity}
             {:db/ident       :owner/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :owner/email
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             ;; TODO Define the :owner/pets schema, remember it is a relation
             {:db/ident       :pet/id                       ;; TODO I want to this to be a unique identity, what is missing?
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one}
             {:db/ident       :pet/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :pet/type                     ;; TODO Define the enums for the pet type: :pet.type/cat, :pet.type/dog, :pet.type/other
              :db/valueType   :db.type/ref                  ;; It's a ref to what? To any entity that exists, it's up to our application to create the correct boundaries.
              :db/cardinality :db.cardinality/one}])

;; and is installed as any other transaction
@(d/transact conn schema)

;; Let's create a UUID for our owner entity, notice I'm using a squuid instead of a random uuid,
;; they are preferable over uuids for uniqueness checks: https://docs.datomic.com/schema/identity.html#squuids
(def owner-id (d/squuid))

(def floki-id (d/squuid))

;; Let's add an owner using a EntityMaps with one Pet
;; As you can see you can transact nested entities!
;; Also notice I'm not adding an email here there is no check for required data
@(d/transact conn [{:owner/id   owner-id
                    :owner/name "Rana"
                    :owner/pets [{:db/id    "F"             ;; TODO What is this? You can learn more about it here: https://docs.datomic.com/transactions/transaction-data-reference.html#tempids
                                  :pet/id   floki-id
                                  :pet/name "Floki"
                                  :pet/type :pet.type/cat}]}])

(def bjorn-id (d/squuid))

;; The return of the transaction info
(def tx-return @(d/transact conn [{:owner/id   owner-id
                                   :owner/pets [{:db/id    "B"
                                                 :pet/id   bjorn-id
                                                 :pet/name "Bjorn"
                                                 :pet/type :pet.type/cat}]}]))

;; You can see what's inside
(clojure.pprint/pprint tx-return)

;; My name is wrong; lets create a new transaction to fix it
;; When updating an attribute of cardinality one of an existing entity,
;; it will implicitly create two assertions one to retract the old value and one to add the new one
;; https://docs.datomic.com/client-tutorial/retract.html#implicit-retract
@(d/transact conn [[:db/add [:owner/id owner-id] :owner/name "Hanna"]])

(d/db conn)

;; Let's see the datoms about the created entities on my database, don't worry about understanding this query now
(d/q '[:find ?e ?a ?v ?t ?op
       :where (or [?e :owner/id]
                  [?e :pet/id])
       [?e ?a ?v ?t ?op]]
     (d/db conn))

;; =============== Workshop 2 ===============

;; Well I got a new cat, Poti! Lets persist this new cat using the list form
;; TODO There is something missing here, how can I assert that Poti is my cat?
@(d/transact conn [[:db/add "P" :pet/id (random-uuid)]
                   [:db/add "P" :pet/name "Poti"]
                   [:db/add "P" :pet/type :pet.type/cat]
                   [:db/add ??? :owner/pets ???]])

;; This is pull, another API we can use to query our data
;; Here we're showing the name of the owner and the name of all the cats
;; Don't worry about understanding it for now
(d/pull (d/db conn) [:db/id :owner/name :owner/email {:owner/pets [:pet/name]}] [:owner/id owner-id])

;; TODO Add an email on Hanna's entity, you can use the list form or the map form
;; Don't forget to use some identity so datomic can identify the entity
@(d/transact conn [{:owner/id    owner-id
                    :owner/email "hanna@gmail.com"}])

;; Remember we said that schemas are also entities? We can also query then
(d/q '{:find  [(pull ?e [* {:db/valueType          [:db/ident]
                            :db/cardinality        [:db/ident]
                            :db.install/_attribute [:db/ident]}])]
       :in    [$ ?schema]
       :where [[?e :db/ident ?schema]]}
     (d/db conn) :pet/name)

;; And finally transactions are entities and we can add extra datoms to it
;; TODO lest add a new attribute called :audit/email
(def schema [])

@(d/transact conn schema)

(def fiona-id (d/squuid))

;; To add extra information on a transaction we need to add a reserved tempid "datomic.tx"
@(d/transact conn [{:db/id       "datomic.tx"
                    :audit/email "hanna@email.com"}
                   {:owner/id   owner-id
                    :owner/pets [{:db/id    "F"
                                  :pet/id   fiona-id
                                  :pet/name "Fiona"
                                  :pet/type :pet.type/cat}]}])

;; Here we will get the transaction entity of the time the datom pet-id fiona was transacted
(d/q '[:find (pull ?t [*])
       :in $ ?pet-id
       :where [?_ :pet/id ?pet-id ?t]]
     (d/db conn) fiona-id)

;; But in fact, there was an error and Fiona's entity should not be created
;; Instead of removing datom by datom we can call transaction function :retractEntity
;;
(d/transact conn [[:db/retractEntity [:pet/id fiona-id]]])

;; TODO Want to learn even more about Datomic?
;; You can study more about all the options to create a schema,
;; we have some interesting options as :db/Components, :db/noHistory, :db/index, and so on
;; https://docs.datomic.com/schema/schema-reference.html

;; You can also create attributes that are tuples!
;; Learn more about it here: https://docs.datomic.com/schema/schema-reference.html#tuples

;; What about creating your on transaction function?
;; You could create a function that will add to the owner entity the count of
;; pets their owns every time a new pet is created
;; https://docs.datomic.com/transactions/transaction-functions.html

;; =============== Workshop 3 ===============

;; Database view in the current state
(d/q '[:find (pull ?e [*])
       :where [?e :owner/id owner-id]]
     (d/db conn))

;; Here I'm getting the instant time of when the entity id #uuid"63298959-0f4c-4edb-afef-44eda878ac48"
;; was created
(def as-of-tx (first (d/q '[:find [?tx]
                            :where [?_ :owner/id #uuid"63298959-0f4c-4edb-afef-44eda878ac48" ?tx]] ;; -> [e a v t] Here we access the t of the datom.
                          (d/db conn))))

(d/query {:query      '[:find (pull ?cat [*])
                        :where [?cat :pet/name ?cat-name]
                        [(>= ?cat-name "F")]
                        [(< ?cat-name "G")]]
          :args       [(d/db conn)]
          :io-context :query/cat-by-name-starting-with-f})

(d/pull (d/db conn) '[*] as-of-tx)

(d/tx->t as-of-tx)

; time -> tx instant time
; tx -> transaction entity? Also the counter, the counter is extracted from the tx entity id
; t -> Counter always increases, not necessary by one

;; As-of: Database view until a defined point on time
(d/q '[:find (pull ?e [*])
       :where [?e :owner/id #uuid"63298959-0f4c-4edb-afef-44eda878ac48"]]
     (d/as-of (d/db conn) as-of-tx))

;; Since: Database view since a defined point in time
;; TODO Why is returning nothing?
(d/q '[:find (pull ?e [* {:owner/pets [*]}])
       :where [?e :owner/id #uuid"63298959-0f4c-4edb-afef-44eda878ac48"]]
     (d/since (d/db conn) as-of-tx))

;; d with will simulate a transaction without really transact the data
(def db-with-misha (d/with (d/db conn) [{:owner/id   owner-id
                                         :owner/pets [{:db/id    "M" ;; Arrumar tempid
                                                       :pet/id   (random-uuid)
                                                       :pet/name "Misha"
                                                       :pet/type :pet.type/cat}]}]))

;; TODO There is no Misha yet, why? How how to fix it?
(d/q '[:find (pull ?e [* {:owner/pets [*]}])
       :where [?e :owner/id #uuid"63298959-0f4c-4edb-afef-44eda878ac48"]]
     (d/db conn))