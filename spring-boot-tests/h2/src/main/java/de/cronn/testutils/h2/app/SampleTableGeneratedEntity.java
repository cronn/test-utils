package de.cronn.testutils.h2.app;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

@Entity
public class SampleTableGeneratedEntity {

	@Id
	@GeneratedValue(generator = "sampleGenerator", strategy = GenerationType.TABLE)
	@TableGenerator(table = "sample_generator", name = "sampleGenerator", allocationSize = 1)
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
