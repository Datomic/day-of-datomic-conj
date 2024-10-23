## CONJ 2024 Day of Datomic

The Amazing Day of Datomic project is a introductory presentation
for learning [Datomic](http://datomic.com).

## Getting Started

### Download 

Get Datomic Pro
Datomic Pro is distributed as a zip. You can download the latest version of Datomic Pro here or with this curl command:

```curl https://datomic-pro-downloads.s3.amazonaws.com/1.0.7260/datomic-pro-1.0.7260.zip -O```

or click to download [here](https://datomic-pro-downloads.s3.amazonaws.com/1.0.7260/datomic-pro-1.0.7260.zip).

  
After you download Datomic, unzip it locally. 
```unzip  <your-download-folder>/datomic-pro-1.0.7260.zip```

### Run a Transactor
For this tutorial, you will run a *dev mode* transactor on your local machine. 

Dev storage will persist your data by using local disk files for storage. 

It requires a transactor to be running.

This guide will use the config directory in your datomic-pro distribution directory:

```cp config/samples/dev-transactor-template.properties <your-download-folder>/config/dev-transactor-template.properties```

### Starting a Transactor
From your shell system, run (at the folder you unziped your transactor):

```bin/transactor config/dev-transactor-template.properties```

### Connecting to a Database
At the workshop code you will find the connection instructions.

### Database Sample
To get a sample database follow the tutorial below starting from the *Getting the Data* section
[mbrainz-sample](https://github.com/Datomic/mbrainz-sample)

## Questions, Feedback?

For specific feedback on the tutorials, please create an
[issue](https://github.com/Datomic/day-of-datomic/issues).

For questions about Datomic, try the [public mailing
list](http://groups.google.com/group/datomic).
