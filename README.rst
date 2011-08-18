==================
House the Homeless
==================

:Info: An administrative app for use by homeless shelters
:Authors: Steve Challis (http://schallis.com)
:Requires: Leiningen 1.6.1, Noir 1.1.1, appengine-magic 0.4.3
:License: Distributed under the Eclipse Public License, the same as Clojure.

Installation and Usage
======================

If you are already setup with Lein, then the following should work for
testing:  

    $ lein deps  
    $ lein appengine-prepare  
  
To deploy to App Engine:  

    $ lein appengine-update house-the-homeless
