## CONJ 2024 Day of Datomic

The Amazing Day of Datomic project is a introductory presentation
for learning [Datomic](http://datomic.com).
<br>

## Getting Started

> ### Download 
Get Datomic Pro

Datomic Pro is distributed as a zip. You can download the latest version of Datomic Pro here or with this curl command:

```curl https://datomic-pro-downloads.s3.amazonaws.com/1.0.7260/datomic-pro-1.0.7260.zip -O```

or click to download [here](https://datomic-pro-downloads.s3.amazonaws.com/1.0.7260/datomic-pro-1.0.7260.zip).

  
After you download Datomic, unzip it locally. 
```unzip  <your-download-folder>/datomic-pro-1.0.7260.zip```

<br>

> ### Run a Transactor
For this tutorial, you will run a *dev mode* transactor on your local machine. 
Dev storage will persist your data by using local disk files for storage. 
It requires a transactor to be running.

This guide will use the config directory in your datomic-pro distribution directory:

```cp config/samples/dev-transactor-template.properties config/dev-transactor-template.properties```


<br>

> ### Starting a Transactor
From your shell system, run (at the folder you unziped your transactor):

```bin/transactor config/dev-transactor-template.properties```



<br>

> ### Running a REPL

You can use your prefered IDE, but if you don't have the enviroment set up in your machine you can use Datomic REPL distribuition optionally:

With the transactor running, open another terminal tab and in config directory in your datomic-pro distribution you can use the command below to run a REPL

```bin/repl```

<br>

> ### Connecting to a Database
In the workshop code you will find the connection instructions.

<br>

### Database Sample
To get a sample database follow the tutorial below using only the *Getting the Data* section
[mbrainz-sample](https://github.com/Datomic/mbrainz-sample)
* Run the commands in a separate terminal tab too.


<br>
<br>


## Questions, Feedback?

For specific feedback on the tutorials, please create an
[issue](https://github.com/Datomic/day-of-datomic/issues).

For questions about Datomic, try the [public mailing
list](http://groups.google.com/group/datomic).
