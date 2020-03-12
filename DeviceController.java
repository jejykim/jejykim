/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.decision.v2x.era.web;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.decision.v2x.era.service.impl.CertificationService;
import com.decision.v2x.era.service.impl.CodeService;
import com.decision.v2x.era.service.impl.DeviceService;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.service.impl.PermissionService;
import com.decision.v2x.era.VO.CertificatesKeyVO;
import com.decision.v2x.era.VO.CertificationVO;
import com.decision.v2x.era.VO.CodeVO;
import com.decision.v2x.era.VO.DeviceVO;
import com.decision.v2x.era.VO.PagingVO;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.convert.LogHelper;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator;
import com.decision.v2x.era.util.log.LogGenerator.Common;
import com.decision.v2x.era.util.log.LogGenerator.Location;
import com.decision.v2x.era.util.paging.PagingControl;
import com.decision.v2x.era.util.setting.SettingManagement;

import egovframework.rte.psl.dataaccess.util.EgovMap;

/**
 * @Class Name : DeviceController.java
 * @Description : Device Controller Class
 * @Modification Information
 * @
 * @  수정일      수정자              수정내용
 * @ ---------   ---------   -------------------------------
 * @ 2019.12.04  김진열         최초생성
 *
 * @author V2X_ERA DECISION 개발팀
 * @since 2019.12.04
 * @version 1.0
 * @see
 *
 *  Copyright (C) by DECISION All right reserved.
 */

@Controller
public class DeviceController 
{
	static Logger logger = Logger.getLogger(DeviceController.class);

	
	@Resource(name = "permissionService")
	private PermissionService permissionService;
	
	@Resource(name = "logService")
	private LogService logService;
	
	private PermissionManagement pm = null;
	private LogGenerator lg = null;
	private LogHelper lh = new LogHelper();
	
	@PostConstruct
	public void init() throws Exception
	{
		RestApi.disableSslVerification();
		pm = new PermissionManagement(permissionService);
		lg = new LogGenerator(logService);
	}
	
	@Resource(name = "certificationService")
	private CertificationService certService;
	
	
	/** DeviceService */
	@Resource(name = "deviceService")
	private DeviceService deviceService;
	
	/** CodeService */
	@Resource(name = "codeService")
	private CodeService codeService;
	
	@Resource(name = "multipartResolver")
	CommonsMultipartResolver multipartResolver;
	
	/**
	 * 단말 목록을 조회한다. (pageing)
	 * @param deviceVO - 조회할 정보가 담긴 DeviceVO
	 * @param model
	 * @return "devices/deviceList"
	 * @exception Exception
	 */
	@RequestMapping(value = {"/devices/list.do", "/devices/search.do"})
	public String selectDeviceList(@ModelAttribute("deviceVO") DeviceVO deviceVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "devices/deviceList";
		int deviceListCount = 0;
		
		try
		{
			Location loc = null;
			String requestUri = req.getRequestURI();
			if(requestUri.indexOf("devices/list") != -1)
			{
				loc = Location.DeviceList;
			}
			else
			{
				loc = Location.DeviceSearch;
			}
			
			if(pm == null)
			{
				return "redirect:/login.do";
			}

			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "DB 접속 정보를 확인해주세요");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		try
		{
			String urlType = "";
			if(req.getRequestURI().indexOf("devices/list") != -1)
			{
				urlType = "L";
			}
			else
			{
				urlType = "S";
			}
			
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) 
			{
				strReturn = "redirect:/login.do";
				
				if(urlType.equals("L"))
				{
					lg.insertLog(Location.DeviceList, Common.Fail, PermissionManagement.getUserId(req), 
							lh.list(req, 0, 0, 0, "로그인이 되어 있지 않습니다."));
				}
				else
				{
					lg.insertLog(Location.DeviceSearch, Common.Fail, PermissionManagement.getUserId(req), 
							lh.list(req, 0, 0, 0, "로그인이 되어 있지 않습니다."));
				}
				
			}
			else 
			{				
				if(!deviceVO.getDevice_id_type_group().isEmpty()) {
					String[] arrData = deviceVO.getDevice_id_type_group().split(":");
					deviceVO.setDevice_id_type(arrData[0]);
					deviceVO.setDevice_id_type_group(arrData[1]);
				}
				
				if(!deviceVO.getDevice_type().isEmpty()) {
					String[] arrData = deviceVO.getDevice_type().split(":");
					deviceVO.setDevice_type(arrData[0]);
					deviceVO.setDevice_type_group(arrData[1]);
				}
				
				if(deviceVO.getNow_page() != 1) {
					deviceVO.setPage((deviceVO.getNow_page()-1)*deviceVO.getPage_per_data());
				}
	
				List<?> deviceList = deviceService.selectDeviceList(deviceVO);
				deviceListCount = deviceService.selectDeviceListCount(deviceVO);
				List<?> deviceIdTypeList = codeService.getDeviceIdTypeList();
				List<?> deviceTypeList = codeService.getDeviceTypeList();
				
				PagingControl pc = new PagingControl();
				PagingVO pagingVO = pc.paging(deviceListCount, deviceVO.getNow_page(), deviceVO.getPage_per_data());

				model.addAttribute("deviceList", deviceList);
				model.addAttribute("deviceListCount", deviceListCount);
				model.addAttribute("deviceIdTypeList", deviceIdTypeList);
				model.addAttribute("deviceTypeList", deviceTypeList);
				model.addAttribute("deviceVO", deviceVO);
				model.addAttribute("searchStartDate", deviceVO.getStart_time());
				model.addAttribute("searchEndDate", deviceVO.getEnd_time());
				model.addAttribute("pagingVO", pagingVO);
				
				if(urlType.equals("L"))
				{
					lg.insertLog(Location.DeviceList, Common.Success, PermissionManagement.getUserId(req), 
							lh.list(req, pagingVO.getPage_per_rows(), pagingVO.getNow_page(), pagingVO.getTotal_count(), ""));
				}
				else
				{
					lg.insertLog(Location.DeviceSearch, Common.Success, PermissionManagement.getUserId(req), 
							lh.deviceSearch(req, pagingVO.getPage_per_rows(), pagingVO.getNow_page(), pagingVO.getTotal_count(), 
									deviceVO.getStart_time(), deviceVO.getEnd_time(), deviceVO.getDevice_name(), deviceVO.getDevice_sn(), deviceVO.getDevice_type(), deviceVO.getDevice_group_id(), ""));
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			lg.insertLog(Location.DeviceSearch, Common.Error, PermissionManagement.getUserId(req), 
					lh.deviceSearch(req, deviceVO.getPage_per_data(), deviceVO.getNow_page(), deviceListCount, 
							deviceVO.getStart_time(), deviceVO.getEnd_time(), deviceVO.getDevice_name(), deviceVO.getDevice_sn(), deviceVO.getDevice_type(), deviceVO.getDevice_group_id(), lh.error(e).toString()));
		
		}
		return strReturn;
	}
	
	/**
	 * 단말을 추가한다.
	 * @param deviceVO - 조회할 정보가 담긴 DeviceVO
	 * @param model
	 * @return "devices/deviceList"
	 * @exception Exception
	 */
	@RequestMapping(value = "/devices/add.do")
	public String addDevice(@ModelAttribute("deviceVO") DeviceVO deviceVO, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model) throws Exception {
		String strReturn = "devices/addDevice";
		
		try
		{
			Location loc = Location.DeviceAdd;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "DB 접속 정보를 확인해주세요");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		
		
		try
		{
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) 
			{
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.DeviceAdd, Common.SessionExpire, PermissionManagement.getUserId(req), 
						lh.deviceAdd(req, "", "세션이 만료됨"));
			}
			else 
			{
				List<?> deviceTypeList = codeService.getDeviceTypeList();
				List<?> deviceCountryList = codeService.getDeviceCountryList();
				List<?> deviceIdTypeList = codeService.getDeviceIdTypeList();
				List<?> ObuPsidList = codeService.getObuPsidList();
				List<?> RsuPsidList = codeService.getRsuPsidList();
				
				model.addAttribute("deviceTypeList", deviceTypeList);
				model.addAttribute("deviceCountryList", deviceCountryList);
				model.addAttribute("deviceIdTypeList", deviceIdTypeList);
				model.addAttribute("ObuPsidList", ObuPsidList);
				model.addAttribute("RsuPsidList", RsuPsidList);
				
				lg.insertLog(Location.DeviceAdd, Common.Move, PermissionManagement.getUserId(req), 
						lh.deviceAdd(req, "", ""));
			}
		}
		catch(Exception e)
		{
			lg.insertLog(Location.DeviceAdd, Common.Error, PermissionManagement.getUserId(req), 
					lh.deviceAdd(req, "", lh.error(e).toString()));
		}
		return strReturn;
	}
	
	@RequestMapping(value = "/devices/addOK.do", method = RequestMethod.POST)
	public String addDeviceOK(@ModelAttribute DeviceVO deviceVO, ModelMap model, HttpServletRequest req, HttpServletResponse resp)
			throws Exception
	{
		String strReturn = "redirect:/devices/list.do";
		SettingManagement setting = LoginController.setting;
		String fileName = "";
		String deviceIdTypeSplit = "";
		String deviceTypeSplit = "";
		
		try
		{
			Location loc = Location.DeviceAdd;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "DB 접속 정보를 확인해주세요");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		
		try
		{
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) 
			{
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.DeviceAdd, Common.SessionExpire, PermissionManagement.getUserId(req), 
						lh.deviceAdd(req, deviceVO.getDevice_sn(), "세션이 만료됨"));
			}
			else
			{
				/*
				// upload CSR 성공 시
				if(deviceVO.getDevice_CSR().getSize() > 0)
				{
					deviceVO.setDevice_cert_type("U");
					fileName = deviceVO.getDevice_CSR().getOriginalFilename();
				}
				else 
				{
					deviceVO.setDevice_cert_type("M");
				}
				//*/
				deviceVO.setDevice_cert_type(null);
				deviceVO.setDevice_group_id(setting.getCompanyCode());
				deviceVO.setDevice_group_id_group(setting.getCompanyGroup());
			
				deviceVO.setCreate_id(req.getSession().getAttribute("user_id").toString());
				
				//단말 ID의 종류
				deviceIdTypeSplit = deviceVO.getDevice_id_type_group();
				String[] deviceIdTypeAndGroup = deviceIdTypeSplit.split(":");
				deviceVO.setDevice_id_type(deviceIdTypeAndGroup[0]);
				deviceVO.setDevice_id_type_group(deviceIdTypeAndGroup[1]);
				

				deviceTypeSplit = deviceVO.getDevice_type();
				String [] deviceTypeAndGroup = deviceTypeSplit.split(":");
				deviceVO.setDevice_type(deviceTypeAndGroup[0]);
				deviceVO.setDevice_type_group(deviceTypeAndGroup[1]);
				
				String message = "잘못된 경로입니다";
				String strResult = "N";
				
				int iResult = deviceService.insertDevice(deviceVO);
				
				if(iResult > 0)
				{
					PermissionManagement.setSessionValue(req, "succMsg", "단말 등록이 정상적으로 처리가 되었습니다.");
					lg.insertLog(Location.DeviceAdd, Common.Success, PermissionManagement.getUserId(req), 
							lh.deviceAdd(req, deviceVO.getDevice_sn(), deviceIdTypeSplit, deviceTypeSplit, 
									deviceVO.getDevice_maker(), deviceVO.getDevice_name(), deviceVO.getDevice_number(), 
									deviceVO.getDevice_ver(), fileName, ""));
					//단말 추가 로그
					/*
					if(deviceVO.getDevice_CSR().getSize() > 0) 
					{
						MultipartFile file = deviceVO.getDevice_CSR();
						DeviceVO tmp = deviceService.selectSN(deviceVO.getDevice_sn());
						RestApi api = PermissionManagement.getApi(req);

						if(api.isToken())
						{
							JSONObject result = api.uploadCsr(file.getBytes(), file.getOriginalFilename(), tmp.getDevice_sn());
							CertificationVO vo  = new CertificationVO();
							CertificatesKeyVO kVo = new CertificatesKeyVO();
						
							if(result.get("success").equals("Y"))
							{
								JSONArray arr = (JSONArray)result.get("results");
								for(int i = 0; i<arr.size(); i++)
								{
									JSONObject item = (JSONObject)arr.get(i);
									String requestHash = (String)item.get("request_hash");
									JSONArray arrEnrol = (JSONArray)item.get("enrols");
									
									for(int q = 0; q<arrEnrol.size(); q++)
									{
										String key = certService.selectId();
										JSONObject arrEnrolItem = (JSONObject)arrEnrol.get(q);										
										String publicKey = arrEnrolItem.get("public_key").toString();
										String enrolHash = arrEnrolItem.get("enrol_hash").toString();
										String enrolValidityStart = arrEnrolItem.get("enrol_validity_start").toString();
										String enrolValidityEnd = arrEnrolItem.get("enrol_validity_end").toString();
									
										
										vo.setCert_id(key);
										vo.setDevice_id(Long.parseLong(tmp.getDevice_id()));
										vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
										vo.setCert_state("2");
										vo.setCert_state_group_id("ECG005");
										vo.setExpiry_date(enrolValidityEnd);
										vo.setIssue_date(enrolValidityStart);
										vo.setCert_type("3");
										vo.setCert_type_group("ECG028");
										
										kVo.setCert_enrol_id(key);
										kVo.setCert_sec_id(null);
										kVo.setRequest_hash(requestHash);
										kVo.setPublic_key(publicKey);	
										
										this.certService.insertCert(vo);
										this.certService.insertCertKey(kVo);
									}
								}
							}
						}
					}
					//*/
				}
				else 
				{
					message = "틍록 실패";
					strResult = "N";
					strReturn = "redirect:/devices/add.do";
					lg.insertLog(Location.DeviceAdd, Common.Fail, PermissionManagement.getUserId(req), 
							lh.deviceAdd(req, deviceVO.getDevice_sn(), deviceIdTypeSplit, deviceTypeSplit, 
									deviceVO.getDevice_maker(), deviceVO.getDevice_name(), deviceVO.getDevice_number(), 
									deviceVO.getDevice_ver(), fileName, "단말 insert 실패(DB 오류)"));
					
					PermissionManagement.setSessionValue(req, "errMsg", "단말 등록이 실패하였습니다. 자세한 내용은 로그를 확인해주세요");
				}
				
			}
		}
		catch(Exception e)
		{
			lg.insertLog(Location.DeviceAdd, Common.Error, PermissionManagement.getUserId(req), 
					lh.deviceAdd(req, deviceVO.getDevice_sn(), deviceIdTypeSplit, deviceTypeSplit, 
							deviceVO.getDevice_maker(), deviceVO.getDevice_name(), deviceVO.getDevice_number(), 
							deviceVO.getDevice_ver(), fileName, lh.error(e).toString()));
			
			PermissionManagement.setSessionValue(req, "errMsg", "단말 등록이 실패하였습니다. 자세한 내용은 로그를 확인해주세요");
		}
		
		
		return strReturn;
	}
	
	// 단말 상세
	@RequestMapping(value = "/devices/show.do")
	public String deviceInfo(@ModelAttribute("deviceVO") DeviceVO deviceVO, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model) 
			throws Exception 
	{
		String strReturn = "devices/deviceInfo";
		
		try
		{
			Location loc = Location.DeviceAdd;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "DB 접속 정보를 확인해주세요");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		
		try
		{
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) 
			{
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.DeviceDetail, Common.SessionExpire, PermissionManagement.getUserId(req), 
						lh.deviceDetail(req, deviceVO.getDevice_sn(), "세션이 만료됨"));
			}
			else 
			{
				if(deviceVO.getDevice_id().isEmpty())
				{
					Map<String, ?> map = RequestContextUtils.getInputFlashMap(req);
					if(map != null) 
					{
						deviceVO.setDevice_id((String) map.get("device_id"));
					}
					else 
					{
						strReturn = "redirect:/devices/list.do";
					}
				}
				else 
				{
					DeviceVO resultVO = deviceService.selectDeviceInfo(deviceVO);
					List<?> deviceTypeList = codeService.getDeviceTypeList();
					List<?> deviceIdTypeList = codeService.getDeviceIdTypeList();
					
					int iResult = deviceService.checkIssueDevice(Long.parseLong(deviceVO.getDevice_id()));
					String strResult = "N";
					if(iResult > 0) 
					{
						strResult = "Y";
					}
					
					model.addAttribute("deviceIdTypeList", deviceIdTypeList);
					model.addAttribute("deviceVO", resultVO);
					model.addAttribute("deviceTypeList", deviceTypeList);
					model.addAttribute("strResult", strResult);
					
					lg.insertLog(Location.DeviceDetail, Common.Success, PermissionManagement.getUserId(req), 
							lh.deviceDetail(req, deviceVO.getDevice_sn(), ""));
				}
			}
		}
		catch(Exception e)
		{
			lg.insertLog(Location.DeviceDetail, Common.Error, PermissionManagement.getUserId(req), 
					lh.deviceDetail(req, deviceVO.getDevice_sn(), lh.error(e).toString()));
		}
		return strReturn;
	}
	
	// 단말 삭제
	@RequestMapping(value = "/devices/delete.do")
	public String deleteDevice(@RequestParam("device_id") String device_id, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model) 
			throws Exception 
	{
		String strReturn = "redirect:/devices/list.do";
	
		
		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				return "redirect:/login.do";	
			}

			Location loc = Location.DeviceDelete;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "DB 접속 정보를 확인해주세요");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		
		try
		{
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) 
			{
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.DeviceDelete, Common.SessionExpire, PermissionManagement.getUserId(req), 
						lh.deviceDelete(req, "", "", "", "", "세션이 만료됨"));
			}
			else 
			{
				RestApi api = PermissionManagement.getApi(req);
				if(api != null)
				{
					String errMsg = "";
					String[] arrDevice_id = device_id.split(",");
					for(String str : arrDevice_id) 
					{
						String[] deviceInfo = str.split(":");
						String deviceId = deviceInfo[0];
						String deviceSn = deviceInfo[1];
						
						//*
						List<Map<String, Object>> deviceList = deviceService.selectDevicePublicKey(Long.parseLong(deviceId));
						boolean bFail = false;
						
						//블랙 리스트 등록
						for(int q = 0; q<deviceList.size(); q++)
						{
							Map<String, Object> item = deviceList.get(q);
							//String deviceId = item.get("device_id").toString();
							//String deviceSn = item.get("device_sn").toString();
							String key = item.get("public_key").toString();
							String certId = item.get("cert_id").toString();
							JSONObject apiResult = api.certBlacklist(key);
							if(apiResult.get("success").equals("N"))
							{
								bFail = true;
								lg.insertLog(Location.DeviceDelete, Common.Fail, PermissionManagement.getUserId(req), 
										lh.deviceDelete(req, deviceSn, key, deviceId, certId, "blacklist 등록 실패"));
							}
							else
							{
								CertificationVO certVO = new CertificationVO();
								
								certVO.setCert_id(certId);
								certVO.setCert_state("7");
								certVO.setCert_state_group_id("ECG005");
								
								certService.updateChangeStateCert(certVO);
								lg.insertLog(Location.DeviceDelete, Common.Success, PermissionManagement.getUserId(req), 
										lh.deviceDelete(req, deviceSn, key, deviceId, certId, "blacklist 등록 성공"));
							}

						}
						
						
						//삭제
						if(bFail == false)
						{
							deviceService.deleteDevice(Long.parseLong(deviceId));
							deviceService.deletePublicKey(Long.parseLong(deviceId));
						
							lg.insertLog(Location.DeviceDelete, Common.Success, PermissionManagement.getUserId(req), 
									lh.deviceDelete(req, deviceSn, "", deviceId, "", "단말 삭제 성공"));
							PermissionManagement.setSessionValue(req, "succMsg", "단말을 정상적으로 제거하였습니다.");
						}
						else
						{
							PermissionManagement.setSessionValue(req, "errMsg", "단말을 삭제하지 못했습니다. 자세한 내용은 로그를 확인해주세요.");
						}
						//*/
					}
				}
				else
				{
					strReturn = "redirect:/login.do";
					lg.insertLog(Location.DeviceDelete, Common.SessionExpire, PermissionManagement.getUserId(req), 
							lh.deviceDelete(req, "", "", "", "", "세션이 만료됨"));
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			lg.insertLog(Location.DeviceDelete, Common.Error, PermissionManagement.getUserId(req), 
					lh.deviceDelete(req, device_id, "", "", "", lh.error(e).toString()));
		}
		
		return strReturn;
	}
	
	// 단말 수정
	@RequestMapping(value = "/devices/change.do")
	public String modifyDevice(@ModelAttribute("deviceVO") DeviceVO deviceVO, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model ,RedirectAttributes redirectAttributes) 
			throws Exception 
	{
		String strReturn = "redirect:/devices/show.do";
		SettingManagement setting = LoginController.setting;
		redirectAttributes.addAttribute("device_id", deviceVO.getDevice_id());
		
		try
		{
			Location loc = Location.DeviceChange;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "DB 접속 정보를 확인해주세요");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) 
		{
			strReturn = "redirect:/login.do";
			lg.insertLog(Location.DeviceChange, Common.SessionExpire, PermissionManagement.getUserId(req), 
					lh.deviceChange(req, deviceVO.getDevice_sn(), "")
					);
		}
		else
		{
			// 단말 타입
			String deviceTypeSplit = deviceVO.getDevice_type();
			String [] deviceTypeAndGroup = deviceTypeSplit.split(":");
			deviceVO.setDevice_type(deviceTypeAndGroup[0]);
			deviceVO.setDevice_type_group(deviceTypeAndGroup[1]);
			
			/*
			// upload CSR 성공 시
			if(deviceVO.getDevice_CSR().getSize() > 0)
			{
				deviceVO.setDevice_cert_type("U");
			}
			else 
			{
				deviceVO.setDevice_cert_type("M");
			}
			//*/
			deviceVO.setDevice_group_id(setting.getCompanyCode());
			deviceVO.setDevice_group_id_group(setting.getCompanyGroup());
			
			int iResult = deviceService.updateDevice(deviceVO);
			
			if(iResult > 0) 
			{
				if(deviceVO.getDevice_CSR().getSize() > 0) 
				{
					MultipartFile file = deviceVO.getDevice_CSR();
					DeviceVO tmp = deviceService.selectSN(deviceVO.getDevice_sn());
					String key = certService.selectId();
					//RestApi api = new RestApi("dcmadmin", "admin");					
					RestApi api = PermissionManagement.getApi(req);			
					
					/*
					try
					{
						api.DCMLogin();
					}
					catch(Exception e)
					{
						//DCM ERROR
						e.printStackTrace();
					}
					//*/
					
					if(api.isToken())
					{
						JSONObject result = api.uploadCsr(file.getBytes(), file.getOriginalFilename(), tmp.getDevice_sn());
						CertificationVO vo  = new CertificationVO();
						CertificatesKeyVO kVo = new CertificatesKeyVO();
						
						vo.setCert_id(key);
						vo.setDevice_id(Long.parseLong(tmp.getDevice_id()));
						vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
						vo.setCert_state("2");
						vo.setCert_state_group_id("ECG005");
						vo.setExpiry_date(result.get("enrol_validity_end").toString());
						vo.setIssue_date(result.get("enrol_validity_start").toString());
						vo.setCert_type("3");
						vo.setCert_type_group("ECG028");
						
						kVo.setCert_enrol_id(key);
						kVo.setCert_sec_id(null);
						kVo.setRequest_hash(result.get("request_hash").toString());
						kVo.setPublic_key(result.get("public_key").toString());				
						
						try
						{
							this.certService.insertCert(vo);
							this.certService.insertCertKey(kVo);
							
							lg.insertLog(Location.DeviceChange, Common.Success, PermissionManagement.getUserId(req), 
									lh.deviceChange(req, deviceVO.getDevice_sn(), "")
									);
						}
						catch(Exception e)
						{
							//insert ERROR
							e.printStackTrace();
							lg.insertLog(Location.DeviceChange, Common.Error, PermissionManagement.getUserId(req), 
									lh.deviceChange(req, deviceVO.getDevice_sn(), lh.error(e).toString())
									);
						}
						
					}
				}
			}
			else 
			{
				lg.insertLog(Location.DeviceChange, Common.Fail, PermissionManagement.getUserId(req), 
						lh.deviceChange(req, deviceVO.getDevice_sn(), "Update 실패")
						);
				strReturn = "redirect:/devices/list.do";
			}
		}
		
		return strReturn;
	}
		
	@ResponseBody
	@RequestMapping(value = "/devices/check.do", produces="application/text;charset=utf8;")
	public String checkDeviceSn(@RequestParam("device_sn") String device_sn, ModelMap model, HttpServletRequest req) throws Exception 
	{
		JSONObject json = new JSONObject();
		int iResult = deviceService.selectCheckDeviceSn(device_sn);
		if(iResult > 0) {
			json.put("result", "1");
			json.put("msg", "이미 등록 된 단말 입니다.");
		}else {
			json.put("result", "0");
			json.put("msg", "등록 가능한 단말 입니다.");
		}
		
		return json.toString();
	}

}
