package de.cronn.testutils.postgres.app;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class SampleEntity {

	@Id
	@GeneratedValue
	private Long id;

	public Long getId() {
		return id;
	}

}
