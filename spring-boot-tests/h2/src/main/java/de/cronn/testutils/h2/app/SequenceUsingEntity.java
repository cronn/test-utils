package de.cronn.testutils.h2.app;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class SequenceUsingEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "my_sequence_generator")
	@SequenceGenerator(allocationSize = 1, name = "my_sequence_generator")
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
