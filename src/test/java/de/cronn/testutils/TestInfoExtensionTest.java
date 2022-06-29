package de.cronn.testutils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;

class TestInfoExtensionTest {

	@RegisterExtension
	private TestInfoExtension testInfoExtension = new TestInfoExtension();

	@Test
	void shouldContainTheSameDataAsTestInfo(TestInfo testInfo) {
		Assertions.assertThat(testInfoExtension)
			.returns(testInfo.getDisplayName(), TestInfoExtension::getDisplayName)
			.returns(testInfo.getTestClass(), TestInfoExtension::getTestClass)
			.returns(testInfo.getTestMethod(), TestInfoExtension::getTestMethod)
			.returns(testInfo.getTags(), TestInfoExtension::getTags);
	}
}
