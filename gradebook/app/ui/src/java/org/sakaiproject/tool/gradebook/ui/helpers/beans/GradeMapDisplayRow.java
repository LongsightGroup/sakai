package org.sakaiproject.tool.gradebook.ui.helpers.beans;

import java.io.Serializable;

public class GradeMapDisplayRow implements Serializable {
	private static final long serialVersionUID = 1L;

	private Double bottomValue;
	private Double topValue;
	private String grade;

	public GradeMapDisplayRow(String grade, Double bottomValue,
			Double topValue) {
		this.bottomValue = bottomValue;
		this.topValue = topValue;
		this.grade = grade;
	}

	public String getGrade() {
		return grade;
	}

	public Double getTopValue() {
		return topValue;
	}

	public void setTopValue(Double value) {
		this.topValue = value;
	}

	public Double getBottomValue() {
		return bottomValue;
	}

	public void setBottomValue(Double value) {
		this.bottomValue = value;
	}

}
