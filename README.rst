==================
House the Homeless
==================

:Info: An administrative app for use by homeless shelters
:Authors: Steve Challis (http://schallis.com)
:Requires: Leiningen 1.6.1, Noir 1.1.1, appengine-magic 0.4.3
:License: Distributed under the Eclipse Public License, the same as Clojure.

Installation and Usage
======================

If you have the correct version of Leiningen installed, getting
started should be as simple as pulling in dependancies and building::

    $ lein deps
    $ lein appengine-prepare  
  
To deploy to App Engine::

    $ lein appengine-update house-the-homeless

Note that the config files in the `war` directory must be changed so
that the application is uploaded to your own App Engine account.

Interactive Development
=======================

The development process starts by launching `lein swank` or `lein
repl`. You then need to compile `core.clj`, `routes.clj`, and
`app_servlet.clj`. A test Jetty server can then be started to serve
the application::

    user> (ns house-the-homeless.core)
    house-the-homeless.core> (require '[appengine-magic.core :as ae])
    house-the-homeless.core> (ae/serve house-the-homeless-app)

By default, the application will be available at `localhost:8080`. Any
changes and recompilations of the code should be reflected immediately
in the running application.

Downloading Data
================

Data export is performed using the App Engine Python bulkloader
tools. A remote_api servelet has already been added to the
application.

To create a config file, run the following command::

    appcfg.py create_bulkloader_config --url=http://house-the-homeless.appspot.com/remote_api --application=s~house-the-homeless --filename=config.yaml
    
The config file will need slight tweaking to specify the connector
preferences but once modified, it can be used to download data like
so::

     appcfg.py download_data --config_file=config.yaml --filename=client.csv --url=http://house-the-homeless.appspot.com/remote_api --kind=Client --application=s~house-the-homeless

.. note:: The authentication for the application must be set to the
default Google Auth API for the bulkloader to work.

Screenshots
===========
.. image:: https://github.com/schallis/house-the-homeless/raw/master/screenshots/screenshot-client.png
.. image:: https://github.com/schallis/house-the-homeless/raw/master/screenshots/screenshot-calendar.png
