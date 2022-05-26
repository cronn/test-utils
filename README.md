[![CI](https://github.com/cronn/test-utils/workflows/CI/badge.svg)](https://github.com/cronn/test-utils/actions)
[![Valid Gradle Wrapper](https://github.com/cronn/test-utils/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/test-utils/actions/workflows/gradle-wrapper-validation.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils)
[![Apache 2.0](https://img.shields.io/github/license/cronn/test-utils.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# cronn test-utils

Utility classes for JUnit 5 tests.

### JUnit5MisusageCheck

JUnit5 `@BeforeAll`, `@BeforeEach`, `@Test`, `@AfterEach` and `@AfterAll` require that annotated methods are not overriden/hidden, otherwise they are silently ignored. JUnit5MisusageCheck is a simple way to detect such situations and fail fast rather than sweep it under the carpet.

Preferred way to use this extension is to [automatically register it for all tests](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic):
 - add `META-INF/services/org.junit.jupiter.api.extension.Extension` file containing `de.cronn.testutils.JUnit5MisusageCheck`
 - add `junit.jupiter.extensions.autodetection.enabled=true` property (for example to [`junit-platform.properties`](https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params) file)


### H2Util

H2Util allows to restore empty state of H2 database in integration tests. Intended usage in `@BeforeEach` callback:
```java
@BeforeEach
void cleanupDatabase(@Autowired H2Util h2Util) {
        h2Util.resetDatabase();
}
```

Empty state is defined as:
 - empty tables, sequences restarted for `resetDatabase()` 
 - empty database schema for `dropAllObjects()`

Warning: H2Util requires JPA, Spring and H2 dependencies which are defined as optional in library: you have to provide them on your own.

### TestClock
TestClock is an implementation of the abstract class `Clock`, and provides features useful for testing. If not specified it is initialized with a fixed Instant in the past. Its time is fixed, this clock will not run. Therefore, it also provides features to manipulate its time.

### ConcurrentTest
ConcurrentTest offers support for tests which need to run on mutliple threads. It handles the overhead of submitting and evaluating each of a given number of a `task`. It uses the `ExecutorServiceUtils` to ensure that the `executorService` is properly shut down and the `task queue` is cleared, so that subsequent tests can run without interference.

## Usage
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>0.4.0</version>
    <scope>test</scope>
</dependency>
```
