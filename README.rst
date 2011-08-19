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
