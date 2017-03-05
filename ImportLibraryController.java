/**
 * 
 */
package com.skuview.common.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.skuview.category.bo.CategoryBO;
import com.skuview.common.command.ExportFile;
import com.skuview.controller.constants.ControllerConstants;
import com.skuview.exception.AttributeIdViolation;
import com.skuview.exception.UniqueLibraryAttributeViolation;
import com.skuview.exception.UniqueNameViolation;
import com.skuview.export.bo.ExportBO;
import com.skuview.fileimport.LibraryFileImport;
import com.skuview.listmanagement.ListManagementBO;
import com.skuview.productattribute.ProdAttrBO;
import com.skuview.service.bo.RoleService;
import com.skuview.user.model.Roles;
import com.skuview.user.model.UserDetails;

/**
 * @author 271971
 *
 */

@Controller
public class ImportLibraryController {
	
	@Autowired
	public CategoryBO categoryBO;

	@Autowired
	public ProdAttrBO prodAttrBO; 
	
	@Autowired
	public ListManagementBO listMgmtBO; 
	
	@Autowired
	public RoleService roleBO;
	
	@Autowired
	public ExportBO exportBO;

	
	@RequestMapping(value = "/importLibrary", method = RequestMethod.GET)
	public String createCategory(Map model,ExportFile eportFile,
			Map<String, Object> batchModel, HttpServletRequest request) {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(batchModel, userSess.getRole());
		model.put("eportFile", eportFile);
		List<String> batchNames = addLibrary();
		batchModel.put("batchNames", batchNames);
		//UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
		return ControllerConstants.PAGE_IMPORT_LIBRARY;
	}
	
	@RequestMapping(value = "/fileUpload", method = RequestMethod.POST)
	public ModelAndView imageUpload(@ModelAttribute("eportFile") ExportFile reportFile,
			@RequestParam("file") MultipartFile file,Map<String, Object> batchModel, HttpServletRequest request) {
		UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(batchModel, user.getRole());
		boolean success = false;
		List<String> batchNames = addLibrary();
		batchModel.put("batchNames", batchNames);
		ModelAndView modelandview = new ModelAndView(ControllerConstants.PAGE_IMPORT_LIBRARY);
		String[] fileArray = file.getOriginalFilename().split("\\.");

		if (!file.isEmpty() && !ControllerConstants.SELECT.equalsIgnoreCase(reportFile.getBatchNames())
				&& null != fileArray[1] && fileArray[1].equalsIgnoreCase(ControllerConstants.FORMAT_XLSX)) {
			try{
				LibraryFileImport libImport = new LibraryFileImport();
				List<Map<String, String>> fileMap = libImport.parseFile(file);
				if(ControllerConstants.PROD_ATTRIBUTE.equalsIgnoreCase(reportFile.getBatchNames())){
					success = prodAttrBO.insertSystemProdAttribute(fileMap);
				} else if (ControllerConstants.CATEGORy.equalsIgnoreCase(reportFile.getBatchNames())){
					success = categoryBO.insertSystemCategory(fileMap);
				} else if (ControllerConstants.EXPORT_TEMPLATE.equalsIgnoreCase(reportFile.getBatchNames())){
					success = exportBO.insertExportTemplate(fileMap);
				}else {
					success = listMgmtBO.insertSystemList(fileMap);
				}
				
				if(!success){
					modelandview.addObject("imgMsg", ControllerConstants.FILE_UPLOAD_FAILED);
				} else{
					modelandview.addObject("imgMsg", "success");
				}
				
			} catch (UniqueLibraryAttributeViolation e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.UNIQUE_LIBRARY_ATTR_FAILED);
				return modelandview;
			} catch (UniqueNameViolation e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.EXPORT_TEMPLATE_NAME_FAILED);
				return modelandview;
			} catch (AttributeIdViolation e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.EXPORT_TEMPLATE_ATTRIBUTE_FAILED);
				return modelandview;
			} catch (IOException e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.FILE_UPLOAD_FAILED);
				return modelandview;
			} catch (Exception e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.FILE_UPLOAD_FAILED);
				return modelandview;
			}
		} else if(ControllerConstants.SELECT.equalsIgnoreCase(reportFile.getBatchNames())){
			modelandview.addObject("imgMsg", ControllerConstants.LIBRARY_REQUIRED);
		} else if(file.isEmpty()){
			modelandview.addObject("imgMsg", ControllerConstants.FILE_REQUIRED);
		} else if(null != fileArray[1] && !ControllerConstants.FORMAT_XLSX.equalsIgnoreCase(fileArray[1])){
			modelandview.addObject("imgMsg", ControllerConstants.FILE_INVALID);
		} 
		return modelandview;
	}
	
	private List<String> addLibrary(){
		
		List<String> libraryNames = new ArrayList<String>();
		libraryNames.add(ControllerConstants.SELECT);
		libraryNames.add(ControllerConstants.PROD_ATTRIBUTE);
		libraryNames.add(ControllerConstants.CATEGORy);
		libraryNames.add(ControllerConstants.DROPDOWN_LIST);
		libraryNames.add(ControllerConstants.EXPORT_TEMPLATE);
		return libraryNames;
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

}
