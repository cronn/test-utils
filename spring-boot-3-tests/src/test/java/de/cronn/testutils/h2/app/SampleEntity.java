package de.cronn.testutils.h2.app;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class SampleEntity {

	@Id
	@GeneratedValue(generator = "my_explicit_generator")
	@SequenceGenerator(name = "my_explicit_generator", allocationSize = 1, sequenceName = "SAMPLE_ENTITY_SEQ")
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
