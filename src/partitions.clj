(require '[datomic.api :as d])
(require '[clojure.string :as str])
(import '[java.util UUID Date])

;; Defines a partition based on a uuid or squuid
(def ^:const MAX_IMPLICIT_PARTITIONS_MASK 0x7FFFF)
(defn- implicit-part-clamp
  "clamps arg to a valid implicit partition (0 <= x < 2^19)"
  ^long [^long n]
  (bit-and n MAX_IMPLICIT_PARTITIONS_MASK))
;;; Do not change the hashing algorithms, they must be stable _across time_.
;;; Instead, create new hashing fns
(defn hash-uuid
  "hashes uuid (random or SQUUID), returning the id of an implicit partition, where
   0<=id<524288.

   WARNING
   Do not change the algorithms, they must be stable _across time_
   in order for partition assignment to be affine"
  ^long [^UUID u]
  ;; grab the 19 least significant bits of uuid, as those are random for SQUUIDs as well
  (implicit-part-clamp (.getLeastSignificantBits u)))

;; Model a database where you have an order entity with a ref for its owner the user entity
;;and for a list line-items, line-items are components of the order
(def database [#:db{:ident       :user/id
                    :valueType   :db.type/uuid
                    :cardinality :db.cardinality/one
                    :unique      :db.unique/identity}
               #:db{:ident       :order/id
                    :valueType   :db.type/uuid
                    :cardinality :db.cardinality/one
                    :unique      :db.unique/identity}
               #:db{:ident       :order/user
                    :valueType   :db.type/ref
                    :cardinality :db.cardinality/one
                    :index       true}
               #:db{:ident       :order/description
                    :valueType   :db.type/string
                    :cardinality :db.cardinality/one}
               #:db{:ident       :order/line-items
                    :valueType   :db.type/ref
                    :cardinality :db.cardinality/many
                    :isComponent true}
               #:db{:ident       :line-item/description
                    :valueType   :db.type/string
                    :cardinality :db.cardinality/one}
               #:db{:ident       :line-item/quantity
                    :valueType   :db.type/long
                    :cardinality :db.cardinality/one}])

;;generate user
(defn gen-user []
  {:user/id (d/squuid)})

(def letters (map char (concat (range 65 91) (range 97 123))))

;;generate a line-item with random description and random quantity
(defn gen-line-items []
  {:line-item/description (str/join (take 5 (repeatedly #(rand-nth letters))))
   :line-item/quantity    (long (rand-int 300))})

;;generate order receiving an user and generating a random number of line items
(defn gen-order-by-user [user-id]
  {:order/id         (d/squuid)
   :order/user       user-id
   :order/line-items (take (rand 150) (repeatedly #(gen-line-items)))})

;;Assoc an implicity partition on a given entity based on a given partition-key
(defn with-partition [entity partition-key]
  (assoc entity :db/id (d/tempid (d/implicit-part (hash-uuid partition-key)))))

(comment
  ;;create a database and transact schema; here we're NOT going to use implicit partitions
  (def db-uri "datomic:dev://localhost:4334/without-partition")

  (d/create-database db-uri)
  (def conn (d/connect db-uri))

  (d/transact conn database)

  ;;create a database and transact schema; here we're going to use implicit partitions
  (def db-uri-p "datomic:dev://localhost:4334/with-partition")

  (d/create-database db-uri-p)
  (def conn-p (d/connect db-uri-p))

  (d/transact conn-p database)

  ;; generates random users and persists in both databases, one using the default partitions
  ;; and other using implicit partitions
  ;(for [i (range 3000)]
  ;  (let [user (gen-user)]
  ;    (d/transact conn [user])
  ;    (d/transact conn-p [(with-partition user (:user/id user))])
  ;    nil))

  ;; gets the ids for all users in the database
  (def user-ids (->> (d/q '{:find  [?in]
                            :where [[?_ :user/id ?in]]}
                          (d/db conn))
                     (map first)))

  ;(def user-ids (->> (d/q '{:find  [?e]
  ;                          :where [[?e :user/id #uuid"6630525d-6f13-42ec-a9c9-bf6250c7cd05"]]}
  ;                        (d/db conn-p))
  ;                   (map first)))

  ;; generates infinit random orders and persists
  (while true
    (let [user-id (rand-nth user-ids)
          order (gen-order-by-user {:user/id user-id})]
      (d/transact conn [order])
      (d/transact conn-p [(with-partition order user-id)])
      nil))

  ;; gets the ids for all orders in the database
  (def order-ids (->> (d/q '{:find  [?in]
                             :where [[?_ :order/id ?in]]}
                           (d/db conn-p))
                      (map first)))

  ;;get a random order id
  @(def order-id (rand-nth order-ids))

  ;;pull all info of an order from the database with default partitions
  (def order (d/pull (d/db conn) '[*] [:order/id order-id] :io-context :partition/off))

  ;;the io-stats for the default partition order
  (:io-stats order)

  ;;the partition for user, order and line-items
  ;;note that they will be all equals because I'm not defining any strategy
  ;;if we're using common datomic, all users would be in the same partition
  ;;all orders in another partition and all line-items in a third partition
  ;;here I'm just printing one line-items for simplicity
  (d/part (-> order :ret :db/id))
  (d/part (-> order :ret :order/user :db/id))
  (d/part (-> order :ret :order/line-items first :db/id))

  ;;pull all info of an order from the database with implicity partitions
  ;;where the user id is the partition key
  (def order-p (d/pull (d/db conn-p) '[*] [:order/id order-id] :io-context :partition/on))

  ;;the io-stats for the implicity partition order
  (:io-stats order-p)

  ;;the partition for user, order and line-items
  ;;here if we run for different orders belonging to different users
  ;;we will see that the partition change by user
  ;;but will be the same for all orders and line-items belonging the same user
  (-> order-p :ret :db/id d/part d/implicit-part-id)
  (-> order-p :ret :order/user :db/id d/part d/implicit-part-id)
  (-> order-p :ret :order/line-items first :db/id d/part d/implicit-part-id)

  ;(d/db-stats (d/db conn-p))

  ;;get random user-id
  @(def user-id (rand-nth user-ids))

  ;;query all orders for a given user-id from the database with default partitions
  (def orders (d/query {:query '[:find (pull ?o [*])
                                 :in $ ?user-id
                                 :where [?u :user/id ?user-id]
                                 [?o :order/user ?u]]
                        :args [(d/db conn) user-id]
                        :io-context :partition/off}))

  ;;query all orders for a given user-id from the database with implicity partitions
  (def orders-p (d/query {:query '[:find (pull ?o [*])
                                   :in $ ?user-id
                                   :where [?u :user/id ?user-id]
                                   [?o :order/user ?u]]
                          :args [(d/db conn-p) user-id]
                          :io-context :partition/on}))

  ;;count and io-stats for default partitions
  (-> orders :ret count)
  (:io-stats orders)

  ;;count and io-stats for implicit partitions
  (-> orders-p :ret count)
  (:io-stats orders-p))
