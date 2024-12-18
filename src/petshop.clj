(ns petshop
  (:require
   [clojure.pprint :as pprint]
   [datomic.api :as d]))

;; Before you start;
;; - TODO comment means that is something you need to complete.
;; - INFO it's and informative message.
;; - hint it's a guidance message.
;; - `change-me` means expected to change it to the correct value.

;; don't worry, we're here for you, just raise your hand and we'll help ;)

;; =============== Workshop 1 ===============

;; To download and set up a transactor follow the tutorial: https://docs.datomic.com/setup/pro-setup.html
;; TLDR; In a terminal form the Datomic Distribution run:
;; ```$ bin/transactor config/samples/dev-transactor-template.properties ```
;; A datomic peer dynamically get a connection.
;; It can connect in multiple simutaneously connections
;; It only changes the db uri for connection

;; Defines a name for the database, in this case pet-shop
(def db-uri "datomic:dev://localhost:4334/pet-shop")

;; INFO: Creates the database, if it does not exist returns false
(d/create-database db-uri) ;; it requires a running transactor

;; INFO: to delete a database use `d/delete-database`
(comment (d/delete-database db-uri))

;; Connect to the database
(def conn (d/connect db-uri))

;; INFO: schema is plain data: https://docs.datomic.com/schema/schema-reference.html

;;TODO:
;; 1. Define the `:owner/pets` schema, it should be a ref to another entity.
;;    INFO: In datomic the notion of referencing to a specific entity doesn't exists, it's up to the application to decide that.
;; 2. Make `:pet/id` a unique identity.
;; 3. Define the enumerations for  `:pet/type` (:pet.type/dog, :pet.type/other)

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
             {:db/ident       :owner/pets
              :db/valueType   :change-me ;; TODO: 1, hint: what is the valueType for a ref? https://docs.datomic.com/schema/schema-reference.html#db-valuetype
              :db/change-me   :db.cardinality/change-me}
             {:db/ident       :pet/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :change-me} ;; TODO: 2, hint: what is the value to define it as unique? https://docs.datomic.com/schema/schema-reference.html#db-unique
             {:db/ident       :pet/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :pet/type
              :db/valueType   :db.type/ref ;; INFO: It's a ref to what? To any entity that exists, it's up to our application to reference to the correct enum.
              :db/cardinality :db.cardinality/one}
             {:db/ident :pet.type/cat}]) ;; TODO 3, :pet.type/dog, :pet.type/other need to be added. https://docs.datomic.com/schema/schema-modeling.html#enums

;; We can proceed to transact it to the database. Yes, it's like any other transaction.
@(d/transact conn schema)

;; Let's double check that it was installed properly.
(d/q '[:find (pull ?e [*])
       :in $ ?attribute
       :where [?e :db/ident ?attribute]] (d/db conn) :owner/pets) ;; Try more attributes (`:pet/name`, `:owner/email`) and query them.

;; INFO: Use squuid instead of a random uuid for uniqueness checks: https://docs.datomic.com/schema/identity.html#squuids
;; Create a UUID for our owner entity, notice I'm using a squuid instead of a random uuid,
(def owner-id (d/squuid))

;; TODO: Add an owner using a EntityMaps with one Pet
;; INFO: https://docs.datomic.com/schema/schema-reference.html#db-cardinality
@(d/transact conn [{:owner/id   owner-id
                    :owner/name "Rana"
                    :owner/pets [{:db/id    "F" ;; INFO: What is this? You can learn more about it here: https://docs.datomic.com/transactions/transaction-data-reference.html#tempids
                                  :pet/id   :change-me
                                  :pet/name "Floki"
                                  :pet/type :change-me}]}]) ;; Hint: :pet/type has different enums available (:pet.type/dog, :pet.type/cat)

;; As you can see you can transact nested entities!
;; Also notice I'm not adding an email here there is no check for required data

;; TODO: Add another cat to :owner/pets
(def bjorn-id (d/squuid))

;; INFO: The return of the transaction info: https://docs.datomic.com/transactions/transaction-processing.html#monitoring-transactions
(def tx-return @(d/transact conn [{:owner/id   :change-me
                                   :owner/pets [{:db/id    "B"
                                                 :pet/id   :change-me
                                                 :pet/name "Bjorn"
                                                 :pet/type :change-me}]}]))

;; See what's inside
(pprint/pprint tx-return)

;;
;; INFO: When updating an attribute of cardinality one of an existing entity,
;;       it will implicitly create two assertions one to retract the old value and one to add the new one
;;       https://docs.datomic.com/client-tutorial/retract.html#implicit-retract
;; TODO Hannah's name is wrong; lets create a new transaction to fix it
@(d/transact conn [[:db/add [:change-me :change-me] :owner/name "Hanna"]])

;; Let's see the datoms about the created entities on my database, don't worry about understanding this query now

(d/q '[:find ?e ?a ?v ?t ?op
       :where (or [?e :owner/id]
                  [?e :pet/id])
       [?e ?a ?v ?t ?op]]
     (d/db conn))

;; =============== Workshop 2 ===============

;; Well I got a new cat, Poti! Lets persist this new cat using the list form
;; TODO Add Poti and reference it to the owner
@(d/transact conn [[:db/add "P" :pet/id (d/squuid)]
                   [:db/add "P" :pet/name "Poti"]
                   [:db/add "P" :pet/type :pet.type/cat]
                   [:db/add [:owner/id owner-id] :owner/pets "change-me"]]) ;; INFO: https://docs.datomic.com/schema/identity.html#lookup-refs

;; INFO: This is pull, another API we can use to query our data.
;;       https://docs.datomic.com/query/query-pull.html
;; We are showing the name of the owner and the name of all the cats
(d/pull (d/db conn) [:db/id :owner/name :owner/email {:owner/pets [:pet/name]}] [:owner/id owner-id])

;; TODO Add an email on Hanna's entity, you can use the list form or the map form
@(d/transact conn [{:owner/id .....}])

;; INFO: Remember we said that schemas are also entities, we can query them using pull.

(d/q '{:find  [(pull ?e [* {:db/valueType          [:db/ident]
                            :db/cardinality        [:db/ident]
                            :db.install/_attribute [:db/ident]}])]
       :in    [$ ?schema]
       :where [[?e :db/ident ?schema]]}
     (d/db conn) :pet/name)

;; INFO: You can add additional attributes to a transaction entity to capture other useful information, such as the purpose of the transaction,
;;       the application that executed it, the provenance of the data it added, or the user who caused it to execute,
;;       or any other information that might be useful for auditing purposes.
;;
;;       Datomic will resolve the reserved tempid "datomic.tx" to the current transaction.
;;       You can use this tempid to make assertions about the current transaction as you would for any other entity.
;;       https://docs.datomic.com/transactions/transaction-data-reference.html#reified-txes

;; TODO add a new attribute called :audit/email with dataType string.
(def audit-schema [{:db/ident :audit/email ......}]) ;; hint: remember to add :db/cardinality and :db/valueType

@(d/transact conn audit-schema)
(def fiona-id (d/squuid))
@(d/transact conn [{:db/id       "datomic.tx"
                    :audit/email "aldo@email.com"}
                   {:owner/id   owner-id
                    :owner/pets [{:db/id    "F"
                                  :pet/id   fiona-id
                                  :pet/name "Fiona"
                                  :pet/type :pet.type/cat}]}])

;; Here we get the transaction entity when the datom pet-id fiona was transacted
(d/q '[:find (pull ?tx [*])
       :in $ ?pet-id
       :where [?_ :pet/id ?pet-id ?tx]]
     (d/db conn) fiona-id)

;; In fact, there was an error and Fiona's entity should not be created
;; INFO: instead of removing datom by datom we can call transaction function :retractEntity
;;       https://docs.datomic.com/transactions/transaction-functions.html#dbfn-retractentity

(d/transact conn [[:db/retractEntity [:pet/id fiona-id]]])

;; INFO: Want to learn even more about Datomic?
;;       You can study more about all the options to create a schema,
;;       we have some interesting options as :db/Components, :db/noHistory, :db/index, and so on
;;       https://docs.datomic.com/schema/schema-reference.html

;;       You can also create attributes that are tuples!
;;       Learn more about it here: https://docs.datomic.com/schema/schema-reference.html#tuples

;;       What about creating your on transaction function?
;;       You could create a function that will add to the owner entity the count of
;;       pets their owns every time a new pet is created
;;       https://docs.datomic.com/transactions/transaction-functions.html

;; =============== Workshop 3 ===============

;; In thish workshop we will focus on the time model, querying at different points in time.
;;
(def db (d/db conn))

;; Database view in the current state, everything that is true in the op [e a v t OP]
(d/q '[:find (pull ?e [*])
       :where [?e :owner/id owner-id]]
     db)

;; TODO Get the tx of the entity `owner-id`

(def as-of-tx (first (d/q '[:find [?tx]
                            :where [?e :owner/id owner-id]] ;; hint: [e a v tx]
                          db)))

;; INFO: `d/as-of`, view until a defined point on time
(d/q '[:find (pull ?e [*])
       :where [?e :owner/id owner-id]]
     (d/as-of db as-of-tx))

;; INFO: `d/since`, Database view since a defined point in time
;; TODO Why is returning nothing?
(d/q '[:find (pull ?e [* {:owner/pets [*]}])
       :where [?e :owner/id owner-id]]
     (d/since db as-of-tx))

;; INFO: `d/with` will simulate a transaction without really transact the data
(def db-with-misha (d/with db [{:owner/id   owner-id
                                :owner/pets [{:db/id    "M" ;; Explain better tempids generation when creating a new entity in a ref
                                              :pet/id   (d/squiid)
                                              :pet/name "Misha"
                                              :pet/type :pet.type/cat}]}]))

;; TODO The query doesn't return Misha, why?
(d/q '[:find (pull ?e [* {:owner/pets [*]}])
       :where [?e :owner/id owner-id]]
     (d/db conn))
