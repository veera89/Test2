package com.skuview.importsku.model;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "REF_STATUS")
public class StatusReference {

	@Id
	private long id;
	private long statusId;
	private String statusName;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getStatusId() {
		return statusId;
	}
	public void setStatusId(long statusId) {
		this.statusId = statusId;
	}
	public String getStatusName() {
		return statusName;
	}
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}
	@Override
	public String toString() {
		return "StatusReference [id=" + id + ", statusId=" + statusId
				+ ", statusName=" + statusName + "]";
	}
	
}
