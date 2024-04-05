package de.cronn.testutils.h2;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:testdb;INIT=CREATE SCHEMA IF NOT EXISTS SECOND_SCHEMA;MODE=PostgreSQL",
})
public abstract class H2PostgresModeUtilTest extends H2UtilTest {
}
