package de.cronn.testutils.jpa.query;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.cronn.testutils.h2.H2Util;
import de.cronn.testutils.jpa.query.app.Application;

@SpringBootTest(classes = Application.class)
public abstract class BaseIntegrationTest {

	@Autowired
	H2Util h2Util;

	@BeforeEach
	void resetDatabase() {
		h2Util.resetDatabase();
	}

}
