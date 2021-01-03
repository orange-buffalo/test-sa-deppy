# simple-accounting

## Development

### Screenshot Tests

1. Start Storybook:
    ```shell script
   npm run storybook:serve
    ```
1. Run the tests:
    ```shell script
   npm run test:screenshot
    ```

To run a particular story or kind:
```shell script
export STORYSHOTS_STORY_NAME="Initial Loading"
export STORYSHOTS_STORY_KIND="EditIncome"
npm run test:screenshot
```

### Load Tests

At this point we decided to not include load test into the CI pipeline. Load test are executed on demand locally.

To run the load tests, follow these steps:
1. Download the database snapshot with required data:
  * [part 1](https://github.com/orange-buffalo/simple-accounting-load-tests-data/raw/master/load-tests-db-snapshot.7z.001)
  * [part 2](https://github.com/orange-buffalo/simple-accounting-load-tests-data/raw/master/load-tests-db-snapshot.7z.002)
1. Unzip the archives.
1. Launch application with `load-tests` Spring Boot profile and `spring.datasource.url` set to 
`jdbc:h2:<path-to-database-file>`.
1. Start JMeter and open `load-tests/jmeter-load-tests.jmx` project.
1. Launch the tests in JMeter.
test
