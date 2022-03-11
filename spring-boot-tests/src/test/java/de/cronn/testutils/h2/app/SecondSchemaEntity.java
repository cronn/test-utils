package de.cronn.testutils.h2.app;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

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
