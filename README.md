MathML Pipeline
================

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/ag-gipp/vmext-demo.svg?branch=master)](https://travis-ci.org/ag-gipp/vmext-demo)


This project is a Java application (Spring Boot) with an embedded Apache Server.
It takes on the following tasks:

1. Convert LaTeX formula to MathML semantics (via MathMLConverters).
2. Compare two MathML semantics and receive node similarities (via tree comparison on MathML).
3. Render a single MathML oder the comparison results of both MathML (via a JS widget).

Task 1 and 2 are executed via an embedded REST Service in this project and is written in Java.
Task 3 is executed via an external JS widget written by students at the HTW Berlin.

## Build ##

Check-out this project and build it via Maven. In the `/target` directory you will 
find the executable server instance `mathpipeline.jar`.

`mvn clean install`

Maven 3.2 or higher is required for the build plugin, it will create an executable jar.

## Requirements ##

For the conversion from LaTeX to MathML, `LaTeXML` is required. Via configuration you can choose
to use an external service for the LaTeX &gt; MathML conversion or use a
local installation of LaTeXML.

You can find LaTeXML here: http://dlmf.nist.gov/LaTeXML/get.html.

## Installation (Standalone) ##

Copy the Jar from the `target` folder to wherever you want. 

Start the server by `java -jar mathpipeline.jar`.
   
Now just call the server on `http://localhost:8080/index.html` and 
you can start whatever you what to do.

## Installation (Service) ##

Since this is a Spring Boot application it can easily be used as a 
Service in a Linux environment, see: 
https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html

Copy the Jar from the `target` folder to `/var/mathpipeline/`

1. Simply create a symlink (_change the path towards your installation_)

    `$ sudo ln -s /var/mathpipeline/mathpipeline.jar /etc/init.d/mathpipeline`
    
2. Once installed, you can start and stop the service in the usual way,
 e.g. `service mathpipeline [start|stop|status|restart]`

You can find an automatic log in `/var/log/mathpipeline.log`.

### JVM Options ###

It is recommended to limit your JVM and set VM options. Just place a file
called `mathpipeline.conf` besides the `mathpipeline.jar` and add the following content:

    export JAVA_OPTS="-Xmx512m -Xms256m"

It should then look like this:

    $ <your_installation_path>    
    $ ls
    mathpipeline.jar
    mathpipeline.conf
    $ cat mathpipeline.conf
    export JAVA_OPTS="-Xmx512m -Xms256m"

## REST API ##

We use Swagger for the API documentation. You can always view the API per 
default on `http://localhost:8080/swagger-ui.html`.

## Configuration ##

If you want to use a custom configuration place a file named `application.yaml` 
in the execution / installation folder. The content should be (_this is the default configuration used by the application_):

    server:
    #  servlet-path: /pipe   # custom servlet-path
      port: 8080            # default server port, if not set otherwise
    
    # Math AST Renderer - Main URL
    mast.url: http://math.citeplag.org
    
    # the use of the latexml online service (via url)
    # if no url is given a local latexml installation will be called
    latexml:
      active: true
      url: http://gw125.iu.xsede.org:8888 # url for online service or emtpy ""
      params:                             # parameters for the online service
        whatsin: math
        whatsout: math
        includestyles:
        format: xhtml
        pmml:
        cmml:
        nodefaultresources:
        linelength: 90
        quiet:
        preload:
          - "LaTeX.pool"
          - "article.cls"
          - "amsmath.sty"
          - "amsthm.sty"
          - "amstext.sty"
          - "amssymb.sty"
          - "eucal.sty"
          - "DLMFmath.sty"
          - "[dvipsnames]xcolor.sty"
          - "url.sty"
          - "hyperref.sty"
          - "[ids]latexml.sty"
          - "texvc"

    # Mathoid - alternative Latex to MathML converter
    mathoid:
      active: true
      url: http://localhost:10044/mml
      
# Deploy for DKE

The `vmext-demo` docker is now a private docker container. It is no 
longer maintained over DockerHub because it contains LaCASt (currently private).

Updating vmext-demo on DKE has to be done manually now. 

1. checkout the repo and build it so that you have a running `target/mathpipeline.jar`
2. Check if its working locally. You need to update `lacast.config.yaml` according to your system needs.
You may need elasticsearch and mathoid running locally also or you create port-forwarding and connect to DKE via ssh.
3. Run locally `java -jar target/mathpipeline.jar` and check on `localhost:8080/swagger-ui.html`
if everything works as expected.
4. Bring the changed files to DKE via `scp target/mathpipeline user@dke01:~/mathpipeline.jar`
5. Login to dke server via ssh
6. Find the running docker container via `docker ps`. The name is `vmext-demo` and tag `lacast`.
7. Copy the changed files to the appropriate locations inside the docker container via
`docker cp mathpipeline.jar vmext-demo:/mathpipeline.jar`
8. **IMPORTANT:** update the image of the container after you changed something in the container via 
`docker commit vmext-demo vmext-demo:lacast` (first the docker name, than the image name)
9. Restart the container so that the changes gets applied via
`/usr/bin/docker-compose restart vmext-demo`. 

That's it.

If you need to start from scratch you need a free developer license for mathematica and:
1. Again, build everything locally and test if it works.
2. Build the container `docker build . -t vmext-demo:lacast` (thats the image name). This may take some
time because installing mathematicas license requires some basic ubuntu libraries.
3. Run the docker container for the first time `docker run --name vmext-demo -p 8080:8080 vmext-demo:lacast`
4. Now connect to the running container `docker exec -it vmext-demo /bin/bash`
5. Run `/usr/bin/wolframscript` and activate your license (e-mail credentials you took to get your free developer license. 
The credentials are the same you need on the website. The wolfram ID is you e-mail.)
6. exit with `exit`
7. **Very important:** update your container NOW! otherwise on the next restart its gone...
`docker commit vmext-demo vmext-demo:lacast`.
8. That's it. For deployment and updates, you need to follow the guide above.
