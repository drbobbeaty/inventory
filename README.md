# inventory - a Simple Clojure web App

There are a lot of examples of little things in the Clojure world, but a simple,
but complete app seemed to be missing as an example for friends. So because they
asked for this, we have created a simple inventory application based on Clojure
with a front-end of Bootstrap and [Handsontable](http://handsontable.com) so
that it can serve as a simple exercise, but with all the configuration and database
use that a larger application would need.

<center>
  <img src="docs/img/forklift.jpg" width="250" height="250" border="0" />
</center>

## Deployment

The _intended_ deployment scheme is not yet set, but it runs just fine on a laptop.

## Development Notes

The reason for this section of the docs is to explain how to get a stock Mac OS X desktop - or linux server - up and running with the necessary tools to be able to develop and test this project.

### Necessary Tools

**[Homebrew](http://brew.sh/)** - all of the following tools can be installed with Homebrew. If it's not already installed on you laptop, it's easy enough to go to the website, run the command to install it, and then continue to the next step. If you are on linux, expect to be installing some scripts and RPMs, but everything is available there as well - just not as convenient.

**[JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)** - it might be nice to get the JDK 1.6 from Apple, but it's _essential_ to get the JDK 1.8 from Oracle. This is a download and package install, but it's pretty simple to do and sets up it's own updater for future releases.

**[Leiningen](http://leiningen.org/)** - it is the _swiss army knife_ of clojure development - library version control, build, test, run, deploy tool all in one with the ability to create extensions. On the Mac, this is installed very easily with:

```bash
$ brew install leiningen
```

and on linux you can download the [lein script](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein) to _somewhere_ in your `$PATH`, and make it executable. Then:

```bash
$ lein
```

and it'll download everything it needs to run properly.

**[Postgres](http://www.postgresql.org/)** - at the current time, this is the back-end persistence and it's really a very simple product to install and use. On the Mac, simply:

```bash
$ brew install postgres
```

and when it's done, follow the directions to run it - the configuration is fine as it is. I've used the `launchctl` version:

```bash
$ ln -sfv /usr/local/opt/postgresql/*.plist ~/Library/LaunchAgents/
$ launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist
```

and it's running everytime you log in.

### Creating the Postgres Database Schema

The database schema for the app is created by the files in `deploy/sql` and each deals with all the database objects for a given service. Each one is a stand-alone script that needs to be run through Postgres' `psql` command line tool. But the first step is to create the database in the first place.

If you choose not to use the name `inventory`, that's OK, whatever you choose can be set in the development configuration in the next section. But assuming we go with the defaults, then simply create the database with:

```bash
$ createdb inventory
```

At this point, you can then run all the creation scripts:

```bash
$ cd deploy/sql
$ psql -d inventory -f create_all.sql
$ psql -d inventory -f seed_cars.sql
```

but you can also do this to the default database with the single command:

```bash
$ make schema
```

### Configuring the Project for Development

In order to allow each developer to have a slightly different configuration for their needs, and at the same time make it very easy to get up and going, we have placed a _starter_ development configuration file in `resources/config_orig.clj`. You simply need to copy this into the file `resources/config.clj` - which is _**very**_ important, as that specific file is read in the start-up of the clojure process:

```bash
$ cd resources
$ cp config_orig.clj config.clj
```

At this point, any changes made to `config.clj` are _not_ tracked, but if something needs to be added to the _starter_ file, that's easy to do.

Looking at the contents of that _starter_ file, we see:

```clojure
{
 ;; Location of the redis server for development
 :redis {:location "localhost:6379"}

 ;; HipChat auth token and room data for notifications
 :hipchat {:auth-token "abcdefghijklmnopqrstuvwxyz"
           :info-room 112233
           :error-room 445566}

 ;; just a local database - should it turn out we need it
 :database {:classname "org.postgresql.Driver"
            :subprotocol "postgresql"
            :subname "//localhost/inventory"
            :user "myuser"
            :migrator "myuser"}
}
```

The file is simply a clojure map, where the _keys_ define the 'sections' in the configuration. There's one for the database - `:database`, and this is where you can change the name of the database you created, or point it to a shared database for testing.

There are several sections that might not be used - `:webhdfs` is likely one, but if we need to be able to read/write Hadoop, then this will become _very_ handy.

### Running the REPL

Once the repo is down and the necessary tools are installed, you can run the clojure REPL - similar to the Ruby `irb` command, this allows you to run and work with the code in an interactive shell. Simply:

```bash
$ cd inventory
$ lein repl
nREPL server started on port 56816 on host 127.0.0.1 - nrepl://127.0.0.1:56816
REPL-y 0.3.5, nREPL 0.2.6
Clojure 1.6.0
Java HotSpot(TM) 64-Bit Server VM 1.7.0_71-b14
    Docs: (doc function-name-here)
          (find-doc "part-of-name-here")
  Source: (source function-name-here)
 Javadoc: (javadoc java-object-or-class-here)
    Exit: Control+D or (exit) or (quit)
 Results: Stored in vars *1, *2, *3, an exception in *e

inventory.main=>
```

at this point, you are live with the code and can run the functions as you wish.

### Running the Tests

[Leiningen](http://leiningen.org/) also controls the tests. These are in the directory structure:

```
  inventory
   |-- test
       |-- inventory
           |-- test
               |-- core.clj
               |-- util.clj
         ...
```

and you can run all of them with the simple command:

```bash
$ lein test
```

There are, of course, lots of options and testing capabilities, but if the tests are written right, then this will result in something like:

```bash
lein test inventory.test.core

lein test inventory.test.util

Ran 5 tests containing 23 assertions.
0 failures, 0 errors.
```

If there are errors, they will be listed, with the expected vs. actual also provided to assist in debugging the problem with the tests.

### Running the Web Server

The current code has a RESTful service to make OAuth2 authentication possibe, so at some point in time, it will _likely_ make sense to run the service on your local machine, against the locally running custos-server instance. To do this, simply:

```bash
$ lein run web
```

and then you can monitor the log in the `/log` directory:

```bash
$ tail -f log/inventory.log
```

## Release Notes

The goal of this section of the docs is to provide a basic set of release notes for the project where each release is tagged in git, and available for check-out, revision, etc.

### Version 0.1.0

This is the first cut of the test app, and it's doing OK for now.

There is even a RESTful API for getting the data out of the service.
