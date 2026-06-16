package de.cronn.testutils.jpa.query.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class ChildEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	@Column(nullable = false)
	private String name;

	@ManyToOne(optional = false)
	private SampleEntity parent;

	public void setName(String name) {
		this.name = name;
	}

	public void setParent(SampleEntity parent) {
		this.parent = parent;
	}
}
