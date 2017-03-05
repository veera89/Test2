package com.skuview.importsku.model;

import java.util.HashMap;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 405146
 *
 */
@Document(collection = "SKU_INFO_MEDIA")
public class SKUInfoMedia {

	@Id
	private long id;
	private long skuInfoId;
	private String default_image;
	private HashMap<String,String> imageMapLocal;
	private HashMap<String,String> imageMapInternet;
	private HashMap<String,String> imageMapIntranet;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getSkuInfoId() {
		return skuInfoId;
	}
	public void setSkuInfoId(long skuInfoId) {
		this.skuInfoId = skuInfoId;
	}
	
	public String getDefault_image() {
		return default_image;
	}
	public void setDefault_image(String default_image) {
		this.default_image = default_image;
	}
	public HashMap<String, String> getImageMapLocal() {
		return imageMapLocal;
	}
	public void setImageMapLocal(HashMap<String, String> imageMapLocal) {
		this.imageMapLocal = imageMapLocal;
	}
	public HashMap<String, String> getImageMapInternet() {
		return imageMapInternet;
	}
	public void setImageMapInternet(HashMap<String, String> imageMapInternet) {
		this.imageMapInternet = imageMapInternet;
	}
	public HashMap<String, String> getImageMapIntranet() {
		return imageMapIntranet;
	}
	public void setImageMapIntranet(HashMap<String, String> imageMapIntranet) {
		this.imageMapIntranet = imageMapIntranet;
	}
	
	
	
}