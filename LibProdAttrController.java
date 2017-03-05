package com.skuview.common.controller;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.skuview.category.dao.constants.Constants;
import com.skuview.category.model.CategoryAttributeRelation;
import com.skuview.common.command.LibPrdCategoryCO;
import com.skuview.common.command.LibProdAttrCO;
import com.skuview.common.command.ProdAttrSearchCO;
import com.skuview.common.model.AttributeMapping;
import com.skuview.common.model.LibProdAttrDisplayModel;
import com.skuview.common.model.LibProdAttrModel;
import com.skuview.common.model.ListManagementModel;
import com.skuview.common.model.SearchAttributeModel;
import com.skuview.common.util.DownloadFileUtil;
import com.skuview.common.utli.DropdownValue;
import com.skuview.common.utli.SkuViewAttrPageListHolder;
import com.skuview.controller.constants.ControllerConstants;
import com.skuview.exception.CustomGenericException;
import com.skuview.exception.UniqueDisplayNameViolation;
import com.skuview.exception.UniqueNameViolation;
import com.skuview.export.bo.ExportBO;
import com.skuview.libcategory.bo.LibPrdCategoryBO;
import com.skuview.libcategory.model.LibPrdCategoryModel;
import com.skuview.libprodattr.LibProdAttrBO;
import com.skuview.listmanagement.ListManagementBO;
import com.skuview.service.bo.RoleService;
import com.skuview.user.model.Roles;
import com.skuview.user.model.UserDetails;

/**
 * This Controller class is used to handle the Library Attribute creation, Edition, Search and Map 
 * 
 * @author 277232
 *
 */
@Controller
public class LibProdAttrController {
	
	private static final Logger logger = LoggerFactory.getLogger(LibProdAttrController.class);
	
	Map<String, String> searchByMap = null;
	Map<String, String> filterByMap = null;
	Map<String, String> isMapMap = null;
	Map<String, String> functionalAreaMap = null;
	Map<String, String> categoryMap = null;
	Map<String, String> sourceClassMap = null;
	Map<String, String> qualityClassMap = null;
	Map<String, String> typeMap = null;
	Map< DropdownValue, String> dataTypeMap = null;
	Map<String, String> uomMap = null;
	Map<String, String> typeOfDataMap = null;
	Map<String, String> hasMultiValueMap = null;
	Map<String, String> funClassMap = null;	
	Map<String, String> relatedToCatMap = null;
	Map<Long, String> activeAttrMap = null;
	long deleteAttributeId=0;
	ProdAttrSearchCO deleteAttrSearchCO;
	Map<String, AttributeMapping> attrCache;
	
	@Autowired
	public LibProdAttrBO libProdAttrBO; 
	
	@Autowired
	SearchAttributeModel searchAttrModel;
	
	@Autowired
	ListManagementBO listManagementBO;
	
	@Autowired
	public RoleService roleBO;
	
	@Autowired
    @Qualifier("libProdAttrValidator")
    private Validator libProdAttrValidator;

    @InitBinder("skuViewLibProdAttrCO")
    private void initBinder(WebDataBinder binder) {
        binder.setValidator(libProdAttrValidator);
    }
    
    @Autowired
	public LibPrdCategoryBO libPrdCategoryBO;
    
	@Autowired
	ExportBO exportBO;
	
	@Autowired
	public DownloadFileUtil downloadFileUtil;
	
	LibProdAttrCO skuViewLibProdAttrCO = null;
	LibProdAttrModel libProdAttrModel;
	//List<LibProdAttrDisplayModel> prodDispModelList = null;
	private long totalCount = 0;
	
	SkuViewAttrPageListHolder pagedListHolder = null;
	
	ProdAttrSearchCO prodSearchCriteria = new ProdAttrSearchCO();
	
	private boolean flag  = false;
	
	public Date createdOn;
	public String createdBy;
	
	/**
	 * This method will be called while screen is loading. By default it will display all the attributes in the system
	 * 
	 * @param model		It holds the values to be populated in JSP
	 * @param request	Request object
	 * @return			Returns to Libraryattributesearch.jsp
	 */
	@RequestMapping(value = "/libattrsearch", method = RequestMethod.GET)
	public String displayAttributes(@RequestParam(value = "pageNo", required=false) String pageNo,Map<String, Object> model,HttpServletRequest request){
		request.getSession().setAttribute("count", "");
		deleteAttrSearchCO=new ProdAttrSearchCO();
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		initializeFormObject(model);
	/*	List<LibProdAttrDisplayModel> prodDispModelList = null;*/
		try {
			flag = false;
			searchAttrModel.setFilterBy(Constants.EMPTY_SELECTION);
			searchAttrModel.setSearchBy(Constants.EMPTY_SELECTION);
			searchAttrModel.setFunctionalArea(Constants.EMPTY_SELECTION);
			searchAttrModel.setCategory(Constants.EMPTY_SELECTION);
			searchAttrModel.setSourceClass(Constants.EMPTY_SELECTION);
			searchAttrModel.setQualityClass(Constants.EMPTY_SELECTION);
			searchAttrModel.setType(Constants.EMPTY_SELECTION);
			//prodDispModelList = libProdAttrBO.getALLProductAttributeResultList(searchAttrModel);
			if(pageNo != null & pageNo != ""){				
				fetchSearchProdcuts(request, prodSearchCriteria, false);
				formBackObject(model, true, pageNo);
			}else{
				prodSearchCriteria = new ProdAttrSearchCO();
				fetchSearchProdcuts(request, prodSearchCriteria, false);
				formBackObject(model, true, null);
			}
			model.put("label",null);
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		//model.put("prodDispModelList", prodDispModelList);
		return Constants.LIBATTRSEARCH;
	}
	
	/**
	 * This method will display the attributes as per the search criteria
	 * 
	 * @param attrSearchCO		Model object holds the selected value from screen
	 * @param model				Model object holds the values to be populated in jsp
	 * @param request			Request object
	 * @return					Returns to Libraryattributesearch.jsp
	 */
	@RequestMapping(value = "/libattrsearchsubmit", method = RequestMethod.POST, params = "search")
	public String attrSearchSubmit(@ModelAttribute("searchForm") ProdAttrSearchCO attrSearchCO, 
			Map<String, Object> model, HttpServletRequest request) {
		logger.debug(attrSearchCO.toString());
		attrSearchCO.setCategory(Constants.EMPTY_SELECTION);
		deleteAttrSearchCO=attrSearchCO;
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		List<LibProdAttrDisplayModel> prodDispModelList = null;
		try {
			flag = true;
			convertProdAttrSearchCOToModel(searchAttrModel, attrSearchCO);
			fetchSearchProdcuts(request,prodSearchCriteria, false);
			formBackObject(model, true, null);
			formBackObject(attrSearchCO, model);
			
			/*formBackObject(attrSearchCO, model);
			convertProdAttrSearchCOToModel(searchAttrModel, attrSearchCO);
			prodDispModelList = libProdAttrBO.getALLProductAttributeResultList(searchAttrModel);*/
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		model.put("prodDispModelList", prodDispModelList);
		return Constants.LIBATTRSEARCH;
	}
	
	/**
	 * 
	 */
	@RequestMapping(value="/libattrsearchsubmit", method = RequestMethod.POST, params = "export")
	public void download(@ModelAttribute("searchForm") ProdAttrSearchCO attrSearchCO, 
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		
		logger.info("Download in Excel");
		try {
			UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			attrSearchCO.setCategory(Constants.EMPTY_SELECTION);
			String exportFilePath = exportBO.getExportFilePath("ExportFilePath");
			String fileType = Constants.FILE_FORMAT_XLSX;
					
			
			String fileName = "LibraryAttributeList" + "_" +new SimpleDateFormat("yyyyMMddhhmmss'.'").format(new Date()) + fileType;
			
			response.setContentType("text/csv");
			response.setHeader("Content-Disposition", "attachment; filename=\""+fileName+ "\"");
			OutputStream out = response.getOutputStream();
			
			convertProdAttrSearchCOToModel(searchAttrModel, attrSearchCO);
			
			libProdAttrBO.exportSearchData(searchAttrModel, fileName, out, exportFilePath);
			if (fileName != null) {
				 	downloadFileUtil
						.downloadFile(
									request,
									response,
									(exportFilePath + "/" + fileName));
				
			}
			
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		
		
		
	}
	
	/**
	 * This methods helps to convert the Library product attribute search CO object to Library Product attribute search CO object
	 * 
	 * @param searchAttrModel
	 * @param attrSearchCO
	 */
	private void convertProdAttrSearchCOToModel(SearchAttributeModel searchAttrModel, 
			ProdAttrSearchCO attrSearchCO) {
		try {
			BeanUtils.copyProperties(searchAttrModel, attrSearchCO);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is used to load the Product attribute create screen with default values.
	 * 
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	
	@RequestMapping(value = "/libdisplaycreateattribute", method = RequestMethod.GET)
	public String displayCreateAttribute(Map<String,Object> model, HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		initializeFormObjectAttCreation(model);
		model.put(Constants.SUCCESS, null);
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		return Constants.LIBPRODATTRIBUTECREATE;
	}
	

	@SuppressWarnings("unchecked")
	public void loadMapProductAttributes(LibPrdCategoryCO categoryCO, Map<String, Object> model) {
		logger.info("Entering loadMapProductAttributes..");
		try {
			Map<Long, String> availableAttributesMap = new HashMap<Long, String>();
			Map<Long, String> displayMappedAttribute = new HashMap<Long, String>();
			AttributeMapping attributeMapping = null;
			
			//availableAttributesMap = attributeBO.getAllAttributeNames();
			availableAttributesMap = libPrdCategoryBO.getAllClientAttrIdMap();
			
			List<AttributeMapping> attributeMappingAvailAndDisplayList = new ArrayList<AttributeMapping>();
			Set<AttributeMapping> attributeMappingDisplaySet = new HashSet<AttributeMapping>();
			
			if(categoryCO == null) {
				if(availableAttributesMap != null) {
					Iterator iter = availableAttributesMap.entrySet().iterator();
					while(iter.hasNext()) {
						attributeMapping = new AttributeMapping();
						Map.Entry<Long, String> attributes = (Map.Entry<Long, String>)iter.next();
						attributeMapping.setKey((Long)attributes.getKey());
						attributeMapping.setValue((String)attributes.getValue());
						attributeMappingAvailAndDisplayList.add(attributeMapping);
					}
					
				}
				model.put("availableAttributesList", attributeMappingAvailAndDisplayList);
				return;
			}
			
			List<CategoryAttributeRelation> mappedAttributesList = libPrdCategoryBO.getAllMappedAttributes(categoryCO.getId());
			List<CategoryAttributeRelation> mappedParentAttributesList = null;
			
			if(mappedAttributesList == null ) {
				mappedParentAttributesList = libPrdCategoryBO.getAllMappedAttributes(categoryCO.getParentCategoryId());
				
			} else {
				for(CategoryAttributeRelation attributeRelation : mappedAttributesList) {
					if(!attributeRelation.getIsInherited()) {
						displayMappedAttribute.put(attributeRelation.getAttributeId(), attributeRelation.getAttributeName());
					} else {
						availableAttributesMap.remove(attributeRelation.getAttributeId());
					}
				}
			}
			if(mappedParentAttributesList != null) {
				for(CategoryAttributeRelation attributeRelation : mappedParentAttributesList) {
					availableAttributesMap.remove(attributeRelation.getAttributeId());
				}
			}
			
			if(availableAttributesMap != null) {
				Iterator iter1 = availableAttributesMap.entrySet().iterator();
				while(iter1.hasNext()) {
					attributeMapping = new AttributeMapping();
					Map.Entry<Long, String> attributes = (Map.Entry<Long, String>)iter1.next();
					attributeMapping.setKey((Long)attributes.getKey());
					attributeMapping.setValue((String)attributes.getValue());
					attributeMappingAvailAndDisplayList.add(attributeMapping);
				}
			}
			if(displayMappedAttribute != null) {
				Iterator iter = displayMappedAttribute.entrySet().iterator();
				while(iter.hasNext()) {
					attributeMapping = new AttributeMapping();
					Map.Entry<Long, String> attributes = (Map.Entry<Long, String>)iter.next();
					attributeMapping.setKey((Long)attributes.getKey());
					attributeMapping.setValue((String)attributes.getValue());
					attributeMappingDisplaySet.add(attributeMapping);
				}
				
			}
			
			logger.info("Display List Size= "+attributeMappingDisplaySet.size());
			logger.info("Whole List Size= "+attributeMappingAvailAndDisplayList.size());
			
			categoryCO.setAttributes(attributeMappingDisplaySet);
			
			
			attrCache = new HashMap<String, AttributeMapping>();
			for (AttributeMapping attrMapping : attributeMappingAvailAndDisplayList) {
				attrCache.put(String.valueOf(attrMapping.getKey()), attrMapping);
			}
			
			model.put("categoryCO", categoryCO);
			model.put("attributeMappingDisplaySet", attributeMappingDisplaySet);
			model.put("availableAttributesList", attributeMappingAvailAndDisplayList);
			logger.info("Exiting loadMapProductAttributes..");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param model
	 * ** To construct the CategoryCO object for Tree
	 */
public void setCategoryTreeData(Map<String, Object> model){
		
		Map<Long, LibPrdCategoryCO> rootMap = new HashMap<Long, LibPrdCategoryCO>();
		Map<Long, LibPrdCategoryCO> hasValueMap = new HashMap<Long, LibPrdCategoryCO>();
		
		try {
			List<LibPrdCategoryModel> categoryModels = libPrdCategoryBO.getAllClientCategories();
			
			List<LibPrdCategoryCO> categoryCOs = convertCategoryModelsToCategoryCOs(categoryModels, null);
			
			/*To set the Root Nodes*/
			for(LibPrdCategoryCO co : categoryCOs){
				if(co.getIsRoot().equals(ControllerConstants.YES)){
					rootMap.put(co.getId(), co);
				}else if(co.getHasValue().equals(ControllerConstants.YES) && co.getIsRoot().equals(ControllerConstants.NO)){
					hasValueMap.put(co.getId(), co);
				}
					
			}
			
			/*To set the Child to Parent using ParentCategoryId*/
			for(LibPrdCategoryCO co : categoryCOs){
				if(rootMap.containsKey(co.getParentCategoryId())){
					LibPrdCategoryCO parentCO = rootMap.get(co.getParentCategoryId());
					Map<Long, LibPrdCategoryCO> childList = parentCO.getChildCategories() == null ? new HashMap<Long, LibPrdCategoryCO>() : parentCO.getChildCategories();
					childList.put(co.getId(), co);
					parentCO.setChildCategories(childList);
					rootMap.put(co.getParentCategoryId(), parentCO);
				}else if(hasValueMap.containsKey(co.getParentCategoryId())){
					LibPrdCategoryCO parentCO = hasValueMap.get(co.getParentCategoryId());
					Map<Long, LibPrdCategoryCO> childList = parentCO.getChildCategories() == null ? new HashMap<Long, LibPrdCategoryCO>() : parentCO.getChildCategories();
					childList.put(co.getId(), co);
					parentCO.setChildCategories(childList);
					hasValueMap.put(co.getParentCategoryId(), parentCO);
				}
			}
			
			/*To reiterate and remove the middle level categories based on hasValue flag*/
			while(hasValueMap.size() > 0){
				List<Long> toRemove = new ArrayList<Long>();
				for(Long hasValueId : hasValueMap.keySet()){
					LibPrdCategoryCO hasValueCO = hasValueMap.get(hasValueId);
					Map<Long, LibPrdCategoryCO> hasValueChildList = hasValueCO.getChildCategories();
					Boolean hasChildDependancy = false;
					for(Long id : hasValueChildList.keySet()){
						LibPrdCategoryCO child = hasValueChildList.get(id);
						if(hasValueMap.containsKey(child.getId())){
							hasChildDependancy = true;
						}
					}
					if(!hasChildDependancy){
						if(rootMap.containsKey(hasValueCO.getParentCategoryId())){
							LibPrdCategoryCO parentCO = rootMap.get(hasValueCO.getParentCategoryId());
							Map<Long, LibPrdCategoryCO> childList = parentCO.getChildCategories();
							childList.put(hasValueCO.getId(), hasValueCO);
							parentCO.setChildCategories(childList);
							rootMap.put(hasValueCO.getParentCategoryId(), parentCO);
							toRemove.add(hasValueCO.getId());
						}else if(hasValueMap.containsKey(hasValueCO.getParentCategoryId())){
							LibPrdCategoryCO parentCO = hasValueMap.get(hasValueCO.getParentCategoryId());
							Map<Long, LibPrdCategoryCO> childList = parentCO.getChildCategories();
							childList.put(hasValueCO.getId(), hasValueCO);
							parentCO.setChildCategories(childList);
							hasValueMap.put(hasValueCO.getParentCategoryId(), parentCO);
							toRemove.add(hasValueCO.getId());
						}
					}
					
				}
				for(Long id : toRemove){
					hasValueMap.remove(id);
				}
			}
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}			
		model.put("childCategories", rootMap);
		
	}
	
	/**
	 * @param categoryModels
	 * @param categoryMap
	 * @return
	 * @throws Exception
	 * ** To convert the list of Category Models to Category COs
	 */

private List<LibPrdCategoryCO> convertCategoryModelsToCategoryCOs(List<LibPrdCategoryModel> categoryModels, Map<Long, LibPrdCategoryModel> categoryMap) {
	List<LibPrdCategoryCO> categoryCOs = new ArrayList<LibPrdCategoryCO>();
	try {
		for(LibPrdCategoryModel model : categoryModels){
			LibPrdCategoryCO categoryCO = new LibPrdCategoryCO();
			BeanUtils.copyProperties(categoryCO, model);
			convertBooleanToString(model, categoryCO);
			if(model.getParentCategoryId() > 0){
				categoryCO.setParentCategory(getCategoryName(model.getParentCategoryId()));
			}
			if(model.getSkuViewId()>0 && categoryMap != null){
				LibPrdCategoryCO skuViewLibraryCO = new LibPrdCategoryCO();
				BeanUtils.copyProperties(skuViewLibraryCO, categoryMap.get(model.getSkuViewId()));
				convertBooleanToString(categoryMap.get(model.getSkuViewId()), skuViewLibraryCO);
				if((categoryMap.get(model.getSkuViewId())).getParentCategoryId() > 0){
					skuViewLibraryCO.setParentCategory(getCategoryName((categoryMap.get(model.getSkuViewId())).getParentCategoryId()));
				}
				categoryCO.setSkuViewLibraryCO(skuViewLibraryCO);
			}
			categoryCOs.add(categoryCO);
		}
	}catch(Exception e) {
		throw new CustomGenericException(e.getMessage());
	}
	return categoryCOs;
}

/**
 * @param model
 * @param LibPrdCategoryCO
 * ** To convert the Boolean values as String to set in CO
 */
private void convertBooleanToString(LibPrdCategoryModel model, LibPrdCategoryCO categoryCO){
	if(model.isRoot()){
		categoryCO.setIsRoot(ControllerConstants.YES);
	}else{
		categoryCO.setIsRoot(ControllerConstants.NO);
	}
	if(model.isActive()){
		categoryCO.setIsActive(ControllerConstants.YES);
	}else{
		categoryCO.setIsActive(ControllerConstants.NO);
	}
	if(model.isMapped()){
		categoryCO.setIsMapped(ControllerConstants.YES);
	}else{
		categoryCO.setIsMapped(ControllerConstants.NO);
	}
	if(model.isHasValue()){
		categoryCO.setHasValue(ControllerConstants.YES);
	}else{
		categoryCO.setHasValue(ControllerConstants.NO);
	}
}

private String getCategoryName(long id){
	String name;
	try {
		name = libPrdCategoryBO.getCategoryName(id);
	}catch(Exception exception) {
		throw new CustomGenericException(exception.getMessage());
	}
	return name; 
}


	@InitBinder
    protected void InitBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Set.class, "attributes", new CustomCollectionEditor(Set.class)
          {
            @Override
            protected Object convertElement(Object element)
            {
            	if (element instanceof AttributeMapping) {
    				return element;
    			}
    			if (element instanceof String) {
    				AttributeMapping attrMap = attrCache.get(element);
    				return attrMap;
    			}
    			if (element instanceof Map.Entry) {
    				//AttributeMapping staff = attrCache.get(element);
    				return element;
    			}
    			return null;
            }
          });
	}
	
	/**
	 * This method is used to create the new attribute
	 * 
	 * @param skuViewProductAttributeCO
	 * @param bindingResult
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	
	@RequestMapping(value = "/libcreateprod", method = RequestMethod.POST, params ="save")
	public String createProductAttribute(@ModelAttribute("skuViewLibProdAttrCO") @Validated LibProdAttrCO skuViewLibProdAttrCO, BindingResult bindingResult, Map<String, Object> model, HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		setProductAttributeCO(skuViewLibProdAttrCO);
		
		 if (bindingResult.hasErrors()) {
	            logger.info("Returning "+Constants.LIBPRODATTRIBUTECREATE+".jsp page");
	            retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
	            loadMapProductAttributes(new LibPrdCategoryCO(), model);
	    		setCategoryTreeData(model);
	            return Constants.LIBPRODATTRIBUTECREATE;
	        }
		boolean success = false;
		 try {
			 skuViewLibProdAttrCO.setId(0);
			 LibProdAttrModel  libProdAttrModel = convertProductCOtoModel(skuViewLibProdAttrCO);
			 libProdAttrModel.setName(libProdAttrModel.getName().trim());
			 success = libProdAttrBO.createProductAttributeDetail(libProdAttrModel,userSess.getLoginName());			 
			 model.put(Constants.SUCCESS, success);
			 if(success) {
				 skuViewLibProdAttrCO.setId(libProdAttrModel.getId());
				 model.put("message", "Product Attribute had been created successfully");
			 } else{
					model.put("message", "Product Attribute is failed to create");
				}
			 skuViewLibProdAttrCO.setDataType(skuViewLibProdAttrCO.getDataType().concat("-"));
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
		 } catch(UniqueNameViolation exception) {
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 model.put(Constants.AVAILABLE,Constants.NAMEEXIST);
			 model.put(Constants.SUCCESS, null); 
		 } catch(UniqueDisplayNameViolation exception){
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 model.put(Constants.DISPLAYNAMEAVAILABLE,Constants.DISPLAYNAMEEXIST);
			 model.put(Constants.SUCCESS, null);
		 } catch(Exception exception) {
			 throw new CustomGenericException(exception.getMessage());
		 }
		 loadMapProductAttributes(new LibPrdCategoryCO(), model);
 		setCategoryTreeData(model);
		 if(skuViewLibProdAttrCO.getId()>0) {
			 return Constants.LIBPRODATTRIBUTEEDIT;
		 }
		return Constants.LIBPRODATTRIBUTECREATE;
	}
		
	@RequestMapping(value = "/libcreateprod", method = RequestMethod.POST, params ="cancel")
	public String cancelProductAttribute(HttpServletRequest request) {
		
		return "redirect:/libdisplaycreateattribute";
	}

	/**
	 * This method will fetch the product attribute
	 * 
	 * @param prodAttrCO
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	
	@RequestMapping(value = "/libcreateprod", method = RequestMethod.POST, params ="go")
	public String fetchProductAttribute(@ModelAttribute("skuViewLibProdAttrCO") LibProdAttrCO libProdAttrCO,Map<String, Object> model, HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
 		
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		skuViewLibProdAttrCO= commonProdAttrCopy(model, libProdAttrCO);
		skuViewLibProdAttrCO.setName(null);
		 skuViewLibProdAttrCO.setDataType(skuViewLibProdAttrCO.getDataType().concat("-"));
		model.put(Constants.SKUVIEWLIBPRODUCTATTRCO, skuViewLibProdAttrCO);
		model.put(Constants.SUCCESS, null);
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		return Constants.LIBPRODATTRIBUTECREATE;
	}
	/**
	 * This method will display the Edit Library Attribute
	 * 
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@RequestMapping(value = "/libeditattrdisplay", method = RequestMethod.GET)
	public String displayUpdateAttribute(Map<String,Object> model, HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		initializeFormObjectAttCreation(model);
		model.put(Constants.SUCCESS, null);
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		return Constants.LIBPRODATTRIBUTEEDIT;
	}
	
	/**
	 * This method will display the Edit Library Attribute based on search criteria
	 * 
	 * @param model
	 * @param id
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@RequestMapping(value = "/libeditattr", method = RequestMethod.GET)
	public String displayUpdateAttrFromSearch(Map<String,Object> model, @RequestParam(value="id",required=true) String id,
			@RequestParam(value="searchBy") String searchBy, 
			@RequestParam(value="filterBy") String filterBy, 
			@RequestParam(value="keyWord") String keyWord,
			@RequestParam(value="functionalArea") String functionalArea, 
			@RequestParam(value="category") String category, 
			@RequestParam(value="sourceClass") String sourceClass, 
			@RequestParam(value="qualityClass") String qualityClass, 
			@RequestParam(value="type") String type,
			
			HttpServletRequest request) 
			throws IllegalAccessException, InvocationTargetException {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		initializeFormObjectAttCreation(model);
		skuViewLibProdAttrCO = new LibProdAttrCO();
		SearchAttributeModel searchAttrModel=new SearchAttributeModel();
		searchAttrModel.setSearchBy(searchBy);
		searchAttrModel.setFilterBy(filterBy);
		searchAttrModel.setKeyWord(keyWord);
		searchAttrModel.setFunctionalArea(functionalArea);
		searchAttrModel.setCategory(category);
		searchAttrModel.setSourceClass(sourceClass);
		searchAttrModel.setQualityClass(qualityClass);
		searchAttrModel.setType(type);
		try {
			LibProdAttrModel libProdAttrModel = libProdAttrBO.findAttribute(Integer.parseInt(id));
			createdOn = libProdAttrModel.getCreatedOn();
			createdBy = libProdAttrModel.getCreatedBy();
			BeanUtils.copyProperties(skuViewLibProdAttrCO, libProdAttrModel);
		} catch (NumberFormatException e) {
			throw new CustomGenericException(e.getMessage());
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		model.put(Constants.SEARCHATTRMODEL, searchAttrModel);
		skuViewLibProdAttrCO.setDataType(skuViewLibProdAttrCO.getDataType().concat("-"));
		model.put(Constants.SKUVIEWLIBPRODUCTATTRCO, skuViewLibProdAttrCO);
		model.put(Constants.SUCCESS, null);
		return Constants.LIBPRODATTRIBUTEEDIT;
	}
	
	/**
	 * @param skuViewLibProdAttrCO
	 * @param bindingResult
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@RequestMapping(value = "/libeditattrsubmit", method = RequestMethod.POST, params = "update")
	public String updateProductAttribute(
			@ModelAttribute("skuViewLibProdAttrCO") @Validated LibProdAttrCO skuViewLibProdAttrCO,
			BindingResult bindingResult, Map<String, Object> model,HttpServletRequest request)
			throws IllegalAccessException, InvocationTargetException {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		setProductAttributeCO(skuViewLibProdAttrCO);
		
		if (bindingResult.hasErrors()) {
			logger.info("Returning "+Constants.PRODATTRIBUTEUPDATE+".jsp page");
			retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			loadMapProductAttributes(new LibPrdCategoryCO(), model);
    		setCategoryTreeData(model);
			return Constants.LIBPRODATTRIBUTEEDIT;
		}
		boolean success = false;
		try {			
			retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			LibProdAttrModel libProdAttrModel = convertProductCOtoModel(skuViewLibProdAttrCO);
			 libProdAttrModel.setName(libProdAttrModel.getName().trim());
			 libProdAttrModel.setCreatedOn(createdOn);
			 libProdAttrModel.setCreatedBy(createdBy);
			 success = libProdAttrBO.updateClientAttr(libProdAttrModel,userSess.getLoginName());
			 skuViewLibProdAttrCO.setDataType(skuViewLibProdAttrCO.getDataType().concat("-"));
			 model.put(Constants.SUCCESS, success);
		} catch(UniqueNameViolation exception) {
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 model.put(Constants.AVAILABLE,Constants.NAMEEXIST);
			 model.put(Constants.SUCCESS, null); 
		 } catch(UniqueDisplayNameViolation exception){
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 model.put(Constants.DISPLAYNAMEAVAILABLE,Constants.DISPLAYNAMEEXIST);
			 model.put(Constants.SUCCESS, null);
		 } catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		return Constants.LIBPRODATTRIBUTEEDIT;
	}
	
	/**
	 * @param libProdAttrCO
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@RequestMapping(value = "/libeditattrsubmit", method = RequestMethod.POST, params ="go")
	public String fetchProductAttributeUpdate(@ModelAttribute("skuViewLibProdAttrCO") LibProdAttrCO libProdAttrCO,
			Map<String, Object> model, HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
		
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		skuViewLibProdAttrCO= commonProdAttrCopy(model, libProdAttrCO);
		model.put(Constants.SKUVIEWLIBPRODUCTATTRCO, skuViewLibProdAttrCO);
		model.put(Constants.SUCCESS, null);
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		return Constants.LIBPRODATTRIBUTEEDIT;
	}
	
	@RequestMapping(value = "/libeditattrsubmit", method = RequestMethod.POST, params ="cancel")
	public String cancelUpdateAttribute(HttpServletRequest request) {
		return "redirect:/libdisplaycreateattribute";
	}
	
	/**
	 * This method will display the attributes when doing copy of existing attributes
	 * 
	 * @param model
	 * @param id
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
		
	@RequestMapping(value = "/libcopyattribute", method = RequestMethod.GET)
	public String displayCopyAttrFromSearch(Map<String,Object> model, @RequestParam(value="id",required=true) String id, HttpServletRequest request) 
			throws IllegalAccessException, InvocationTargetException {
		
		try {
			UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			initializeFormObjectAttCreation(model);
			skuViewLibProdAttrCO = new LibProdAttrCO();
			LibProdAttrModel libProdAttrModel = libProdAttrBO.findAttribute(Integer.parseInt(id));
			BeanUtils.copyProperties(skuViewLibProdAttrCO, libProdAttrModel);
			skuViewLibProdAttrCO.setName(skuViewLibProdAttrCO.getName());
		} catch (NumberFormatException e) {
			throw new CustomGenericException(e.getMessage());
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		skuViewLibProdAttrCO.setDataType(skuViewLibProdAttrCO.getDataType().concat("-"));
		model.put(Constants.SKUVIEWLIBPRODUCTATTRCO, skuViewLibProdAttrCO);
		model.put(Constants.SUCCESS, null);		
		return Constants.LIBPRODATTRIBUTECOPY;
	}
	/**
	 * This method is used to copy the product attribute and save into system
	 * 
	 * @param skuViewProductAttributeCO
	 * @param bindingResult
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	
	@RequestMapping(value = "/libcopyattributesave", method = RequestMethod.POST, params = "save")
	public String copyProductAttribute(
			@ModelAttribute("skuViewLibProdAttrCO") @Validated LibProdAttrCO  skuViewLibProdAttrCO,
			BindingResult bindingResult, Map<String, Object> model,HttpServletRequest request)
			throws IllegalAccessException, InvocationTargetException {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		setProductAttributeCO(skuViewLibProdAttrCO);
		
		if (bindingResult.hasErrors()) {
			logger.info("Returning "+Constants.LIBPRODATTRIBUTECOPY+".jsp page");
			retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			loadMapProductAttributes(new LibPrdCategoryCO(), model);
    		setCategoryTreeData(model);
			return Constants.LIBPRODATTRIBUTECOPY;
		}
		 boolean success = false;
		 try {
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 LibProdAttrModel libProductAttrModel = convertProductCOtoModel(skuViewLibProdAttrCO);
			 libProductAttrModel.setName(libProductAttrModel.getName().trim());
			 success = libProdAttrBO.createProductAttributeDetail(libProductAttrModel,userSess.getLoginName());
			 model.put(Constants.SUCCESS, success);
			 if(success) {
				 skuViewLibProdAttrCO.setId(libProductAttrModel.getId());
				 model.put("message", "Product Attribute had been created successfully");
			 } else{
					model.put("message", "Product Attribute is failed to create");
				}
			 skuViewLibProdAttrCO.setDataType(skuViewLibProdAttrCO.getDataType().concat("-"));
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 
		 } catch(UniqueNameViolation exception) {
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 model.put(Constants.AVAILABLE,Constants.NAMEEXIST);
			 model.put(Constants.SUCCESS, null); 
		 } catch(UniqueDisplayNameViolation exception){
			 retrieveFormObjectAttCreation(model, skuViewLibProdAttrCO);
			 model.put(Constants.DISPLAYNAMEAVAILABLE,Constants.DISPLAYNAMEEXIST);
			 model.put(Constants.SUCCESS, null);
		 } catch(Exception exception) {
			 throw new CustomGenericException(exception.getMessage());
		 }
		 loadMapProductAttributes(new LibPrdCategoryCO(), model);
 		setCategoryTreeData(model);
		 if(skuViewLibProdAttrCO.getId()>0) {
			 return Constants.LIBPRODATTRIBUTEEDIT;
		 }
		return Constants.LIBPRODATTRIBUTECOPY;
	}
	
	/**
	 * Canceling the copy attribute functionality
	 * 
	 * @param model
	 * @param request
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	
	@RequestMapping(value = "/libcopyattributesave",  method = RequestMethod.POST, params ="cancel")
	public String displayCopyAttribute(Map<String,Object> model,HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		initializeFormObjectAttCreation(model);
		model.put(Constants.SUCCESS, null);
		loadMapProductAttributes(new LibPrdCategoryCO(), model);
		setCategoryTreeData(model);
		return Constants.LIBPRODATTRIBUTECOPY;
	}
	
	private LibProdAttrCO commonProdAttrCopy(Map<String, Object> model, 
			LibProdAttrCO skuViewLibProdAttrCO) throws IllegalAccessException, InvocationTargetException {
				
		try {
			initializeFormObjectAttCreation(model);		
			LibProdAttrModel libProdAttrModel = libProdAttrBO.findAttribute((int) (skuViewLibProdAttrCO.getId()));
			BeanUtils.copyProperties(skuViewLibProdAttrCO, libProdAttrModel);
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}		
		return skuViewLibProdAttrCO;		
	}
		
	/**
	 * This method will retain the attributes as per the search criteria after clicking cancel in edit screen
	 * 
	 * @param attrSearchCO		Model object holds the selected value from screen
	 * @param model				Model object holds the values to be populated in jsp
	 * @param request			Request object
	 * @return					Returns to Libraryattributesearch.jsp
	 */
	
	@RequestMapping(value = "/libattreditsearchsubmit", method = RequestMethod.GET)
	public String attrSearchSubmit1(Map<String, Object> model, 
			@RequestParam(value="searchBy") String searchBy, 
			@RequestParam(value="filterBy") String filterBy, 
			@RequestParam(value="keyWord") String keyWord,
			@RequestParam(value="functionalArea") String functionalArea, 
			@RequestParam(value="category") String category, 
			@RequestParam(value="sourceClass") String sourceClass, 
			@RequestParam(value="qualityClass") String qualityClass, 
			@RequestParam(value="type") String type, 
			HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {								
		
		ProdAttrSearchCO attrSearchCO = new ProdAttrSearchCO();
		
		attrSearchCO.setSearchBy(searchBy);
		attrSearchCO.setFilterBy(filterBy);
		attrSearchCO.setKeyWord(keyWord);
		attrSearchCO.setFunctionalArea(functionalArea);
		attrSearchCO.setCategory(Constants.EMPTY_SELECTION);
		attrSearchCO.setSourceClass(sourceClass);
		attrSearchCO.setQualityClass(qualityClass);
		attrSearchCO.setType(type);
	
		logger.debug(attrSearchCO.toString());
		deleteAttrSearchCO=attrSearchCO;
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		List<LibProdAttrDisplayModel> prodDispModelList = null;
		try {
			formBackObject(attrSearchCO, model);
			convertProdAttrSearchCOToModel(searchAttrModel, attrSearchCO);
			prodDispModelList = libProdAttrBO.getALLProductAttributeResultList(searchAttrModel);
			flag = true;
			convertProdAttrSearchCOToModel(searchAttrModel, attrSearchCO);
			fetchSearchProdcuts(request,prodSearchCriteria, false);
			formBackObject(model, true, null);
			formBackObject(attrSearchCO, model);
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		model.put("prodDispModelList", prodDispModelList);
		return Constants.LIBATTRSEARCH;
	}
	
	/**
	 * This method is used to delete the product attribute
	 * 
	 * @return
	 */
	@RequestMapping(value = "/libdeleteprod", method = RequestMethod.POST)
	public String deleteProductAttributes() {
		String productId="";
		try {
			boolean productAttributesDeleted = libProdAttrBO.deleteProductAttributeDetail(productId);
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		return "productAttribute";
	}
	
	private LibProdAttrModel convertProductCOtoModel(LibProdAttrCO skuViewLibProdAttrCO) throws IllegalAccessException, InvocationTargetException {
		LibProdAttrModel libProdAttrModel = new LibProdAttrModel();
		BeanUtils.copyProperties(libProdAttrModel, skuViewLibProdAttrCO);
		
		return libProdAttrModel;
	}
	
	public void setProductAttributeCO(LibProdAttrCO skuViewLibProdAttrCO)
	{
		String dataType = skuViewLibProdAttrCO.getDataType();
		if(dataType!=null)
		{
			String[] parts = dataType.split("-");
			String part1 = parts[0]; 
			skuViewLibProdAttrCO.setDataType(part1);
		}
	}
	
	/**
	 * Initializing the form object to product attribute search screen
	 * 
	 * @param model
	 */
	public void initializeFormObject(Map<String, Object> model) {
		ProdAttrSearchCO searchForm = new ProdAttrSearchCO();
		model.put(Constants.PRODATTRSEARCHFORM, searchForm);
		
		searchByMap = getSearchByMap();
		model.put(Constants.SEARCHBYDD, searchByMap);

		filterByMap = getFilterByMap();
		model.put(Constants.FILTERBYDD, filterByMap);
		
		isMapMap = getIsMapMap();
		model.put(Constants.ISMAPDD, isMapMap);
		
		functionalAreaMap = getFunctionalAreaMap();
		model.put(Constants.FUNCTIONALAREADD, functionalAreaMap);
		
		categoryMap = getCategoryMap();
		model.put(Constants.CATEGORYMAPDD, categoryMap);
		
		sourceClassMap = getSourceClassMap();
		model.put(Constants.SOURCECLASSMAPDD, sourceClassMap);
		
		qualityClassMap = getQualityClassMap();
		model.put(Constants.QUALITYCLASSDD, qualityClassMap);
		
		typeMap = getTypeMap();
		model.put(Constants.TYPEDD, typeMap);
		
	}
	
	/**
	 * Restoring the form object to product attribute search screen
	 * 
	 * @param attrSearchCO
	 * @param model
	 */
	private void formBackObject(ProdAttrSearchCO attrSearchCO, Map<String, Object> model) {
		
		ProdAttrSearchCO searchForm = attrSearchCO;
		model.put(Constants.PRODATTRSEARCHFORM, searchForm);
		
		searchByMap = getSearchByMap();
		model.put(Constants.SEARCHBYDD, searchByMap);

		filterByMap = getFilterByMap();
		model.put(Constants.FILTERBYDD, filterByMap);
		
		isMapMap = getIsMapMap();
		model.put(Constants.ISMAPDD, isMapMap);
		
		functionalAreaMap = getFunctionalAreaMap();
		model.put(Constants.FUNCTIONALAREADD, functionalAreaMap);
		
		categoryMap = getCategoryMap();
		model.put(Constants.CATEGORYMAPDD, categoryMap);
		
		sourceClassMap = getSourceClassMap();
		model.put(Constants.SOURCECLASSMAPDD, sourceClassMap);
		
		qualityClassMap = getQualityClassMap();
		model.put(Constants.QUALITYCLASSDD, qualityClassMap);
		
		typeMap = getTypeMap();
		model.put(Constants.TYPEDD, typeMap);
		
	}
	
	private Map<String, String> getSearchByMap() {
		searchByMap = new LinkedHashMap<String, String>();	
		searchByMap.put("all", "--Select--");
		searchByMap.put("name", "Attribute name");
		searchByMap.put("typeOfData", "Type of data");
		searchByMap.put("dataType", "Datatype");
		return searchByMap;
	}
	
	private Map<String, String> getFilterByMap() {
		filterByMap = new LinkedHashMap<String, String>();
		//filterByMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeFilterBy");
		for(ListManagementModel listModel:listOfModels){
			filterByMap.put(listModel.getListValue(), listModel.getListValue());
		}
				
		return filterByMap;
	}
	
	private Map<String, String> getIsMapMap() {
		isMapMap = new LinkedHashMap<String, String>();
		isMapMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeIsMap");
		for(ListManagementModel listModel:listOfModels){
			isMapMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		return isMapMap;
	}
	
	private Map<String, String> getFunctionalAreaMap() {
		functionalAreaMap = new LinkedHashMap<String, String>();
		functionalAreaMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeFunctionalArea");
		for(ListManagementModel listModel:listOfModels){
			functionalAreaMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		return functionalAreaMap;
	}
	
	private Map<String, String> getCategoryMap() {
		categoryMap = new LinkedHashMap<String, String>();
		categoryMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeCategory");
		for(ListManagementModel listModel:listOfModels){
			categoryMap.put(listModel.getListValue(), listModel.getListValue());
		}
		return categoryMap;
	}
	
	private Map<String, String> getSourceClassMap() {
		sourceClassMap = new LinkedHashMap<String, String>();
		sourceClassMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeClass");
		for(ListManagementModel listModel:listOfModels){
			sourceClassMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		return sourceClassMap;
	}
	
	private Map<String, String> getQualityClassMap() {
		qualityClassMap = new LinkedHashMap<String, String>();
		qualityClassMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeQualityClass");
		for(ListManagementModel listModel:listOfModels){
			qualityClassMap.put(listModel.getListValue(), listModel.getListValue());
		}
		return qualityClassMap;
	}
	
	private Map<String, String> getTypeMap() {
		typeMap = new LinkedHashMap<String, String>();
		typeMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeType");
		for(ListManagementModel listModel:listOfModels){
			typeMap.put(listModel.getListValue(), listModel.getListValue());
		}

		return typeMap;
	}
	
	/**
	 * Initializing form object for Library attribute creation
	 * 
	 * @param model
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void initializeFormObjectAttCreation(Map<String, Object> model) throws IllegalAccessException, InvocationTargetException {	
		LibProdAttrCO prodAttrCO = new LibProdAttrCO();
		model.put(Constants.SKUVIEWLIBPRODUCTATTRCO, prodAttrCO);
		
		dataTypeMap = getDataTypeMap();
		model.put(Constants.DATATYPEMAP,dataTypeMap);
		uomMap = getUOMMap();
		model.put(Constants.UOMMAP, uomMap);
		typeOfDataMap = getTypeOfDataMap();
		model.put(Constants.TYPEOFDATAMAP, typeOfDataMap);
		hasMultiValueMap = getHashMultiValueMap();
		model.put(Constants.HASMULTIVALUEMAP, hasMultiValueMap);
		funClassMap = getFunClassMap();
		model.put(Constants.FUNCLASSMAP, funClassMap);
		qualityClassMap = getQualityClassMap();
		model.put(Constants.QUALITYCLASSDD, qualityClassMap);
		sourceClassMap = getSourceClassMap();
		model.put(Constants.SOURCECLASSMAPDD, sourceClassMap);
		relatedToCatMap = getRelatedToCatMap();
		model.put(Constants.RELTOCATMAP, relatedToCatMap);
		activeAttrMap = getActiveAttrMap();
		model.put(Constants.ACTIVATEATTRMAP, activeAttrMap);
	}
	
	/**
	 * Retrieving form object for attribute creation
	 *  
	 * @param model
	 * @param skuViewLibProdAttrCO
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void retrieveFormObjectAttCreation(Map<String, Object> model, LibProdAttrCO skuViewLibProdAttrCO) throws IllegalAccessException, InvocationTargetException {

		model.put(Constants.SKUVIEWLIBPRODUCTATTRCO, skuViewLibProdAttrCO);
		dataTypeMap = getDataTypeMap();
		model.put(Constants.DATATYPEMAP,dataTypeMap);
		uomMap = getUOMMap();
		model.put(Constants.UOMMAP, uomMap);
		typeOfDataMap = getTypeOfDataMap();
		model.put(Constants.TYPEOFDATAMAP, typeOfDataMap);
		hasMultiValueMap = getHashMultiValueMap();
		model.put(Constants.HASMULTIVALUEMAP, hasMultiValueMap);
		funClassMap = getFunClassMap();
		model.put(Constants.FUNCLASSMAP, funClassMap);
		qualityClassMap = getQualityClassMap();
		model.put(Constants.QUALITYCLASSDD, qualityClassMap);
		sourceClassMap = getSourceClassMap();
		model.put(Constants.SOURCECLASSMAPDD, sourceClassMap);
		relatedToCatMap = getRelatedToCatMap();
		model.put(Constants.RELTOCATMAP, relatedToCatMap);
		activeAttrMap = getActiveAttrMap();
		model.put(Constants.ACTIVATEATTRMAP, activeAttrMap);
	}
	
	private Map<DropdownValue, String> getDataTypeMap() {
		dataTypeMap = new LinkedHashMap<DropdownValue,String>();
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeDataType");
		for(ListManagementModel listModel:listOfModels){
			if(listModel.getListValue().equals("Alphanumeric")||listModel.getListValue().equals("Numeric")||listModel.getListValue().equals("Date")||listModel.getListValue().equals("Character")||listModel.getListValue().equals("Numeric Decimal"))
				dataTypeMap.put(new DropdownValue(listModel.getListValue(),""), listModel.getListValue());
			else
				dataTypeMap.put(new DropdownValue(listModel.getListValue(),"disabled"), listModel.getListValue());
		}
		return dataTypeMap;
	}
	
	private Map<String, String> getUOMMap() {
		uomMap = new LinkedHashMap<String, String>();
		uomMap.put("", "NA");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeUOM");
		for(ListManagementModel listModel:listOfModels){
			uomMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		return uomMap;
	}
	
	private Map<String, String> getTypeOfDataMap() {
		typeOfDataMap = new LinkedHashMap<String, String>();
		typeOfDataMap.put("","NA");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeTypeOfData");
		for(ListManagementModel listModel:listOfModels){
			typeOfDataMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		return typeOfDataMap;
	}
	
	private Map<String, String> getHashMultiValueMap() {
		hasMultiValueMap = new HashMap<String, String>();
		
	
		hasMultiValueMap.put("true", "True");
		hasMultiValueMap.put("false", "False");
		return hasMultiValueMap;
	}
	
	private Map<String, String> getFunClassMap() {
		funClassMap = new LinkedHashMap<String, String>();	
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeFunClass");
		for(ListManagementModel listModel:listOfModels){
			funClassMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		return funClassMap;
	}	
		
	private Map<String, String> getRelatedToCatMap() {
		relatedToCatMap = new LinkedHashMap<String, String>();
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("ProdAttributeRelatedToCatgry");
		for(ListManagementModel listModel:listOfModels){
			relatedToCatMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		return relatedToCatMap;
	}
	
	private Map<Long, String> getActiveAttrMap() throws IllegalAccessException, InvocationTargetException {		
		activeAttrMap = new LinkedHashMap<Long, String>();
		activeAttrMap.put((long) 0, ControllerConstants.DEFAULT_SELECT);
		try {
			List<LibProdAttrModel> libProdAttrModel = libProdAttrBO.findAllAttr(true);
			List<LibProdAttrCO> prodAttrCOList = new ArrayList<LibProdAttrCO>();
			for(LibProdAttrModel model:libProdAttrModel) {
				LibProdAttrCO libProdAttrCO = new LibProdAttrCO();
				BeanUtils.copyProperties(libProdAttrCO, model);
				prodAttrCOList.add(libProdAttrCO);
			}		
			for(LibProdAttrCO attrCO: prodAttrCOList) {			
				activeAttrMap.put(attrCO.getId(), attrCO.getName());
			}
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}		
		return activeAttrMap;
	}
	
	/**
	 * @param model
	 * @param roleId
	 * to get role based menu view
	 */
	private void setAccessMap(Map<String, Object> model, long roleId) {
		
		try{
			Roles role = roleBO.getRole(roleId);
			String[] accessLevel =  role.getAccessLevel();
		
			for(String access : accessLevel){
				String accessName = roleBO.getAccessName(Long.parseLong(access));
				model.put(accessName, accessName);
			}
		}catch(Exception e) {
			e.printStackTrace();
			
		}
	}
	
	@RequestMapping(value = "/libdeleteAttribute",  method = RequestMethod.GET)
	public String deleteImportBatch( Map<String, Object> model,@RequestParam("attrId") String attrId, HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
		
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		boolean retVal=false;
		initializeFormObject(model);
		List<LibProdAttrDisplayModel> prodDispModelList = null;
		deleteAttributeId = Integer.parseInt(attrId);
		try {
			retVal=libProdAttrBO.deleteAttributeDetail(deleteAttributeId);
			convertProdAttrSearchCOToModel(searchAttrModel, deleteAttrSearchCO);
			
			if(searchAttrModel.getFilterBy()==null && searchAttrModel.getSearchBy() == null &&
					searchAttrModel.getFunctionalArea()==null && searchAttrModel.getCategory()==null &&
					searchAttrModel.getSourceClass() ==null && searchAttrModel.getQualityClass() ==null &&
					searchAttrModel.getType() ==null) {
				
				searchAttrModel.setFilterBy(Constants.EMPTY_SELECTION);
				searchAttrModel.setSearchBy(Constants.EMPTY_SELECTION);
				searchAttrModel.setFunctionalArea(Constants.EMPTY_SELECTION);
				searchAttrModel.setCategory(Constants.EMPTY_SELECTION);
				searchAttrModel.setSourceClass(Constants.EMPTY_SELECTION);
				searchAttrModel.setQualityClass(Constants.EMPTY_SELECTION);
				searchAttrModel.setType(Constants.EMPTY_SELECTION);
			}
			prodSearchCriteria = new ProdAttrSearchCO();
			flag = true;
			fetchSearchProdcuts(request,prodSearchCriteria, false);
			formBackObject(model, true, null);
			formBackObject(deleteAttrSearchCO, model);
			prodDispModelList = libProdAttrBO.getALLProductAttributeResultList(searchAttrModel);
		} catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		model.put("prodDispModelList", prodDispModelList);
		
		if(retVal)
			model.put(Constants.DELETESUCCESS,"success");
		else
			model.put(Constants.DELETESUCCESS,"failed");
		
		return Constants.LIBATTRSEARCH;
	}
	
	
	@RequestMapping(value = "/libAttrPagination", method = RequestMethod.GET)
	public String librarypagination(@RequestParam(value="itest", required=false) String count, Map<String, Object> model, HttpServletRequest request,
			@RequestParam(value="searchBy") String searchBy, 
			@RequestParam(value="filterBy") String filterBy, 
			@RequestParam(value="keyWord") String keyWord,
			@RequestParam(value="functionalArea") String functionalArea, 
			@RequestParam(value="category") String category, 
			@RequestParam(value="sourceClass") String sourceClass, 
			@RequestParam(value="qualityClass") String qualityClass, 
			@RequestParam(value="type") String type) {
		
		ProdAttrSearchCO attrSearchCO = new ProdAttrSearchCO();
		attrSearchCO.setSearchBy(searchBy);
		attrSearchCO.setFilterBy(filterBy);
		attrSearchCO.setKeyWord(keyWord);
		attrSearchCO.setFunctionalArea(functionalArea);
		attrSearchCO.setCategory(Constants.EMPTY_SELECTION);
		attrSearchCO.setSourceClass(sourceClass);
		attrSearchCO.setQualityClass(qualityClass);
		attrSearchCO.setType(type);
		try{
			UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, user.getRole());
			request.getSession().setAttribute("count", count);
			fetchSearchProdcuts(request,prodSearchCriteria, true);
			formBackObject(model, true, null);
			formBackObject(prodSearchCriteria, model);
			formBackObject(attrSearchCO, model);
			model.put("label",null);
			model.put("productMergeSearchCriteriaCO", prodSearchCriteria);			
			if(null!=prodSearchCriteria.getSearchBy()) {
				model.put("Initial","second");
			}else {
				model.put("Initial","first");
			}
		} catch (Exception exception){
			throw new CustomGenericException(exception.getMessage());
		}
		return Constants.LIBATTRSEARCH;
	}
	
	
	private void fetchSearchProdcuts(HttpServletRequest request, ProdAttrSearchCO prodSearch, boolean pagination){
		try{
			UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
			//List<ProductSearchResultCO> searchResultList = null;
			List<LibProdAttrDisplayModel> prodDispModelList = null;
			if(null == searchAttrModel){
				searchAttrModel = new SearchAttributeModel();
			}
			if(null == prodSearch){
				prodSearch = new ProdAttrSearchCO();
			}
			int page = ServletRequestUtils.getIntParameter(request, "p", 0);
			if(!pagination){
				if(flag == false)
					totalCount = libProdAttrBO.getAttrTotCount();
				else{
					prodDispModelList = libProdAttrBO.getALLProductAttributeResultList(searchAttrModel);
					totalCount = prodDispModelList.size();
					flag = false;
				}
				
				//pagedListHolder = new PagedListHolder<LibProdAttrDisplayModel>();
				pagedListHolder = new SkuViewAttrPageListHolder();
				pagedListHolder.setMaxLinkedPages(10);
			//	pagedListHolder.setSource(prodDispModelList);
				pagedListHolder.setLibProdAttrBO(libProdAttrBO);
				pagedListHolder.setSearchModel(searchAttrModel);
				pagedListHolder.setTotalCount(totalCount);
				pagedListHolder.setUserId(user.getId());
			}
			pagedListHolder.setPage(page);
			pagedListHolder.setPageSize(Constants.PAGED_SIZE);
		}catch(Exception exception) {
			throw new CustomGenericException(exception.getMessage());
			
		}
	}
	
	private void formBackObject(Map<String, Object> model,boolean mergeFlag, String pageNo){
		try{
		//	searchUtil = new ProductSearchUtil();
			model.put("prodSearchCriteria", prodSearchCriteria );
			if(pageNo != null)
				pagedListHolder.setPage(Integer.valueOf(pageNo));
			else 
				initializeFormObject(model);
			model.put(Constants.PAGED_LIST, pagedListHolder);
			//model.put(Constants.SCREEN_NAME, screenName);
			model.put(Constants.TOTAL_COUNT, totalCount);
		}catch (Exception exception){
			throw new CustomGenericException(exception.getMessage());
		}
	}
	
	}

