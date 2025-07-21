# Amazing Day of Datomic

Datomic is a distributed, general-purpose database that provides immutability, time-aware data access, and strong ACID guarantees.
The **Amazing Day of Datomic** is an interactive workshop designed to teach you these fundamentals and help you get hands-on experience with Datomic.

[📽️ Presentation Slides](https://docs.google.com/presentation/d/1OagiOnZf4vPinEvLs8rV9n3Cy2g7IC9rUMhaGQSE7qM/edit?slide=id.g2d4ad46bc0e_2_0#slide=id.g2d4ad46bc0e_2_0)

---

## 📑 Table of Contents

1. [About](#about)
2. [Getting Started](#getting-started)
    - [Install Clojure](#install-clojure)
    - [Download Datomic Pro](#download-datomic-pro)
    - [Run a Transactor](#run-a-transactor)
    - [Running a REPL](#running-a-repl)
    - [Running in Editors](#running-in-editors)
3. [Connecting to a Database](#connecting-to-a-database)
4. [Database Sample](#database-sample)
5. [Learn More](#learn-more)
6. [Questions & Feedback](#questions--feedback)

---

## What You’ll Find in This Repo

Inside the `src` directory, you'll find three Clojure files:

- **petshop.clj** – The main file used in the workshop. It walks through basic Datomic operations such as creating a database, performing transactions, and running queries.
- **music_brainz.clj** – Contains more complex query examples. To run it, you'll need to restore the MusicBrainz sample database. See how to do this in the [Database Sample](#database-sample) section.
- **partitions.clj** – Demonstrates how to use [implicit partitions](https://docs.datomic.com/transactions/partitions.html#implicit-partitions) in Datomic and discusses the performance implications.

---

## Getting Started

### Install Clojure

Follow the official instructions to install Clojure for your operating system:

https://clojure.org/guides/install_clojure

You'll also need to have Java installed.

---

### Download Datomic Pro

Download Datomic Pro from:

https://docs.datomic.com/setup/pro-setup.html#get-datomic

---

### Run a Transactor

For this workshop, you’ll run a **dev mode** transactor locally.

Dev mode uses local disk storage and requires a transactor process to be running:

https://docs.datomic.com/setup/pro-setup.html#starting-a-transactor

---

### Running a REPL

You can use your preferred IDE or run the Datomic REPL directly.

If you don’t have a development environment set up, Datomic provides a REPL distribution.

With the transactor running, open another terminal tab, navigate to the `config` directory of your Datomic Pro distribution, and run:

```bash
bin/repl
```

---

### Running in Editors

You can also run the examples in your favorite editor:

[VSCode + Calva](https://calva.io/getting-started/)

[IntelliJ + Cursive](https://cursive-ide.com/userguide/index.html)

[Emacs + CIDER](https://docs.cider.mx/cider/basics/installation.html)

---

### Connecting to a Database

Here you can learn how to connect to a database:

https://docs.datomic.com/peer-tutorial/connect-to-a-database.html

Connection instructions are also included in the workshop source code.

---

### Database Sample

To use a sample database, follow the tutorial below using only the *Getting the Data* section of the mbrainz-sample repository.

[mbrainz-sample](https://github.com/Datomic/mbrainz-sample)
* Run the commands in a separate terminal tab too.

---

## Learn more

### Datomic

* [Datomic Documentation](https://docs.datomic.com/datomic-overview.html) – Official Datomic documentation.
* [Learn Datalog Today](https://www.learndatalogtoday.org) – an interactive tutorial on Datalog query language the one used by Datomic
* [Max Datom](https://max-datom.com) – An interactive Datomic tutorial with exercises and visualizations.
* [Learn by Example](https://docs.datomic.com/resources/learn-by-example.html) – Build a simple TODO app using Datomic to learn by doing.
* [Original Day of Datomic Repository](https://github.com/Datomic/day-of-datomic) – The original Day of Datomic repo, with additional examples and exercises.
* [Original Day of Datomic Video Recording](https://www.youtube.com/watch?v=yWdfhQ4_Yfw&t=31s) – Recording of the original Day of Datomic presentation.

---

### Clojure 

* [Clojure Official Website](https://clojure.org) – The official Clojure language website.
* [4Clojure](https://4clojure.oxal.org) – Solve small coding problems interactively to practice Clojure.
* [Try Clojure](https://tryclojure.org) – An interactive REPL to learn the basics of Clojure online.
* [Brave Clojure](https://www.braveclojure.com) – A free, beginner-friendly online book to learn Clojure, with practical examples and explanations.

## Questions, Feedback?

For questions or feedback about the tutorials, please create an
[issue](https://github.com/Datomic/day-of-datomic-conj/issues).
