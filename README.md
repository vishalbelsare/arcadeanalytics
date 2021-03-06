[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/ArcadeAnalytics/arcadeanalytics.svg?branch=master)](https://travis-ci.org/ArcadeAnalytics/arcadeanalytics)

![Arcade Analytics Screenshot](https://arcadeanalytics.com/wp-content/uploads/2018/11/arcade-panama-papers-dashboard.png)

# If you want to just run

Go to https://github.com/ArcadeData/arcadeanalytics-recipes

clone that repo and run

docker-compose -f recipes/arcade-standalone.yml up

# Arcade Analytics - Play With Data

Docker images of Arcade and demo databases are available on [Docker hub](https://cloud.docker.com/u/arcadeanalytics/)

## Quick start 

To launch ArcadeAnalytics just run (needs docker installed)

    docker-compose -f src/main/docker/app.yml up
    
and then go to 
    
    http://localhost:8080/
    
login as _user_ with password _user_

To be able to create new users, fill the properties inside the compose:

          - SPRING_EMAIL_HOST=smtp.gmail.com
          - SPRING_EMAIL_PORT=587
          - SPRING_EMAIL_USERNAME=
          - SPRING_EMAIL_PASSWORD=
          - JHIPSTER_MAIL_FROM=
          - JHIPSTER_MAIL_BASE-URL=

Then restart the container and login with _admin/admin_ credentials.

The Docker compose starts ArcadeAnalytics, a PostgreSQL database, an Elastic instance and an OrientDB with its _demodb_ preloaded.

For detailed instructions on how to use Arcade read the [manual](https://arcadeanalytics.com/usermanual/) 

## Start with "single" (embedded) image

Arcade is provided as a all-embedded image, where hsql and embedded Elasicsearch are used instead of Postgresql and ES on separate containers.

    docker-compose -f src/main/docker/app-single.yml up

This compose does not start containers with test databases.

## Configure SSH 

AracadeAnalytics can connect to databases using an SSH tunnel.
To do that, it needs the private and public keys to be used by the application.
In the _app.yml_ or _app-single.yml_ :

              - JAVA_OPTS=-DSSH_PRIV_KEY=/arcade/.ssh/id_rsa -DSSH_PUB_KEY=/arcade/.ssh/id_rsa.pub

The directory /arcade inside the container is mounted as volume:

            volumes:
              - ~/.arcade/:/arcade/

Create a directory in *~/.arcade/* named *.ssh* and put the private and public keys inside.
The public key should be copied on the server used as ssh gateway too.


## Run support containers with test databases

Check the [dedicated repository](https://github.com/ArcadeAnalytics/arcadeanalytics-recipes)

## Connect to Amazon AWS

Read the dedicated [post](https://arcadeanalytics.com/amazon-neptune-hands-on/).
Use your own SSH keys, as explained in the previous paragraph.

## Development

*NOTE*: Arcade was built using JHipster

Before you can build this project, you must install and configure the following dependencies on your machine:

1. [Node.js][]: We use Node to run a development web server and build the project.
   Depending on your system, you can install Node either from source or as a pre-packaged bundle.
2. [Yarn][]: We use Yarn to manage Node dependencies.
   Depending on your system, you can install Yarn either from source or as a pre-packaged bundle.

After installing Node, you should be able to run the following command to install development tools.
You will only need to run this command when dependencies change in [package.json](package.json).

    yarn install

We use yarn scripts and [Webpack][] as our build system.


Run the following commands in two separate terminals to create a blissful development experience where your browser
auto-refreshes when files change on your hard drive.

    ./mvnw
    yarn start

[Yarn][] is also used to manage CSS and JavaScript dependencies used in this application. You can upgrade dependencies by
specifying a newer version in [package.json](package.json). You can also run `yarn update` and `yarn install` to manage dependencies.
Add the `help` flag on any command to see how you can use it. For example, `yarn help update`.

The `yarn run` command will list all of the scripts available to run for this project.

### Service workers

Service workers are commented by default, to enable them please uncomment the following code.

* The service worker registering script in index.html
```
<script>
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker
        .register('./sw.js')
        .then(function() { console.log('Service Worker Registered'); });
    }
</script>
```
* The copy-file option in webpack-common.js
```js
{ from: './src/main/webapp/sw.js', to: 'sw.js' },
```
Note: Add the respective scripts/assets in `sw.js` that is needed to be cached.

### Managing dependencies

For example, to add [Leaflet][] library as a runtime dependency of your application, you would run following command:

    yarn add --exact leaflet

To benefit from TypeScript type definitions from [DefinitelyTyped][] repository in development, you would run following command:

    yarn add --dev --exact @types/leaflet

Then you would import the JS and CSS files specified in library's installation instructions so that [Webpack][] knows about them:
Edit [src/main/webapp/app/vendor.ts](src/main/webapp/app/vendor.ts) file:
~~~
import 'leaflet/dist/leaflet.js';
~~~

Edit [src/main/webapp/content/css/vendor.css](src/main/webapp/content/css/vendor.css) file:
~~~
@import '~leaflet/dist/leaflet.css';
~~~
Note: there are still few other things remaining to do for Leaflet that we won't detail here.

For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

### Using angular-cli

You can also use [Angular CLI][] to generate some custom client code.

For example, the following command:

    ng generate component my-component

will generate few files:

    create src/main/webapp/app/my-component/my-component.component.html
    create src/main/webapp/app/my-component/my-component.component.ts
    update src/main/webapp/app/app.module.ts

### Doing API-First development using swagger-codegen

[Swagger-Codegen]() is configured for this application. You can generate API code from the `src/main/resources/swagger/api.yml` definition file by running:
Then implements the generated interfaces with `@RestController` classes.

To edit the `api.yml` definition file, you can use a tool such as [Swagger-Editor](). Start a local instance of the swagger-editor using docker by running: `docker-compose -f src/main/docker/swagger-editor.yml up -d`. The editor will then be reachable at [http://localhost:7742](http://localhost:7742).

Refer to [Doing API-First development][] for more details.

## Building for production

To optimize the arcadeanalytics application for production, run:

    ./mvnw -Pprod clean package

This will concatenate and minify the client CSS and JavaScript files. It will also modify `index.html` so it references these new files.
To ensure everything worked, run:

    java -jar build/libs/*.war

Then navigate to [http://localhost:8080](http://localhost:8080) in your browser.

Refer to [Using JHipster in production][] for more details.

## Testing

To launch your application's tests, run:

    ./mvnvtest

### Client tests

Unit tests are run by [Karma][] and written with [Jasmine][]. They're located in [src/test/javascript/](src/test/javascript/) and can be run with:

    yarn test

UI end-to-end tests are powered by [Protractor][], which is built on top of WebDriverJS. They're located in [src/test/javascript/e2e](src/test/javascript/e2e)
and can be run by starting Spring Boot in one terminal (`./gradlew bootRun`) and running the tests (`yarn run e2e`) in a second one.

### Other tests

Performance tests are run by [Gatling][] and written in Scala. They're located in [src/test/gatling](src/test/gatling) and can be run with:

    ./mvnw gatling:test

For more information, refer to the [Running tests page][].

## Using Docker to simplify development (optional)

You can use Docker to improve your JHipster development experience. A number of docker-compose configuration are available in the [src/main/docker](src/main/docker) folder to launch required third party services.
For example, to start a postgresql database in a docker container, run:

    docker-compose -f src/main/docker/postgresql.yml up -d

To stop it and remove the container, run:

    docker-compose -f src/main/docker/postgresql.yml down

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

    ./mvnw clean package -Pprod 

Then run:

    docker-compose -f src/main/docker/app.yml up -d

For more information refer to [Using Docker and Docker-Compose][], this page also contains information on the docker-compose sub-generator (`jhipster docker-compose`), which is able to generate docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more information.

[JHipster Homepage and latest documentation]: http://www.jhipster.tech
[JHipster 4.11.1 archive]: http://www.jhipster.tech/documentation-archive/v4.11.1

[Using JHipster in development]: http://www.jhipster.tech/documentation-archive/v4.11.1/development/
[Using Docker and Docker-Compose]: http://www.jhipster.tech/documentation-archive/v4.11.1/docker-compose
[Using JHipster in production]: http://www.jhipster.tech/documentation-archive/v4.11.1/production/
[Running tests page]: http://www.jhipster.tech/documentation-archive/v4.11.1/running-tests/
[Setting up Continuous Integration]: http://www.jhipster.tech/documentation-archive/v4.11.1/setting-up-ci/

[Gatling]: http://gatling.io/
[Node.js]: https://nodejs.org/
[Yarn]: https://yarnpkg.org/
[Webpack]: https://webpack.github.io/
[Angular CLI]: https://cli.angular.io/
[BrowserSync]: http://www.browsersync.io/
[Karma]: http://karma-runner.github.io/
[Jasmine]: http://jasmine.github.io/2.0/introduction.html
[Protractor]: https://angular.github.io/protractor/
[Leaflet]: http://leafletjs.com/
[DefinitelyTyped]: http://definitelytyped.org/
[Swagger-Codegen]: https://github.com/swagger-api/swagger-codegen
[Swagger-Editor]: http://editor.swagger.io
[Doing API-First development]: http://www.jhipster.tech/documentation-archive/v4.11.1/doing-api-first-development/
