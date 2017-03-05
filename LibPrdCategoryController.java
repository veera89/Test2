package com.skuview.common.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.skuview.category.dao.constants.Constants;
import com.skuview.category.model.CategoryAttributeRelation;
import com.skuview.category.model.CategoryAttributeRelationModel;
import com.skuview.common.command.CategorySearchCO;
import com.skuview.common.command.LibPrdCategoryCO;
import com.skuview.common.model.AttributeMapping;
import com.skuview.common.model.LibProdAttrModel;
import com.skuview.common.model.ListManagementModel;
import com.skuview.common.model.SearchAttributeModel;
import com.skuview.common.util.DownloadFileUtil;
import com.skuview.common.util.Utilities;
import com.skuview.controller.constants.ControllerConstants;
import com.skuview.exception.CustomGenericException;
import com.skuview.exception.UniqueNameViolation;
import com.skuview.export.model.ExportSummaryModel;
import com.skuview.libcategory.bo.LibPrdCategoryBO;
import com.skuview.libcategory.model.LibPrdCategoryModel;
import com.skuview.libprodattr.LibProdAttrBO;
import com.skuview.listmanagement.ListManagementBO;
import com.skuview.service.bo.RoleService;
import com.skuview.user.model.Roles;
import com.skuview.user.model.UserDetails;
/**
 * @author 372153
 *
 */
@Controller
public class LibPrdCategoryController {
	
	private static final Logger logger = LoggerFactory.getLogger(LibPrdCategoryController.class);
	
	@Autowired
	public LibPrdCategoryBO libPrdCategoryBO;
	
	@Autowired
	public LibProdAttrBO attributeBO;

	@Autowired
	public RoleService roleBO;
	
	@Autowired
	ListManagementBO listManagementBO;
		
	@Autowired
	public DownloadFileUtil downloadFileUtil;

	
	@Autowired
	@Qualifier("libPrdCategoryValidator")
	private Validator validator;
		
	@InitBinder("categoryCO")
	private void InitBinder(WebDataBinder binder) {
		binder.setValidator(validator);		
	}
	
	Map<String, String> searchByMap = null;
	Map<String, String> filterByMap = null;
	Map<String, String> isRootMap = null;
	Map<String, String> isActiveMap = null;
	Map<Long, String> clientCategories = null;
	Map<String, AttributeMapping> attrCache;
	Map<Integer, String> isMandatoryMap = null;	
	List<AttributeMapping> attributeMappingAvailAndDisplayList = null;
	long deleteCategoryId=0;
	String treeName = "";
	
	/**
	 * @param model
	 * @return
	 * 
	 * ** Author : 443699
	 * ** To initialize and load create Category screen
	 */
	@RequestMapping(value = "/libCreateCategory", method = RequestMethod.GET)
	public String createCategory(ModelMap model, HttpServletRequest request) {
		
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		treeName = "";
		initializeCreateCategoryFormObject(model);
		LibPrdCategoryCO categoryCO = new LibPrdCategoryCO();		
		model.put("categoryCO", categoryCO);
		model.put("action", "create");
		try {
			loadMapProductAttributes(new LibPrdCategoryCO(), model);
		} catch (Exception e) {
		}
		setCategoryTreeData(model);
		return "LibCategory";
	}
	
	
	/**
	 * @param LibPrdCategoryCO
	 * @param model
	 * @param request
	 * @return
	 * 
	 * ** To Copy Existing category details to create a new category
	 */
	@RequestMapping(value = "/libSaveCategory", method = RequestMethod.POST, params="go")
	public ModelAndView copySelectedCategoryToPage(@ModelAttribute("categoryCO") LibPrdCategoryCO categoryCO, ModelMap model, HttpServletRequest request) {
		try {

			UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			LibPrdCategoryModel clientCategoryModel = new LibPrdCategoryModel();
			treeName = "";
			isRootMap = getIsRootMap();
			model.put("isRootMap", isRootMap);			
			isActiveMap = getIsActiveMap();
			model.put("isActiveMap", isActiveMap);			
			clientCategories = libPrdCategoryBO.getAllClientCategoryNames();
			clientCategories.put(Long.valueOf("0"), ControllerConstants.DEFAULT_SELECT);
			model.put("clientCategories", clientCategories);
			loadMapProductAttributes(categoryCO, model);
			model.put("action", "create");
			setCategoryTreeData(model);
			
			if(categoryCO.getClientCategories() > 0){
				clientCategoryModel = libPrdCategoryBO.getClientCategory(categoryCO.getClientCategories());
			}else{
				model.put("message", "Please select a Category to copy the values");
				model.put("messageType", "failure");
				model.addAttribute("categoryCO", categoryCO);
				return new ModelAndView("LibCategory", model);
			}
						
			LibPrdCategoryCO clientCategoryCO = convertModelToCO(clientCategoryModel);
			if(clientCategoryModel.getParentCategoryId() > 0){
				clientCategoryCO.setParentCategory(getCategoryName(clientCategoryModel.getParentCategoryId()));
			}
			clientCategoryCO.setIsRoot(Utilities.getStringFromBoolean(clientCategoryModel.isRoot()));
			clientCategoryCO.setIsActive(Utilities.getStringFromBoolean(clientCategoryModel.isActive()));
			clientCategoryCO.setId(0);
			clientCategoryCO.setSkuViewId(0);
			clientCategoryCO.setName(categoryCO.getName());
			clientCategoryCO.setAttributes(categoryCO.getAttributes());
			model.addAttribute("categoryCO", clientCategoryCO);
						
		}catch(Exception e) {
			throw new CustomGenericException(e.getMessage());
		}		
		return new ModelAndView("LibCategory", model);
	}
	
	/**
	 * @param LibPrdCategoryCO
	 * @param model
	 * @return
	 * ** To clear the data on Create Category screen on click of cancel
	 */
	@RequestMapping(value = "/libSaveCategory", method = RequestMethod.POST, params="cancel")
	public String cancelCategory(@ModelAttribute("categoryCO") LibPrdCategoryCO categoryCO, Map<String, Object> model) {
		return "redirect:/libCreateCategory";
	}
	
	/**
	 * @param LibPrdCategoryCO
	 * @param model
	 * @return
	 * ** To save new Category
	 */
	@RequestMapping(value = "/libSaveCategory", method = RequestMethod.POST, params="save")
	public ModelAndView saveCategory(@ModelAttribute("categoryCO")@Validated LibPrdCategoryCO categoryCO, BindingResult result, Map<String, Object> model,HttpServletRequest request) {
		

		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		treeName = "";
		if(result.hasErrors()) {
			initializeCreateCategoryErrorObject(model,categoryCO);
			setCategoryTreeData(model);
			if(!(categoryCO.getId() > 0)) {
				model.put("action", "create");
			}			
			return new ModelAndView("LibCategory", model);
		}
		
		try{
			LibPrdCategoryModel categoryModel = new LibPrdCategoryModel();
			categoryCO.setName(categoryCO.getName().trim());
			BeanUtils.copyProperties(categoryModel, categoryCO);
			convertStringToBoolean(categoryCO, categoryModel);
			if(categoryCO.getParentCategory() != null && categoryCO.getParentCategory() != ControllerConstants.EMPTY_STRING){
				categoryModel.setParentCategoryId(getClientCategoryId(categoryCO.getParentCategory()));
			}
			
			Boolean isSaved = false;
			Boolean isUpdated = false; 
			Boolean isAttributeMapped = false;			
			if(categoryCO.getId() > 0 ) {				
				isUpdated = libPrdCategoryBO.updateCategory(categoryModel);
				BeanUtils.copyProperties(categoryCO, categoryModel);
				isAttributeMapped = createAttributeMapping(categoryCO);
				if(isUpdated){
					model.put("message", "Category has been updated successfully");
					model.put("messageType", "success");
				}else{
					model.put("message", "Error occurred while updating Category.Please contact server admin");
					model.put("messageType", "failure");
				}
			}  else {										
					categoryModel = libPrdCategoryBO.createCategory(categoryModel);
					if(categoryModel.getId() > 0){
						isSaved = true;
					}
					BeanUtils.copyProperties(categoryCO, categoryModel);
					isAttributeMapped = createAttributeMapping(categoryCO);
					if(isSaved){
						model.put("message", "Category has been saved successfully");
						model.put("messageType", "success");
					}else{
						model.put("message", "Error occurred while saving Category.Please contact server admin");
						model.put("messageType", "failure");
					}				
					//model.put("action", "create");
			}
			
			Map<Long, String> availableAttributesMap = new HashMap<Long, String>();
			//availableAttributesMap = attributeBO.getAllAttributeNames();
			availableAttributesMap = libPrdCategoryBO.getAllClientAttrIdMap();
			List<AttributeMapping> attributeMappingAvailAndDisplayList = new ArrayList<AttributeMapping>();
			AttributeMapping attributeMapping = null;
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
			//categoryCO = new CategoryCO();		
			model.put("categoryCO", categoryCO);
								
		} catch(UniqueNameViolation uniqueException) {
			model.put("available", "catnameexist");
			if(!(categoryCO.getId() > 0)) {
				model.put("action", "create");
			}
		}catch(Exception exception){
			throw new CustomGenericException(exception.getMessage());
		}
		initializeCreateCategoryFormObject(model);
		setCategoryTreeData(model);

		return new ModelAndView("LibCategory", model);
	}
	
	private boolean createAttributeMapping(LibPrdCategoryCO categoryCO) throws Exception {
		
		boolean isAttributeMapped = false;
		LibPrdCategoryModel categoryModel = new LibPrdCategoryModel();
		BeanUtils.copyProperties(categoryModel, categoryCO);
		List<CategoryAttributeRelation> parentAttrRelnWithChild = libPrdCategoryBO.getAllMappedAttributes(categoryCO.getId());
		
		List<CategoryAttributeRelation> attrRelnToModelSet = new ArrayList<CategoryAttributeRelation>();
		
		if(parentAttrRelnWithChild == null) {
			List<CategoryAttributeRelation> attrRelnToModelSetParent = new ArrayList<CategoryAttributeRelation>();
			if(categoryCO.getIsRoot().equalsIgnoreCase("yes")) {
				isAttributeMapped = updateChildCategories(categoryCO, categoryModel, attrRelnToModelSetParent);
			} else {
				long parentId = categoryCO.getParentCategoryId();
				while(parentId > 0) {
					 parentAttrRelnWithChild = libPrdCategoryBO.getAllMappedAttributes(parentId);
					 LibPrdCategoryModel catModel=  libPrdCategoryBO.getClientCategory(parentId);
					 if(parentAttrRelnWithChild != null) {
						 for(CategoryAttributeRelation attributeRelation : parentAttrRelnWithChild) {
							 attrRelnToModelSet.add(buildAttributeRelation(attributeRelation.getAttributeId(), true, parentId));
						 }
					 }
					 parentId = catModel.getParentCategoryId();
				}
				isAttributeMapped = updateChildCategories(categoryCO, categoryModel, attrRelnToModelSet);
			}
		} else {
			long parentId = categoryCO.getParentCategoryId();
			while(parentId > 0) {
				 parentAttrRelnWithChild = libPrdCategoryBO.getAllMappedAttributes(parentId);
				 LibPrdCategoryModel catModel=  libPrdCategoryBO.getClientCategory(parentId);
				 if(parentAttrRelnWithChild != null) {
					 for(CategoryAttributeRelation attributeRelation : parentAttrRelnWithChild) {
						 attrRelnToModelSet.remove(attributeRelation.getAttributeId());
						 attrRelnToModelSet.add(buildAttributeRelation(attributeRelation.getAttributeId(), true, parentId));
					 }
				 }
				 parentId = catModel.getParentCategoryId();
			}
			 isAttributeMapped = updateChildCategories(categoryCO, categoryModel, attrRelnToModelSet);
		}
		return isAttributeMapped;
	}
	
	private boolean updateChildCategories(LibPrdCategoryCO categoryCO, LibPrdCategoryModel categoryModel, List<CategoryAttributeRelation> attrRelnToModelSetParent) throws Exception {
		logger.info("Entering updateChildCategories..");
		
		List<CategoryAttributeRelation> attrRelnToModelSetToInherit = new ArrayList<CategoryAttributeRelation>();
		List<CategoryAttributeRelation> attrRelnToModelSetMapped = new ArrayList<CategoryAttributeRelation>();
		List<CategoryAttributeRelation> attrRelnToModelSetFinal = new ArrayList<CategoryAttributeRelation>();
		
		CategoryAttributeRelationModel attributeRelationModel = new CategoryAttributeRelationModel();
		boolean isAttributeMapped = false;
		removeDuplicate(attrRelnToModelSetParent);
		// loop for updating the current category .
		if(categoryCO.getAttributes() != null) {
			for(AttributeMapping attributeMapping : categoryCO.getAttributes()) {
				attrRelnToModelSetParent.add(buildAttributeRelation(attributeMapping.getKey(), false, 0));
			}
		}
		
		attributeRelationModel.setMappedAttributes(attrRelnToModelSetParent);
		attributeRelationModel.setId(categoryCO.getId());
		attributeRelationModel.setCategoryName(categoryCO.getName());
		isAttributeMapped = libPrdCategoryBO.createAttributeMapping(attributeRelationModel);
		//
		
		
		
		List<LibPrdCategoryModel>  categoryModelList = returnAllChildCategories(categoryModel);
		if(categoryCO.getAttributes() != null) {
			for(AttributeMapping attributeMapping : categoryCO.getAttributes()) {
				attrRelnToModelSetToInherit.add(buildAttributeRelation(attributeMapping.getKey(), true, categoryCO.getId()));
			}
		}
		
		attrRelnToModelSetFinal.addAll(attrRelnToModelSetToInherit);
		if(categoryModelList != null) {
			 for(LibPrdCategoryModel childCategory : categoryModelList) {
				 attrRelnToModelSetMapped = new ArrayList<CategoryAttributeRelation>();
				 if(childCategory.getId() != categoryCO.getId()) {
					 List<CategoryAttributeRelation> mappedAttributes = libPrdCategoryBO.getAllMappedAttributes(childCategory.getId());
					 if(mappedAttributes != null) {
						 for(CategoryAttributeRelation CategoryAttributeRelation : mappedAttributes) {
							 attrRelnToModelSetMapped.add(CategoryAttributeRelation);
						 }
					 }
					 attrRelnToModelSetFinal.addAll(attrRelnToModelSetMapped);
					 
					 logger.info("To be inherited to child =" + attrRelnToModelSetToInherit.size());
					 logger.info("Already Mapped List for child" + attrRelnToModelSetMapped.size());
					 logger.info("To be inherited to Final =" + attrRelnToModelSetFinal.size());
					 
					 
					 // Loop for removing the duplicates in the final list to update the child.
					 
					 int sizeattr = attrRelnToModelSetToInherit.size();
					 for(int i = 0; i < sizeattr; i++) {
				        for(int x = 0; x < attrRelnToModelSetMapped.size(); x++) {
				            if(attrRelnToModelSetToInherit.get(i).getAttributeId() == attrRelnToModelSetMapped.get(x).getAttributeId()) {
				            		logger.info("Duplicate Found..");
				            		attrRelnToModelSetFinal.remove(attrRelnToModelSetToInherit.get(i));
				            	
				            }
				        }
					 }
					 
					 
					 // loop for removing the attribute from child which is not in parent and child inherited flag is true.
					 if(attrRelnToModelSetFinal != null){
						 List<CategoryAttributeRelation> tempValue = new ArrayList<CategoryAttributeRelation>();
						 for(CategoryAttributeRelation categoryAttributeRelation : attrRelnToModelSetFinal){
							 
							 boolean matchFound = false;
							 for(CategoryAttributeRelation categoryAttributeRelationInherit : attrRelnToModelSetToInherit){
						          	if(categoryAttributeRelation.getAttributeId() == categoryAttributeRelationInherit.getAttributeId()) {
						            	matchFound = true;
						            }
						       }
							 if(!matchFound && categoryAttributeRelation.getIsInherited() && categoryCO.getId() == categoryAttributeRelation.getParentCategoryId()) {
						        	logger.info("No Matches found...");
						        	
						     }else{
						    	 tempValue.add(categoryAttributeRelation);
						     }
						 }
						 attrRelnToModelSetFinal = null; attrRelnToModelSetFinal= tempValue;
					 }
					 
					 attributeRelationModel.setMappedAttributes(attrRelnToModelSetFinal);
					 attributeRelationModel.setId(childCategory.getId());
					 attributeRelationModel.setCategoryName(childCategory.getName());
					 isAttributeMapped = libPrdCategoryBO.createAttributeMapping(attributeRelationModel);
					 attrRelnToModelSetToInherit.clear();
					 attrRelnToModelSetFinal.clear();
					 if(categoryCO.getAttributes() != null) {
						 for(AttributeMapping attributeMapping : categoryCO.getAttributes()) {
							 attrRelnToModelSetToInherit.add(buildAttributeRelation(attributeMapping.getKey(), true, categoryCO.getId()));
						 }
					 }
					 attrRelnToModelSetFinal.addAll(attrRelnToModelSetToInherit);
				 }
			 }
		 }
		 logger.info("Exiting updateChildCategories..");
		 return isAttributeMapped;
	}
	
	private void removeDuplicate(List<CategoryAttributeRelation> list) throws Exception {
		 HashSet<Long> h = new HashSet<Long>();
		 List <CategoryAttributeRelation> newList = new ArrayList <CategoryAttributeRelation>();
		 
		 ListIterator li = list.listIterator(list.size());
		 while(li.hasPrevious()) {
			 CategoryAttributeRelation relation = ((CategoryAttributeRelation)li.previous());
			 if(h.add(relation.getAttributeId())) {
				 newList.add(relation);
			 }
		 }

		 list.clear();
	     list.addAll(newList);
	}
	
	private List<LibPrdCategoryModel> returnAllChildCategories(LibPrdCategoryModel category) throws Exception {
		logger.info("Enterning returnAllChildCategories..");
	    List<LibPrdCategoryModel> listOfCategories = new ArrayList<LibPrdCategoryModel>();
	    addAllChildCategories(category, listOfCategories);
	    logger.info("Exiting returnAllChildCategories..");
	    return listOfCategories;
	}

	private void addAllChildCategories(LibPrdCategoryModel category, List<LibPrdCategoryModel> listOfCategories) throws Exception {
		logger.info("Enterning addAllCategories.. ");
	    if (category != null && category.getId() > 0) {
	    	listOfCategories.add(category);
	        List<LibPrdCategoryModel> childCategoryList = libPrdCategoryBO.getChildCategories(category.getId());
	        if (childCategoryList != null && childCategoryList.size() > 0) {
	            for (LibPrdCategoryModel childCategory: childCategoryList) {
	            	addAllChildCategories(childCategory, listOfCategories);
	            }
	        }
	    }
	    logger.info("Exiting addAllCategories.. ");
	}
	
	private CategoryAttributeRelation buildAttributeRelation(long key, boolean isInherited, long parentId) throws Exception {
		logger.info("Enterning buildAttributeRelation.. ");
		CategoryAttributeRelation catAttrRelation = new CategoryAttributeRelation();
		LibProdAttrModel attributeModel =  attributeBO.findAttribute((int)key);
		catAttrRelation.setAttributeId(attributeModel.getId());
		catAttrRelation.setAttributeName(attributeModel.getName());
		catAttrRelation.setActive(attributeModel.isActive());
		catAttrRelation.setInherited(isInherited);
		catAttrRelation.setIsChecked(true);
		catAttrRelation.setIsMandatory(catAttrRelation.getIsMandatory() >= 0 ? catAttrRelation.getIsMandatory() : ControllerConstants.MANDATORY);
		catAttrRelation.setParentCategoryId(parentId);
		logger.info("Exiting buildAttributeRelation.. ");
		return catAttrRelation;
	}
	
	/**
	 * @param model
	 * @return
	 * ** To list out all the available Categories
	 */
	@RequestMapping(value = "/libSearchAllCategory", method = RequestMethod.GET)
	public String searchAllCategory(Map<String, Object> model, HttpServletRequest request) {
		

		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		initializeSearchFormObject(model);
		treeName = "";
		try{
			List<LibPrdCategoryModel> categoryModels = libPrdCategoryBO.getALLCategoryResult();
			Map<Long, LibPrdCategoryModel> categoryMap = libPrdCategoryBO.getAllMappedLib();
			
			List<LibPrdCategoryCO> categoryCOs = convertCategoryModelsToCategoryCOs(categoryModels, categoryMap);
			model.put("categoryCOs", categoryCOs);
			
			setCategoryTreeData(model);
			
		}catch(Exception exception){
			throw new CustomGenericException(exception.getMessage());
		}
		
		return "LibCategorySearch";
		
	}
	
	
	/**
	 * @param model
	 * ** To construct the LibPrdCategoryCO object for Tree
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
	 * @param categorySearchCO
	 * @param model
	 * @return
	 * ** To list out the filtered Categories
	 */
	@RequestMapping(value = "/libSearchCategory", method = RequestMethod.POST)
	public String searchCategory(@ModelAttribute("categorySearchCO") CategorySearchCO categorySearchCO, Map<String, Object> model, HttpServletRequest request) {
		try{
			UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			treeName = "";
			SearchAttributeModel searchAttributeModel = new SearchAttributeModel();
			List<LibPrdCategoryModel> categoryModels = new ArrayList<LibPrdCategoryModel>();
			if(categorySearchCO.getKeyWord().equals(ControllerConstants.EMPTY_STRING) && categorySearchCO.getIsActive().equals(ControllerConstants.ALL) && 
					categorySearchCO.getIsRoot().equals(ControllerConstants.ALL)){
				categoryModels = libPrdCategoryBO.getALLCategoryResult();
			}else{
				convertSearchCOToSearchModel(searchAttributeModel, categorySearchCO);
				categoryModels = libPrdCategoryBO.getALLCategoryResult(searchAttributeModel);
			}
			Map<Long, LibPrdCategoryModel> categoryMap = libPrdCategoryBO.getAllMappedLib();
			
			List<LibPrdCategoryCO> categoryCOs = convertCategoryModelsToCategoryCOs(categoryModels, categoryMap);
			//initializeSearchFormObject(model);
			retrieveSearchFormObject(model,categorySearchCO);
			setCategoryTreeData(model);
			model.put("categoryCOs", categoryCOs);
		}catch(Exception exception){
			throw new CustomGenericException(exception.getMessage());
		}
		return "LibCategorySearch";
	}
	
	/**
	 * @param searchBy,filterBy,keyWord,isRoot,isActive.
	 * @param model
	 * @return
	 * ** To retain the list of the filtered Categories.
	 */
	
	@RequestMapping(value = "/librarySearchCategory", method = RequestMethod.GET)
	public String libsearchCategory( 
			@RequestParam(value = "searchBy")String searchBy,
			@RequestParam(value = "filterBy")String filterBy,
			@RequestParam(value = "keyWord")String keyWord,
			@RequestParam(value = "isRoot")String isRoot,
			@RequestParam(value = "isActive")String isActive,
			Map<String, Object> model, HttpServletRequest request) {
		CategorySearchCO categorySearchCO = new CategorySearchCO();
		categorySearchCO.setSearchBy(searchBy);
		categorySearchCO.setFilterBy(filterBy);
		categorySearchCO.setKeyWord(keyWord);
		categorySearchCO.setIsRoot(isRoot);
		categorySearchCO.setIsActive(isActive);
		treeName = "";
		try{
			UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			
			SearchAttributeModel searchAttributeModel = new SearchAttributeModel();
			List<LibPrdCategoryModel> categoryModels = new ArrayList<LibPrdCategoryModel>();
			if(categorySearchCO.getKeyWord().equals(ControllerConstants.EMPTY_STRING) && categorySearchCO.getIsActive().equals(ControllerConstants.ALL) && 
					categorySearchCO.getIsRoot().equals(ControllerConstants.ALL)){
				categoryModels = libPrdCategoryBO.getALLCategoryResult();
			}else{
				convertSearchCOToSearchModel(searchAttributeModel, categorySearchCO);
				categoryModels = libPrdCategoryBO.getALLCategoryResult(searchAttributeModel);
			}
			Map<Long, LibPrdCategoryModel> categoryMap = libPrdCategoryBO.getAllMappedLib();
			
			List<LibPrdCategoryCO> categoryCOs = convertCategoryModelsToCategoryCOs(categoryModels, categoryMap);
			//initializeSearchFormObject(model);
			retrieveSearchFormObject(model,categorySearchCO);
			setCategoryTreeData(model);
			model.put("categoryCOs", categoryCOs);
			model.put("categorySearchCO", categorySearchCO);
		}catch(Exception exception){
			//throw new CustomGenericException(exception.getMessage());
			exception.printStackTrace();
		}
		return "LibCategorySearch";
	}
	
	/**
	 * @param name
	 * @param model
	 * @return
	 * ** To filter the Category from Tree Structure
	 */
	@RequestMapping(value = "/libSearchTreeCategory")
	public String searchTreeCategory(@RequestParam(value="name",required=true)String name, Map<String, Object> model, HttpServletRequest request){
		
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		SearchAttributeModel searchAttributeModel = new SearchAttributeModel();
		List<LibPrdCategoryModel> categoryModels = new ArrayList<LibPrdCategoryModel>();
		try{
			searchAttributeModel.setSearchBy(ControllerConstants.NAME);
			searchAttributeModel.setFilterBy(ControllerConstants.EQUALS);
			searchAttributeModel.setKeyWord(name);
			searchAttributeModel.setIsRoot(ControllerConstants.ALL);
			searchAttributeModel.setIsActive(ControllerConstants.ALL);
			treeName = name;
			categoryModels = libPrdCategoryBO.getALLCategoryResult(searchAttributeModel);
			Map<Long, LibPrdCategoryModel> categoryMap = libPrdCategoryBO.getAllMappedLib();
			
			List<LibPrdCategoryCO> categoryCOs = convertCategoryModelsToCategoryCOs(categoryModels, categoryMap);
			initializeSearchFormObject(model);
			setCategoryTreeData(model);
			model.put("categoryCOs", categoryCOs);
		}catch(Exception exception){
			throw new CustomGenericException(exception.getMessage());
		}
		return "LibCategorySearch";
	}
	
	/**
	 * @param clientCategoryId
	 * @param model
	 * @return
	 * ** To load screen for editing category
	 */
	@RequestMapping(value = "/libLoadEditCategory", method = RequestMethod.GET)
	public ModelAndView editCategory(@RequestParam(value="id",required=false)long categoryId,
			@RequestParam(value = "searchBy")String searchBy,
			@RequestParam(value = "filterBy")String filterBy,
			@RequestParam(value = "keyWord")String keyWord,
			@RequestParam(value = "isRoot")String isRoot,
			@RequestParam(value = "isActive")String isActive,
			@ModelAttribute("categoryCO") LibPrdCategoryCO categoryCO, ModelMap model, HttpServletRequest request) {
		
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		logger.info("Entering editCategory ..");
		CategorySearchCO categorySearchCO = new CategorySearchCO();
		categorySearchCO.setSearchBy(searchBy);
		categorySearchCO.setFilterBy(filterBy);
		categorySearchCO.setKeyWord(keyWord);
		categorySearchCO.setIsRoot(isRoot);
		categorySearchCO.setIsActive(isActive);
		treeName = "";
		try {
			LibPrdCategoryModel clientCategoryModel = null;
			if(categoryId > 0) {
				clientCategoryModel = libPrdCategoryBO.getClientCategory(categoryId);
			} else {
				clientCategoryModel = libPrdCategoryBO.getClientCategory(categoryCO.getId());
			}
			if(libPrdCategoryBO.checkCatNameExistance(clientCategoryModel))
				model.put("message", "exist");
			else
				model.put("message", "not exist");
			isRootMap = getIsRootMap();
			model.put("isRootMap", isRootMap);
			
			isActiveMap = getIsActiveMap();
			model.put("isActiveMap", isActiveMap);
			categoryCO.setParentCategoryId(clientCategoryModel.getParentCategoryId());
			loadMapProductAttributes(categoryCO, model);

			LibPrdCategoryCO clientCategoryCO = convertModelToCO(clientCategoryModel);
			clientCategoryCO.setIsRoot(Utilities.getStringFromBoolean(clientCategoryModel.isRoot()));
			clientCategoryCO.setIsActive(Utilities.getStringFromBoolean(clientCategoryModel.isActive()));
			clientCategoryCO.setIsMapped(Utilities.getStringFromBoolean(clientCategoryModel.isMapped()));
			clientCategoryCO.setHasValue(Utilities.getStringFromBoolean(clientCategoryModel.isHasValue()));
			clientCategoryCO.setAttributes(categoryCO.getAttributes());

			if(clientCategoryModel.getParentCategoryId() > 0){
				clientCategoryCO.setParentCategory(getCategoryName(clientCategoryModel.getParentCategoryId()));
			}
			//model.addAttribute("attributes", categoryCO.getAttributes());
			model.put("categorySearchCO",categorySearchCO);
			model.addAttribute("categoryCO", clientCategoryCO);
			setCategoryTreeData(model);
		}catch(Exception exception) {
			throw new CustomGenericException(exception.getMessage());
		}
		logger.info("Exiting editCategory ..");
		return new ModelAndView("LibCategory", model);
	}
	
	/**
	 * @param clientCategoryId
	 * @param LibPrdCategoryCO
	 * @param model
	 * @return
	 * ** To initialize the copy Category screen
	 */
	@RequestMapping(value = "/libLoadCopyCategory", method = RequestMethod.GET)
	public ModelAndView copyCategory(@ModelAttribute("categoryCO") LibPrdCategoryCO categoryCO, ModelMap model, HttpServletRequest request) {
		logger.info("Entering libLoadCopyCategory");
		try {
			UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			LibPrdCategoryModel clientCategoryModel = libPrdCategoryBO.getClientCategory(categoryCO.getId());
			isRootMap = getIsRootMap();
			model.put("isRootMap", isRootMap);
			clientCategories = libPrdCategoryBO.getAllClientCategoryNames();
			clientCategories.put(Long.valueOf("0"), ControllerConstants.DEFAULT_SELECT);
			model.put("clientCategories", clientCategories);
			isActiveMap = getIsActiveMap();
			model.put("isActiveMap", isActiveMap);
			loadMapProductAttributes(categoryCO, model);
			model.put("action", "create");
			treeName = "";
			LibPrdCategoryCO clientCategoryCO = convertModelToCO(clientCategoryModel);
			clientCategoryCO.setIsRoot(Utilities.getStringFromBoolean(clientCategoryModel.isRoot()));
			clientCategoryCO.setIsActive(Utilities.getStringFromBoolean(clientCategoryModel.isActive()));
			clientCategoryCO.setName("");
			clientCategoryCO.setId(0);
			clientCategoryCO.setSkuViewId(0);
			clientCategoryCO.setAttributes(categoryCO.getAttributes());
			
			if(clientCategoryModel.getParentCategoryId() > 0){
				clientCategoryCO.setParentCategory(getCategoryName(clientCategoryModel.getParentCategoryId()));
			}
			model.addAttribute("categoryCO", clientCategoryCO);
			setCategoryTreeData(model);
		}catch(Exception excpt) {
			throw new CustomGenericException(excpt.getMessage());
		}
		logger.info("Exiting libLoadCopyCategory");
		return new ModelAndView("LibCategory", model);
	}
	
	/**
	 * @param model
	 * @param request
	 * @return
	 * 
	 * ** To remove a category
	 */
	
	@SuppressWarnings("unchecked")
	public void loadMapProductAttributes(LibPrdCategoryCO categoryCO, ModelMap model) {
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
	
	@RequestMapping(value = "/libUpdateMappedAttributes", method = RequestMethod.POST, params="updateMappedAttr")
	public ModelAndView updateMappedAttributes(@ModelAttribute("categoryCO")LibPrdCategoryCO categoryCO, ModelMap model, HttpServletRequest request) throws Exception {
		logger.info("Entering updateMappedAttributes..");
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		isRootMap = getIsRootMap();
		model.put("isRootMap", isRootMap);			
		isActiveMap = getIsActiveMap();
		model.put("isActiveMap", isActiveMap);
		treeName = "";
		Map<Long, String> availableAttributesMap = new HashMap<Long, String>();
		//availableAttributesMap = attributeBO.getAllAttributeNames();
		availableAttributesMap = libPrdCategoryBO.getAllClientAttrIdMap();
		List<AttributeMapping> attributeMappingAvailAndDisplayList = new ArrayList<AttributeMapping>();
		AttributeMapping attributeMapping = null;
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
		model.addAttribute("availableAttributesList", attributeMappingAvailAndDisplayList);
		model.addAttribute("categoryCO", categoryCO);
		logger.info("Exiting updateMappedAttributes..");
		return new ModelAndView("LibCategory", model);
	}
	
	@RequestMapping(value = "/libCheckLibDeleteCategory", method = RequestMethod.POST, produces = MediaType.ALL_VALUE)
	public @ResponseBody String checkDeleteAttribute(
			@RequestBody final long catId, HttpServletResponse response) {
		deleteCategoryId=catId;
		String message="";
		try{
			
			if(libPrdCategoryBO.hasValue(catId))
				message="in use";
			else
				message = "not in use";
			
			
		}catch (Exception e) {
			throw new CustomGenericException(e.getMessage());
		}	
		
	return message;
			
		}
	
	
	@RequestMapping(value = "/libDeleteCategory", method = RequestMethod.GET)
	public String deleteCategory(Map<String, Object> model,@RequestParam("catId") String catId ,HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
		deleteCategoryId=Integer.parseInt(catId);
		boolean retVal=false;
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(model, userSess.getRole());
		initializeSearchFormObject(model);
		treeName = "";
		try {
			retVal = libPrdCategoryBO.createCatTree(deleteCategoryId);
			List<LibPrdCategoryModel> categoryModels = libPrdCategoryBO.getALLCategoryResult();
			Map<Long, LibPrdCategoryModel> categoryMap = libPrdCategoryBO.getAllMappedLib();
			List<LibPrdCategoryCO> categoryCOs = convertCategoryModelsToCategoryCOs(categoryModels, categoryMap);
			model.put("categoryCOs", categoryCOs);
			setCategoryTreeData(model);
		}catch(Exception excep){
			//throw new CustomGenericException(excep.getMessage());
			excep.printStackTrace();
		}
		
		if(retVal)
			model.put(Constants.DELETESUCCESS,"success");
		else
			model.put(Constants.DELETESUCCESS,"failed");
		
		return "LibCategorySearch";
	}
	
	
	@RequestMapping(value = "/downloadLibCat")
	public void downloadMergeSummaryDetails(
			@RequestParam(value = "searchBy")String searchBy,
			@RequestParam(value = "filterBy")String filterBy,
			@RequestParam(value = "keyWord")String keyWord,
			@RequestParam(value = "isRoot")String isRoot,
			@RequestParam(value = "isActive")String isActive,
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		
		CategorySearchCO categorySearchCO = new CategorySearchCO();
		if(treeName != null && treeName.length() > 0)
			categorySearchCO.setSearchBy(ControllerConstants.NAME);
		else
			categorySearchCO.setSearchBy(searchBy);
		if(treeName != null && treeName.length() > 0)
			categorySearchCO.setFilterBy(ControllerConstants.EQUALS);
		else
			categorySearchCO.setFilterBy(filterBy);
		if(treeName != null && treeName.length() > 0)
			categorySearchCO.setKeyWord(treeName);
		else
			categorySearchCO.setKeyWord(keyWord);
		if(treeName != null && treeName.length() > 0)
			categorySearchCO.setIsRoot(ControllerConstants.ALL);
		else
			categorySearchCO.setIsRoot(isRoot);
		if(treeName != null && treeName.length() > 0)	
			categorySearchCO.setIsActive(ControllerConstants.ALL);
		else
			categorySearchCO.setIsActive(isActive);
		try {
			
			Map<String, String> settingsMap = libPrdCategoryBO.getSettingsCollection();
			String downloadFileFormat = settingsMap.get(Constants.DOWNLOAD_FILE_FORMAT);
			
			ExportSummaryModel exportSummaryModel = new ExportSummaryModel();
			UserDetails userSess = (UserDetails) request.getSession().getAttribute(
					"LoggedUser");
			setAccessMap(model, userSess.getRole());
			
			exportSummaryModel.setRequestedUser(userSess.getLoginName());
			
			String exportFilePath = libPrdCategoryBO.getlibCatFilePath("ExportFilePath");
			exportSummaryModel.setFilePath(exportFilePath);
			exportSummaryModel.setFileType(downloadFileFormat);
			
			SearchAttributeModel searchAttributeModel = new SearchAttributeModel();
			if(categorySearchCO.getKeyWord().equals(ControllerConstants.EMPTY_STRING) && categorySearchCO.getIsActive().equals(ControllerConstants.ALL) && 
					categorySearchCO.getIsRoot().equals(ControllerConstants.ALL)){
				libPrdCategoryBO.getLibCatDetails(exportSummaryModel);
			}else{
				convertSearchCOToSearchModel(searchAttributeModel, categorySearchCO);
				libPrdCategoryBO.getLibCatDetails(exportSummaryModel,searchAttributeModel);
			}
			
			

			if (exportSummaryModel.getFileName() != null) {
				downloadFileUtil.downloadFile(request,response,(exportSummaryModel.getFilePath() + "/" + exportSummaryModel.getFileName()));
			}
		} catch(Exception exception) {
			exception.printStackTrace();
			 throw new CustomGenericException(exception.getMessage());
		}
		
		
	}
	
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
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
	 * @param categoryModel
	 * @return
	 * ** To convert the LibPrdCategoryModel to LibPrdCategoryCO
	 */
	private LibPrdCategoryCO convertModelToCO(LibPrdCategoryModel categoryModel) {
		LibPrdCategoryCO categoryCO = new LibPrdCategoryCO();
		try {
			BeanUtils.copyProperties(categoryCO, categoryModel);
		} catch (IllegalAccessException e) {
			throw new CustomGenericException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new CustomGenericException(e.getMessage());
		}
		return categoryCO;
	}
	
	/**
	 * @param model
	 * ** To initialize the create Category form objects
	 */
	private void initializeCreateCategoryFormObject(Map<String, Object> model){
		isRootMap = getIsRootMap();
		model.put("isRootMap", isRootMap);
		
		isActiveMap = getIsActiveMap();
		model.put("isActiveMap", isActiveMap);
		try {	
			clientCategories = libPrdCategoryBO.getAllClientCategoryNames();
			clientCategories.put(Long.valueOf("0"), ControllerConstants.DEFAULT_SELECT);
			model.put("clientCategories", clientCategories);
		}catch(Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
	}
	
	/**
	 * @param model
	 * ** To initialize the search Category form objects
	 */
	private void initializeSearchFormObject(Map<String, Object> model){
		CategorySearchCO categorySearchCO = new CategorySearchCO();
		model.put("categorySearchCO", categorySearchCO);
		
		searchByMap = getSearchByMap();
		model.put("searchByMap", searchByMap);
		
		filterByMap = getFilterByMap();
		model.put("filterByMap", filterByMap);
		
		isRootMap = getSearchIsRootMap();
		model.put("isRootMap", isRootMap);
		
		isActiveMap = getSearchIsActiveMap();
		model.put("isActiveMap", isActiveMap);
	}
	

	private void retrieveSearchFormObject(Map<String, Object> model,CategorySearchCO categorySearchCO1){
		CategorySearchCO categorySearchCO = categorySearchCO1;
		model.put("categorySearchCO", categorySearchCO);
		
		searchByMap = getSearchByMap();
		model.put("searchByMap", searchByMap);
		
		filterByMap = getFilterByMap();
		model.put("filterByMap", filterByMap);
		
		isRootMap = getSearchIsRootMap();
		model.put("isRootMap", isRootMap);
		
		isActiveMap = getSearchIsActiveMap();
		model.put("isActiveMap", isActiveMap);
	}
	
	/**
	 * @param model
	 * ** To initialize the create Category error objects
	 */
	private void initializeCreateCategoryErrorObject(Map<String, Object> model, LibPrdCategoryCO categoryCO){
		
		model.put("categoryCO", categoryCO);
		
		isRootMap = getIsRootMap();
		model.put("isRootMap", isRootMap);
		
		isActiveMap = getIsActiveMap();
		model.put("isActiveMap", isActiveMap);
		try {
			clientCategories = libPrdCategoryBO.getAllClientCategoryNames();
			clientCategories.put(Long.valueOf("0"), ControllerConstants.DEFAULT_SELECT);
			model.put("clientCategories", clientCategories);
		}catch(Exception e) {
			throw new CustomGenericException(e.getMessage());
		}		
		
	}
	
	private Map<String, String> getSearchByMap() {
		searchByMap = new LinkedHashMap<String, String>();
		searchByMap.put("all", "--Select--");
		searchByMap.put("name", "Category Name");
		searchByMap.put("shortDesc", "Short Desc");
		searchByMap.put("longDesc", "Long Desc");
		return searchByMap;
	}
	
	private Map<String, String> getFilterByMap() {
		filterByMap = new LinkedHashMap<String, String>();
		//filterByMap.put("all", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("CategoryFilterBy");
		for(ListManagementModel listModel:listOfModels){
			filterByMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		
		/*filterByMap.put("contains", "Contains");
		filterByMap.put("beginswith", "Begins With");
		filterByMap.put("endswith", "Ends With");
		filterByMap.put("equalto", "Equal to");*/
		return filterByMap;
	}
	
	private Map<String, String> getIsRootMap() {
		isRootMap = new LinkedHashMap<String, String>();
		
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("CategoryIsRoot");
		for(ListManagementModel listModel:listOfModels){
			isRootMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		/*	isRootMap.put("Yes", "Yes");
		isRootMap.put("No", "No");*/
		return isRootMap;
	}
	
	private Map<String, String> getIsActiveMap() {
		isActiveMap = new LinkedHashMap<String, String>();
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("CategoryIsActive");
		for(ListManagementModel listModel:listOfModels){
			isActiveMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		/*	isActiveMap.put("Yes", "Yes");
		isActiveMap.put("No", "No");*/
		return isActiveMap;
	}
	
	private Map<String, String> getSearchIsRootMap() {
		isRootMap = new LinkedHashMap<String, String>();
		isRootMap.put("All", "All");
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("CategoryIsRoot");
		for(ListManagementModel listModel:listOfModels){
			isRootMap.put(listModel.getListValue(), listModel.getListValue());
		}
		
		
		/*isRootMap.put("Yes", "Yes");
		isRootMap.put("No", "No");*/
		return isRootMap;
	}
	
	private Map<String, String> getSearchIsActiveMap() {
		isActiveMap = new LinkedHashMap<String, String>();
		
		
		isActiveMap.put("All", "All");
		
		List<ListManagementModel> listOfModels=listManagementBO.findListValues("CategoryIsActive");
		for(ListManagementModel listModel:listOfModels){
			isActiveMap.put(listModel.getListValue(), listModel.getListValue());
		}
		/*	isActiveMap.put("Yes", "Yes");
		isActiveMap.put("No", "No");*/
		return isActiveMap;
	}
	
	
	private long getClientCategoryId(String name){
		long id=0;
		try {
			id = libPrdCategoryBO.getClientCategoryId(name);
		} catch(Exception exception ){
			throw new CustomGenericException(exception.getMessage());
		}
		return id; 
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
	
	/**
	 * @param searchModel
	 * @param searchCO
	 * @throws Exception
	 * ** To convert the Search Category CO object to search Model
	 */
	private void convertSearchCOToSearchModel(SearchAttributeModel searchModel, CategorySearchCO searchCO) {
		
		try{
			searchCO.setKeyWord((searchCO.getKeyWord()).trim());
			BeanUtils.copyProperties(searchModel, searchCO);
		}catch(Exception e) {
			throw new CustomGenericException(e.getMessage());
		}
		/*if((searchCO.getIsActive()).equals("Yes")){
			searchModel.setActive(true);
		}else{
			searchModel.setActive(false);
		}
		if((searchCO.getIsRoot()).equals("Yes")){
			searchModel.setRoot(true);
		}else{
			searchModel.setRoot(false);
		}*/
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
	
	/**
	 * @param LibPrdCategoryCO
	 * @param model
	 * ** To convert the String values as Boolean to set in Model
	 */
	private void convertStringToBoolean(LibPrdCategoryCO categoryCO, LibPrdCategoryModel model){
		if((categoryCO.getIsRoot() != null) && (categoryCO.getIsRoot()).equals(ControllerConstants.YES)){
			model.setRoot(true);
		}else{
			model.setRoot(false);
		}
		if((categoryCO.getIsActive() != null) && (categoryCO.getIsActive()).equals(ControllerConstants.YES)){
			model.setActive(true);
		}else{
			model.setActive(false);
		}
		if((categoryCO.getIsMapped() != null) && (categoryCO.getIsMapped()).equals(ControllerConstants.YES)){
			model.setMapped(true);
		}else{
			model.setMapped(false);
		}
		if((categoryCO.getHasValue() != null) && (categoryCO.getHasValue()).equals(ControllerConstants.YES)){
			model.setHasValue(true);
		}else{
			model.setHasValue(false);
		}
	}
	
	/**
	 * 
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
	
	
}
