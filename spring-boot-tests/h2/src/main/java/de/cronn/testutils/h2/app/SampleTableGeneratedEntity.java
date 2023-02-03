package de.cronn.testutils.h2.app;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;

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
