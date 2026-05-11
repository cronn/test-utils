package de.cronn.testutils.jpa.query.app;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class SampleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToMany(mappedBy = "parent", cascade = { CascadeType.PERSIST })
	@OrderBy
	private final List<ChildEntity> children = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void addChild(ChildEntity child) {
		child.setParent(this);
		children.add(child);
	}

	public List<ChildEntity> getChildren() {
		return children;
	}
}
