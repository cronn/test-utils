[![CI](https://github.com/cronn/test-utils/workflows/CI/badge.svg)](https://github.com/cronn/test-utils/actions)
[![Valid Gradle Wrapper](https://github.com/cronn/test-utils/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/test-utils/actions/workflows/gradle-wrapper-validation.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/test-utils)
[![Apache 2.0](https://img.shields.io/github/license/cronn/test-utils.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# cronn test-utils

Utility classes for JUnit 5 tests.

## Basic features

Add the following dependency to your project:

Gradle:
```groovy
testImplementation "de.cronn:test-utils:{version}"
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
</dependency>
```

### JUnit5MisusageCheck

JUnit 5 lifecycle annotations (`@BeforeAll`, `@BeforeEach`, `@Test`, `@AfterEach`, `@AfterAll`) require that annotated methods are not overridden or hidden; otherwise they are silently ignored. `JUnit5MisusageCheck` detects such situations and fails fast instead of hiding the problem.

The preferred way to register this extension is [automatic registration for all tests](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic):
 - Add a `META-INF/services/org.junit.jupiter.api.extension.Extension` file containing `de.cronn.testutils.JUnit5MisusageCheck`
 - Add `junit.jupiter.extensions.autodetection.enabled=true` to your [`junit-platform.properties`](https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params)

If a subclass accidentally overrides a `@BeforeEach` method without re-annotating it, `JUnit5MisusageCheck` will throw an exception during test setup and report which method is affected.

### TestInfoExtension

`TestInfoExtension` implements `TestInfo` and can be registered as a field in your test class, avoiding the need to inject `TestInfo` as a method parameter in every lifecycle method.

```java
class MyTest {

    @RegisterExtension
    TestInfoExtension testInfo = new TestInfoExtension();

    @Test
    void example() {
        assertThat(testInfo.getDisplayName()).isEqualTo("example()");
    }
}
```

### TestClock

`TestClock` is an implementation of `Clock` with a fixed time useful for testing. It is initialized to `2016-01-01T00:00:00.123456Z` by default and does not advance on its own. Use `changeInstant()`, `windClock()`, or `reset()` to control its time.

```java
@Bean
TestClock testClock() {
    return new TestClock();
}

@Test
void orderExpiresAfterOneDay() {
    Order order = orderService.create();
    testClock.windClock(Duration.ofDays(1));
    assertThat(order.isExpired()).isTrue();
}
```

### ConcurrentTest

`ConcurrentTest` provides support for tests that need to run on multiple threads. It handles the overhead of submitting and evaluating a given number of tasks. It uses `ExecutorServiceUtils` to ensure that the `ExecutorService` is properly shut down and the task queue is cleared so that subsequent tests run without interference.

```java
class RegistrationServiceTest {

    @RegisterExtension
    TestInfoExtension testInfo = new TestInfoExtension();

    @Test
    void registrationIsThreadSafe() throws Exception {
        ConcurrentTest.create((int index) -> userService.register("user-" + index))
            .withConcurrencyLevel(20)
            .withThreadNamePrefix(testInfo.getDisplayName())
            .runAndAssertEachResult(user -> assertThat(user.getId()).isNotNull());
    }
}
```

Setting a thread name prefix is useful for debugging: log output from worker threads will include the prefix, making it easier to attribute log lines to a specific test. `withThreadNamePrefixFromClass()` is a shorthand when the class name is sufficient.

## Optional features

Some features have additional dependencies that are not included by default. Declare the relevant Gradle capability or Maven classifier to pull them in.

### 🗄️ H2 support

`H2Util` brings an H2 database back to a clean state between tests. Typical usage in a `@BeforeEach` callback:

```java
@BeforeEach
void cleanupDatabase(@Autowired H2Util h2Util) {
    h2Util.resetDatabase();
}
```

Empty state is defined as:
- Empty tables, sequences restarted for `resetDatabase()`
- Empty database schema for `dropAllObjects()`

`H2Util` is automatically provided as a Spring bean via auto-configuration when the h2-support feature and a single `DataSource` bean are present on the classpath. No manual `@Bean` definition or `@Import` is needed.

> [!NOTE]
> Hibernate caches sequence values in advance. If you use JPA with Hibernate, see [Hibernate support](#%EF%B8%8F-hibernate-support) to keep Hibernate's in-memory sequence state in sync after each database reset.

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-h2-support")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>h2-support</classifier>
</dependency>
```

### 🐘 Postgres support

`PostgresUtil` brings a PostgreSQL database back to a clean state between tests. It is designed for use in Spring Boot integration tests and is automatically registered as a Spring bean via auto-configuration.

`truncateAllTables()` truncates all tables in the `public` schema (excluding Liquibase changelog tables) with `RESTART IDENTITY CASCADE`. Tables can be excluded by name. `resetAllSequences()` resets all sequences with a non-null `last_value` back to 1 and returns the names of the reset sequences.

Typical usage in a `@BeforeEach` callback:

```java
@BeforeEach
void cleanupDatabase(@Autowired PostgresUtil postgresUtil) throws SQLException {
    postgresUtil.truncateAllTables();
    postgresUtil.resetAllSequences();
}
```

> [!NOTE]
> Hibernate caches sequence values in advance. If you use JPA with Hibernate, see [Hibernate support](#-hibernate-support) to keep Hibernate's in-memory sequence state in sync after each database reset.

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-postgres-support")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>postgres-support</classifier>
</dependency>
```

### Hibernate support

Hibernate's pooled sequence optimizer pre-allocates IDs in batches: it fetches the next high value from the database sequence once, then serves IDs from its in-memory cache until the batch is exhausted. This reduces the number of sequence queries, especially with batched inserts.

In integration tests, database sequences are typically reset between test runs to keep generated IDs deterministic. However, resetting the database sequence does not clear Hibernate's in-memory cache. On the next insert, Hibernate continues serving IDs from the stale cache, which causes either duplicate key errors or unexpected ID values.

`HibernateUtil` resolves this by resetting the internal state of all Hibernate sequence generators. It is automatically registered as a Spring bean via auto-configuration.

```java
@BeforeEach
void cleanupDatabase(
        @Autowired PostgresUtil postgresUtil,
        @Autowired HibernateUtil hibernateUtil) throws SQLException {
    postgresUtil.truncateAllTables();
    List<String> resetSequences = postgresUtil.resetAllSequences();
    if (!resetSequences.isEmpty()) {
        hibernateUtil.resetSequenceGeneratorStates(resetSequences);
    }
}
```

> [!NOTE]
> `HibernateUtil` accesses internal Hibernate APIs via reflection. Test thoroughly after Hibernate upgrades.

Alternatively, you can disable Hibernate's sequence caching entirely:

```properties
spring.jpa.properties.hibernate.id.optimizer.pooled.preferred=NONE
```

This avoids the need for `HibernateUtil`, but requires one sequence query per entity insertion instead of one query per batch. This is particularly costly when using batched inserts.

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-hibernate-support")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>hibernate-support</classifier>
</dependency>
```

### 🌱 Spring support

`ResetClockExtension` is a convenient JUnit 5 extension for Spring-based integration tests: it automatically resets a `TestClock` bean found in the test `ApplicationContext` after each test.

The extension can be registered [automatically for all tests](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic) by adding the following to your [`junit-platform.properties`](https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params):

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

The spring-support JAR already ships the necessary `META-INF/services` entry. No `@ExtendWith` annotation is needed.

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-spring-support")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>spring-support</classifier>
</dependency>
```

### 🔍 JPA query capturing support

`QueryCaptor` captures the SQL queries and their parameters executed during a test via [datasource-proxy](https://github.com/ttddyy/datasource-proxy). This makes it easy to detect N+1 problems, unexpected lazy loading, or unintended updates. See [hibernate-stop-guessing-start-testing](https://github.com/cronn/hibernate-stop-guessing-start-testing) for a full example and background.

A `QueryCaptor` bean is provided automatically via auto-configuration and can be injected into test classes. Use `captureQueriesDuring(...)` to capture all SQL executed within a block and `getCapturedQueries()` to retrieve the formatted results:

```java
@SpringBootTest
class OrderServiceTest {

    @Autowired QueryCaptor queryCaptor;

    @Test
    void loadsOrders() throws Exception {
        queryCaptor.captureQueriesDuring(() -> orderService.findAll());
        List<String> queries = queryCaptor.getCapturedQueries();
        assertThat(queries).hasSize(1);
    }

    @Test
    void createsOrder() throws Exception {
        Order created = queryCaptor.captureQueriesDuring(() -> orderService.create(new OrderRequest(...)));
        assertThat(created.getId()).isNotNull();
    }
}
```

> [!TIP]
> Knowing how many queries were executed is a start, but to really catch regressions you also want to verify exactly which queries ran, which tables were touched, which parameters were bound, and whether a `JOIN` suddenly turned into N selects. Asserting all of that inline gets verbose fast. That's where `QueryValidationTraits` comes in.

#### Asserting against validation files

`QueryValidationTraits` is a mixin interface that connects `QueryCaptor` with [validation-file-assertions](https://github.com/cronn/validation-file-assertions). It captures the full formatted query output and compares it against a validation file in a single call to `captureQueryAndCompareWithFile(...)`. Implement `getQueryCaptor()` to wire it in:

```java
@SpringBootTest
class OrderServiceTest implements QueryValidationTraits {

    @Autowired QueryCaptor queryCaptor;

    @Override
    public QueryCaptor getQueryCaptor() {
        return queryCaptor;
    }

    @Test
    void loadsOrders() {
        captureQueryAndCompareWithFile(() -> orderService.findAll());
    }

    @Test
    void createsOrder() {
        Order created = captureQueryAndCompareWithFile(() -> orderService.create(new OrderRequest(...)));
        assertThat(created.getId()).isNotNull();
    }
}
```

On the first run the validation file is generated automatically and contains the full SQL of every captured query including its bound parameters. On subsequent runs the output is compared against it, so any change in query structure, table access, or parameter values will fail the test. See the [validation-file-assertions] documentation for details on file locations and how to update validation files.

By default, byte array parameters are masked with `[MASKED-BYTE-ARRAY]` via `ByteArrayReplacer`. Override `defaultValidationNormalizerForQueryCaptor()` in your test class to supply a different normalizer, or pass one explicitly:

```java
captureQueryAndCompareWithFile(() -> orderService.findAll(), new MyCustomNormalizer());
```

If Hibernate is on the classpath, SQL queries are automatically formatted using Hibernate's SQL formatter. To use a different formatter, define a `SqlQueryFormatter` bean in your test application context; it will take precedence over the default.

> [!NOTE]
> `de.cronn:validation-file-assertions` and `datasource-proxy-spring-boot-starter` are pulled in automatically as transitive dependencies.

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-jpa-query-capturing-support")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>jpa-query-capturing-support</classifier>
</dependency>
```

### 🛡️ Authorization Test Support [INCUBATING]

> **INCUBATING** — API and output format may change in a backwards-incompatible way in any release.

AuthorizationTestUtil generates an authorization matrix for a running Spring MVC application as a Markdown table.
This is useful for asserting that each endpoint is reachable by exactly the roles you expect.
We recommend to assert the authorization matrix using our [validation-file-assertions] library.

For every endpoint registered in the application's `RequestMappingHandlerMapping`,
the AuthorizationTestUtil issues one HTTP request per provided role (plus one anonymous request)
and records which requests were *not* rejected with `401`/`403`/`405`.
Endpoints accessible to every provided role render as `{ANY_ROLE}`;
anonymously accessible endpoints render as `{UNAUTHENTICATED}`.


```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyAuthorizationTest implements JUnit5ValidationFileAssertions {

    @LocalServerPort int port;
    @Autowired RequestMappingHandlerMapping handlerMapping;

    @Test
    void authorizationMatrix() {
        List<RoleAndToken> roles = List.of(
            new RoleAndToken("ADMIN", adminToken),
            new RoleAndToken("USER",  userToken));
        String markdown = AuthorizationTestUtil.buildAuthorizationMatrix(
            port, handlerMapping, roles, List.of("/ignored-endpoint-prefix"));
        assertWithFile(markdown, FileExtensions.MD);
    }
}
```

Gradle:
```groovy
testImplementation("de.cronn:test-utils:{version}") {
    capabilities {
        requireCapability("de.cronn:test-utils-authorization-test")
    }
}
```

Maven:
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>test-utils</artifactId>
    <version>{version}</version>
    <scope>test</scope>
    <classifier>authorization-test</classifier>
</dependency>
```

> [!WARNING]
> This feature only supports Spring MVC with bearer-token authentication — WebFlux, form login, session cookies, basic auth, mTLS etc. are not supported.
> 
> Be aware of the following additional constraints:
> - Path variables are replaced with a fixed placeholder:
>   - Endpoints whose authorization depends on the variable's *value* (e.g. `@PreAuthorize("#id == authentication.principal.userId")`) may produce incorrect results. 
>   - Regex-constrained variables (e.g. `/users/{id:[0-9]+}`) are rejected outright.
> - No `Accept` header is sent. For applications that vary authorization by media type, only the endpoint matching an absent `Accept` header will be tested.

[validation-file-assertions]: https://github.com/cronn/validation-file-assertions
