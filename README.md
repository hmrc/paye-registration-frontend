# paye-registration-frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/paye-registration-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/paye-registration-frontend/_latestVersion)

Source code for the PAYE Registration frontend microservice, part of the SCRS journey.

## Running the Application


In order to run the microservice, you must have SBT installed. You should then be able to start the application using: 

```sbt "run {PORTNUM}"```



To run the tests for the application, you can run: ```sbt test it:test``` 

or ```sbt coverage test it:test coverageReport```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")