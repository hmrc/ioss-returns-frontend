
# ioss-returns-frontend

This is the repository for Import One Stop Shop Returns Frontend

Backend: https://github.com/hmrc/ioss-returns

Stub: https://github.com/hmrc/ioss-returns-stub

Requirements
------------
=
This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application

To update from Nexus and start all services from the RELEASE version instead of snapshot
```
sm2 --start IMPORT_ONE_STOP_SHOP_ALL
```

### To run the application locally execute the following:

```
sm2 --stop IOSS_RETURNS_FRONTEND
```
and
```
sbt run
```

Unit and Integration Tests
------------

To run the unit and integration tests, you will need to open an sbt session in the terminal.

### Unit Tests

To run all tests, run the following command in your sbt session:
```
test
```

To run a single test, run the following command in your sbt session:
```
testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
testOnly *YourAccountControllerSpec
```

### Integration Tests

To run all tests, run the following command in your sbt session:
```
it:test
```

To run a single test, run the following command in your sbt session:
```
it:testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
it:testOnly *SessionRepositorySpec
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
