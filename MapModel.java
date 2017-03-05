package com.skuview.importsku.model;

public class MapModel {
	
	private Long skuId;
	private String skuFileName;
	private Long sourcePriority;
	private Long passthroughPriority;
	private String sourceName;
	
	public Long getSkuId() {
		return skuId;
	}
	public void setSkuId(Long skuId) {
		this.skuId = skuId;
	}
	public String getSkuFileName() {
		return skuFileName;
	}
	public void setSkuFileName(String skuFileName) {
		this.skuFileName = skuFileName;
	}
	public Long getSourcePriority() {
		return sourcePriority;
	}
	public void setSourcePriority(Long sourcePriority) {
		this.sourcePriority = sourcePriority;
	}
	public Long getPassthroughPriority() {
		return passthroughPriority;
	}
	public void setPassthroughPriority(Long passthroughPriority) {
		this.passthroughPriority = passthroughPriority;
	}
	public String getSourceName() {
		return sourceName;
	}
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
			
}
