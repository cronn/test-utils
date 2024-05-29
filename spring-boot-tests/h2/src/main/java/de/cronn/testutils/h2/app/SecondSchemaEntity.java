package de.cronn.testutils.h2.app;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Table(schema = "second_schema")
@Entity
public class SecondSchemaEntity {

	@Id
	@GeneratedValue(generator = "differentSchemaGenerator", strategy = GenerationType.TABLE)
	@TableGenerator(schema = "second_schema", table = "second_generator", name = "differentSchemaGenerator", allocationSize = 1)
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
