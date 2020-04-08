package com.decision.v2x.dcm.web;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.acl.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.spi.DirStateFactory.Result;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.decision.v2x.dcm.VO.CertSettingVO;
import com.decision.v2x.dcm.VO.CertificatesKeyVO;
import com.decision.v2x.dcm.VO.CertificationVO;
import com.decision.v2x.dcm.VO.DeviceAndCertListVO;
import com.decision.v2x.dcm.VO.DeviceVO;
import com.decision.v2x.dcm.VO.PagingVO;
import com.decision.v2x.dcm.VO.SecCertWaitVO;
import com.decision.v2x.dcm.mapper.SecCertWaitMapper;
import com.decision.v2x.dcm.service.impl.CertSettingService;
import com.decision.v2x.dcm.service.impl.CertificationService;
import com.decision.v2x.dcm.service.impl.CodeService;
import com.decision.v2x.dcm.service.impl.DeviceService;
import com.decision.v2x.dcm.service.impl.LogService;
import com.decision.v2x.dcm.service.impl.PermissionService;
import com.decision.v2x.dcm.service.impl.SecCertWaitService;
import com.decision.v2x.dcm.util.Exception.EmptyUserException;
import com.decision.v2x.dcm.util.Exception.NullSessionException;
import com.decision.v2x.dcm.util.auth.PermissionManagement;
import com.decision.v2x.dcm.util.convert.DateHelper;
import com.decision.v2x.dcm.util.convert.LogHelper;
import com.decision.v2x.dcm.util.convert.RestApiParameter;
import com.decision.v2x.dcm.util.convert.ZipHelper;
import com.decision.v2x.dcm.util.http.RestApi;
import com.decision.v2x.dcm.util.log.LogGenerator;
import com.decision.v2x.dcm.util.log.LogGenerator.Common;
import com.decision.v2x.dcm.util.log.LogGenerator.Location;
import com.decision.v2x.dcm.util.paging.PagingControl;
import com.ibatis.common.io.ReaderInputStream;

import egovframework.rte.psl.dataaccess.util.EgovMap;

@Controller
public class CertificationController 
{
	Logger logger = Logger.getLogger(CertificationController.class);
	
	@Resource(name = "secCertWaitService")
	private SecCertWaitService secCertWaitService;
	
	@Resource(name = "codeService")
	private CodeService codeService;
 
	@Resource(name = "certificationService")
	private CertificationService certService;
	
	@Resource(name = "permissionService")
	private PermissionService permissionService;
	
	@Resource(name = "logService")
	private LogService logService;
	
	@Resource(name = "certSettingService")
	private CertSettingService certSettingService;
	
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
	
	
	static boolean isCheck(String item)
	{
		if(item != null && !item.isEmpty())
		{
			return true;
		}
		
		return false;
	}
	
	// 등록인증서 목록
	@RequestMapping(value = {"/cert/enrol/list.do", "/cert/enrol/search.do"})
	public String enrolList(@ModelAttribute("deviceAndCertListVO") DeviceAndCertListVO deviceAndCertListVO, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				return "redirect:/login.do";
			}
			
			Location loc = null;
			if(req.getRequestURI().indexOf("enrol/list") != -1)
			{
				loc = Location.EnrolList;
			}
			else
			{
				loc = Location.EnrolSearch;
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
			return "redirect:/login.do";
		}
		
		int totalCnt = 0;
		
		try
		{
			if(deviceAndCertListVO.getNow_page() != 1) {
				deviceAndCertListVO.setPage((deviceAndCertListVO.getNow_page()-1)*deviceAndCertListVO.getPage_per_data());
			}
			
			if(!deviceAndCertListVO.getDevice_sn_type_id().isEmpty()) {
				String[] arrDeviceSnType = deviceAndCertListVO.getDevice_sn_type_id().split(":");
				deviceAndCertListVO.setDevice_sn_type_id(arrDeviceSnType[0]);
				deviceAndCertListVO.setDevice_sn_type_group(arrDeviceSnType[1]);
			}
			
			if(!deviceAndCertListVO.getDevice_type().isEmpty()) {
				String[] arrDeviceType = deviceAndCertListVO.getDevice_type().split(":");
				deviceAndCertListVO.setDevice_type(arrDeviceType[0]);
				deviceAndCertListVO.setDevice_type_group(arrDeviceType[1]);
			}
			
			model.addAttribute("deviceIdTypeList", codeService.getDeviceIdTypeList());
			model.addAttribute("deviceTypeList", codeService.getDeviceTypeList());
			
			totalCnt = certService.selectEnrolCertCnt(deviceAndCertListVO);
			
			PagingControl pc = new PagingControl();
			PagingVO pagingVO = pc.paging(totalCnt, deviceAndCertListVO.getNow_page(), deviceAndCertListVO.getPage_per_data());
			
			List<Map<String, Object>> list = certService.selectEnrolCertList(deviceAndCertListVO);
			
			List<Map<String, Object>> certList = new ArrayList<>();
			
			for(Map<String, Object> map : list) {
				
				String issue_date = (map.get("issue_date") == null) ? "" : map.get("issue_date").toString();
				String expiry_date = (map.get("expiry_date") == null) ? "" : map.get("expiry_date").toString();
				
				map.put("issue_date", (issue_date.isEmpty() ? "-" : issue_date.substring(0, issue_date.length()-2)));
				map.put("expiry_date", (expiry_date.isEmpty() ? "-" : expiry_date.substring(0, expiry_date.length()-2)));
				
				certList.add(map);
			}
			
 			model.addAttribute("certList", certList);
 			model.addAttribute("certTotalCnt", totalCnt);
 			model.addAttribute("enrolTotalCnt", certService.selectIssueCertCnt(deviceAndCertListVO));
 			model.addAttribute("deviceAndCertListVO", deviceAndCertListVO);
 			model.addAttribute("issueDateStart", deviceAndCertListVO.getIssue_date_start());
 			model.addAttribute("issueDateEnd", deviceAndCertListVO.getIssue_date_end());
 			model.addAttribute("expireDateStart", deviceAndCertListVO.getExpire_date_start());
 			model.addAttribute("expireDateEnd", deviceAndCertListVO.getExpire_date_end());
 			model.addAttribute("pagingVO", pagingVO);
			
			if(req.getRequestURI().indexOf("enrol/list") != -1)
			{
				lg.insertLog(Location.EnrolList, Common.Success, PermissionManagement.getUserId(req), 
						lh.list(req, deviceAndCertListVO.getPage_per_data(), deviceAndCertListVO.getNow_page(), totalCnt, ""));
			}
			else
			{
				lg.insertLog(Location.EnrolSearch, Common.Success, PermissionManagement.getUserId(req), 
					lh.certSearch(req, deviceAndCertListVO.getPage_per_data(), deviceAndCertListVO.getNow_page(), totalCnt,
							deviceAndCertListVO.getIssue_date(), deviceAndCertListVO.getExpiry_date(), deviceAndCertListVO.getCreate_date_start(),
							deviceAndCertListVO.getModify_date_start(), deviceAndCertListVO.getDevice_sn(), "", ""));
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			if(req.getRequestURI().indexOf("enrol/list") != -1)
			{
				lg.insertLog(Location.EnrolList, Common.Error, PermissionManagement.getUserId(req), 
						lh.list(req, deviceAndCertListVO.getPage_per_data(), deviceAndCertListVO.getNow_page(), totalCnt, 
								lh.error(e).toString()));
			}
			else
			{
				lg.insertLog(Location.EnrolSearch, Common.Error, PermissionManagement.getUserId(req), 
						lh.certSearch(req, deviceAndCertListVO.getPage_per_data(), deviceAndCertListVO.getNow_page(), totalCnt,
								deviceAndCertListVO.getIssue_date(), deviceAndCertListVO.getExpiry_date(), deviceAndCertListVO.getCreate_date_start(),
								deviceAndCertListVO.getModify_date_start(), deviceAndCertListVO.getDevice_sn(), "", 
								lh.error(e).toString()));
			}
		}
		
		return "certifications/enrolList";
	}

	@RequestMapping(value = {"/cert/make/list.do", "/cert/makeList.do"}, method = RequestMethod.GET)
	public String makeList(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				return "redirect:/login.do";
			}
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, Location.MakeList);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			return "redirect:/login.do";
		}
		
		//현재 페이지
		int certNowPage = 1;
		
		//현재 페이지에서 보여줄 최대 값
		int certMaxPage = 5;
		
		//현재 페이지의 보여줄 최소 값
		int certMinPage = 1;
		
		//현재 페이지에서 보여줄 레코드의 개수
		int certCntList = 10;
		
		int totalCnt = 0;
		
		try
		{
			model.addAttribute("certTypeList", certService.selectCertType());
			model.addAttribute("deviceIdTypeList", codeService.getDeviceIdTypeList());
			
			HttpSession ss = req.getSession();


			DeviceAndCertListVO vo = new DeviceAndCertListVO((certNowPage - 1) * certCntList, certCntList);
			Map<String, Object> cnt = certService.selectMakeCertCnt(vo);
			
			//보여줄 수 있는 최대 페이지의 수
			int maxPage = Integer.parseInt(cnt.get("totalPageCnt").toString());
			totalCnt = Integer.parseInt(cnt.get("totalCnt").toString());
			
			certMaxPage = maxPage > certMaxPage ? certMaxPage : maxPage;
			
			ss.setAttribute("now", certNowPage);
			ss.setAttribute("max", certMaxPage);
			ss.setAttribute("min", certMinPage);
			ss.setAttribute("row", certCntList);
			ss.removeAttribute("searchDeviceSN");
			ss.removeAttribute("searchIssueDate");
			ss.removeAttribute("searchExpireDate");
			ss.removeAttribute("searchCreateDate");
			ss.removeAttribute("searchModifyDate");
			ss.removeAttribute("searchCertType");
			ss.removeAttribute("searchIssueYn");
			
			Object list = certService.selectMakeCertList(vo);
			
			
			//인증서 목록
 			model.addAttribute("certList", list);
			
			//인증서의 총 개수
			model.addAttribute("certTotalCnt", totalCnt);
			
			//표시가 가능한 최대 페이지의 개수
			model.addAttribute("certTotalPageCnt", maxPage);
			
			//현재 페이지의 위치
			model.addAttribute("certNowPage", certNowPage);
			
			//보여줄 최대 페이지
			model.addAttribute("certMaxPage", certMaxPage);
			
			//보여줄 최소 페이지
			model.addAttribute("certMinPage", certMinPage);
			
			lg.insertLog(Location.MakeList, Common.Success, PermissionManagement.getUserId(req), 
					lh.list(req, certCntList, certNowPage, totalCnt, ""));
			
		}
		catch(Exception e)
		{
			//*
			lg.insertLog(Location.MakeList, Common.Fail, PermissionManagement.getUserId(req), 
					lh.list(req, certCntList, certNowPage, totalCnt, lh.error(e).toString())
					);
			//*/
		}
		
		return "certifications/makeList";
	}
	
	@RequestMapping(value = {"/cert/makeList.do", "/cert/make/search.do", "/cert/make/list.do"}, method = RequestMethod.POST)
	public String makeList2(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				return "redirect:/login.do";
			}
			
			Location loc = null;
			String requestUri = req.getRequestURI();
			if(requestUri.indexOf("makeList") != -1 || requestUri.indexOf("make/list") != -1)
			{
				loc = Location.MakeList;
			}
			else
			{
				loc = Location.MakeSearch;
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
			return "redirect:/login.do";
		}
		
		int certNowPage = 0;
		int certMaxPage = 0;
		int certMinPage = 0;
		int certCntList = 0;
		int totalCnt = 0;
		String issueDate = "";
		String expireDate = "";
		String createDate = "";
		String modifyDate = "";
		String deviceSN = "";
		String certType = "";
		String deviceIdType = "";			
		String[] arr = null;
		String issueYN = "";
		
		try
		{
			model.addAttribute("certTypeList", certService.selectCertType());
			model.addAttribute("deviceIdTypeList", codeService.getDeviceIdTypeList());
		
			final String splitDate = " - ";
			issueDate = req.getParameter("issueDate");
			expireDate = req.getParameter("expireDate");
			createDate = req.getParameter("createDate");
			modifyDate = req.getParameter("modifyDate");
			deviceSN = req.getParameter("deviceSN");
			deviceIdType = req.getParameter("deviceIdType");
			certType = req.getParameter("certType");
			issueYN = req.getParameter("issueYN");
			
			//현재 페이지
			certNowPage = Integer.parseInt(req.getParameter("now"));
			
			//현재 페이지에서 보여줄 최대 값
			certMaxPage = Integer.parseInt(req.getParameter("max"));
			
			//현재 페이지의 보여줄 최소 값
			certMinPage = Integer.parseInt(req.getParameter("min"));
			
			//현재 페이지에서 보여줄 레코드의 개수
			certCntList = Integer.parseInt(req.getParameter("row"));
			
			HttpSession ss = req.getSession();
			DeviceAndCertListVO vo = new DeviceAndCertListVO((certNowPage - 1) * certCntList, certCntList);

			if(issueYN != null)
			{
				if(issueYN.equals("Y") || issueYN.equals("N"))
				{
					
				}
				else
				{
					issueYN = "";
				}
				
				ss.setAttribute("searchIssueYn", issueYN);
				vo.setIssueYn(issueYN);
			}
			else
			{
				ss.setAttribute("searchIssueYn", "");
				vo.setIssueYn(issueYN);
			}
			
			if(isCheck(deviceSN))
			{
				ss.setAttribute("searchDeviceSN", deviceSN);
				vo.setDevice_sn(deviceSN);
			}
			else
			{
				ss.removeAttribute("searchDeviceSN");
			}
			if(isCheck(issueDate))
			{
				arr = issueDate.split(splitDate);
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchIssueDate", issueDate);
					vo.setIssue_date_start(arr[0]);
					vo.setIssue_date_end(arr[1]);
				}
			}
			if(isCheck(expireDate))
			{
				arr = expireDate.split(splitDate);
				
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchExpireDate", expireDate);
					vo.setExpire_date_start(arr[0]);
					vo.setExpire_date_end(arr[1]);	
				}
			}
			if(isCheck(createDate))
			{
				arr = createDate.split(splitDate);
				
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchCreateDate", createDate);
					vo.setCreate_date_start(arr[0]);
					vo.setCreate_date_end(arr[1]);
				}
			}
			if(isCheck(modifyDate))
			{
				arr = modifyDate.split(splitDate);
				
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchModifyDate", modifyDate);
					vo.setModify_date_start(arr[0]);
					vo.setModify_date_end(arr[1]);
				}
			}
			if(isCheck(certType))
			{
				arr = certType.split("=");
				
				ss.setAttribute("searchCertType", certType);
				vo.setCert_type_id(arr[0]);
				vo.setCert_type_group(arr[1]);
				
			}
			else
			{
				ss.removeAttribute("searchCertType");
			}
			if(isCheck(deviceIdType))
			{
				arr = deviceIdType.split("=");
				ss.setAttribute("searchDeviceIdType", deviceIdType);
				
				vo.setDevice_sn_type_id(arr[0]);
				vo.setDevice_sn_type_group(arr[1]);
			}
			
			Map<String, Object> cnt = certService.selectMakeCertCnt(vo);
			int maxPage = Integer.parseInt(cnt.get("totalPageCnt").toString());
			totalCnt = Integer.parseInt(cnt.get("totalCnt").toString());
			certMaxPage = maxPage > certMaxPage ? certMaxPage : maxPage;
			
			//*
			ss.setAttribute("now", certNowPage);
			ss.setAttribute("max", certMaxPage);
			ss.setAttribute("min", certMinPage);
			ss.setAttribute("row", certCntList);
			//*/
			
			
 			model.addAttribute("certList", certService.selectMakeCertList(vo));
			
			//인증서의 총 개수
			model.addAttribute("certTotalCnt", totalCnt);
			
			//페이지의 최대 개수
			model.addAttribute("certTotalPageCnt", maxPage);
			
			//현재 페이지의 위치
			model.addAttribute("certNowPage", certNowPage);
			
			//보여줄 페이지의 최대 개수
			model.addAttribute("certMaxPage", certMaxPage);
			
			//보여줄 페이지의 최소 개수
			model.addAttribute("certMinPage", certMinPage);
			
			lg.insertLog(Location.MakeSearch, Common.Success, PermissionManagement.getUserId(req), 
					lh.certSearch(req, certCntList, certNowPage, totalCnt, issueDate, expireDate, createDate, modifyDate, deviceSN, certType, ""));
		}
		catch(Exception e)
		{
			lg.insertLog(Location.MakeSearch, Common.Fail, PermissionManagement.getUserId(req), 
					lh.certSearch(req, certCntList, certNowPage, totalCnt, 
							issueDate, expireDate, createDate, modifyDate, deviceSN, certType, lh.error(e).toString()));
		
		}
		
		return "certifications/makeList";
	}
	
	
	static public Integer[] JSONArrayToIntegerArray(JSONArray arr)
	{
		if(arr == null || arr.size() == 0)
			return null;
		
		Integer[] retval = new Integer[arr.size()];
		
		for(int i = 0; i<arr.size(); i++)
		{
			retval[i] = Integer.parseInt(arr.get(i).toString());
		}
		
		return retval;
	}
	static public int[] JSONArrayToIntArray(JSONArray arr)
	{
		if(arr == null || arr.size() == 0)
			return null;
		
		int[] retval = new int[arr.size()];
		
		for(int i = 0; i<arr.size(); i++)
		{
			retval[i] = Integer.parseInt(arr.get(i).toString());
		}
		
		return retval;
	}
	

	@RequestMapping(value = "/cert/make/cert.do", method = RequestMethod.POST)
	public String makeCert(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		Map<String, String[]> form = req.getParameterMap();		
		String[] arrDeviceId = form.get("arrDevice[]");
		
		if(arrDeviceId == null)
		{
			PermissionManagement.setSessionValue(req, "errMsg", "파라미터 오류");
///////////////////////////////////////POST 파리미터 오류
			return "redirect:/cert/make/list.do";
		}
		
		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				return "redirect:/login.do";
			}
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, Location.MakeCert);
			if(LogGenerator.Common.Success != perResult)
			{
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			//DB오류
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		int settingCnt = certSettingService.selectSettingSecCnt();
		if(settingCnt != 3)
		{
			PermissionManagement.setSessionValue(req, "errMsg", "보안 인증서를 먼저 세팅을 해야 합니다.");
			return "redirect:/cert/makeList.do";
		}
		
		//*
		String userId = (String) req.getSession().getAttribute("user_id");
		JSONObject retval = new JSONObject();
		JSONArray resultList = new JSONArray();
		JSONParser parser = new JSONParser();
		
		String certTypeId = "";
		String certTypeGroup = "";		
		String strDurationUint = "HOURS";
		int nDurationValue = 4320;
		int[] arrContryCode = new int[] {410};
		int[] arrCircularRegin = null;
		int[] arrPsids = new int[] {38, 135, 82049, 82053, 82054, 82055, 82056, 82057, 82059};
		int count = 0;
		String type = "";
		

		RestApi api = PermissionManagement.getApi(req);			
		for(int i = 0; i<arrDeviceId.length; i++)
		{
			
			JSONObject result = new JSONObject();
			JSONObject item = new JSONObject();
			String deviceCertType = "";		
			DeviceVO deviceVO = new DeviceVO();
			String deviceSN = "";
			String deviceID = "";		
			
			
			try
			{
				String[] arr = arrDeviceId[i].split("=");
				deviceSN = arr[1];
				deviceID = arr[0];
				deviceVO.setDevice_sn(deviceSN);
				deviceVO.setDevice_id(deviceID);
				
				
				//인증서 중복 검사
				if(certService.selectEnrolCheck(deviceVO) != 0)
				{
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", "이미 인증서가 생성이된 단말입니다.");
					resultList.add(item);
					
					continue;
				}
				
				//단말의 정보를 조회
				Map<String, Object> deviceType = certService.selectDeviceType(deviceVO);
				if(deviceType == null)
				{
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", "잘못된 단말의 정보입니다.");
					resultList.add(item);
					
					continue;	
				}
				
				if(deviceType.get("device_sec_type_group").equals("ECG028"))
				{
					switch(deviceType.get("device_sec_type").toString())
					{
					case "1":
						deviceCertType = "pse";
						break;
						
					case "2":
						deviceCertType = "ide";
						break;
						
					case "4":
						deviceCertType = "app";
						break;
						
					default:
						break;
					}
				}
				else
				{
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", "잚소된 단말의 정보입니다.");
					resultList.add(item);
					
					continue;	
				}
				
				//사용할 인증서의 설정을 불러오기
				switch(deviceCertType)
				{
				case "app":
					{
						String setting = certService.getAppSetting();
						JSONObject jSetting = (JSONObject)parser.parse(setting);
						JSONObject duration = (JSONObject)((JSONObject)jSetting.get("validity_period")).get("duration");
											
						arrContryCode = this.JSONArrayToIntArray((JSONArray)jSetting.get("country_code"));
						arrPsids = this.JSONArrayToIntArray((JSONArray)jSetting.get("psids"));
						arrCircularRegin = this.JSONArrayToIntArray((JSONArray)jSetting.get("circular_region"));
						type = "app";
						count = Integer.parseInt(jSetting.get("count").toString());
						strDurationUint = duration.get("unit").toString();
						nDurationValue = Integer.parseInt(duration.get("value").toString());
					}
					break;
					
				case "pse":
	
					{
						String setting = certService.getPseSetting();
						JSONObject jSetting = (JSONObject)parser.parse(setting);
						JSONObject duration = (JSONObject)((JSONObject)jSetting.get("validity_period")).get("duration");
											
						arrContryCode = this.JSONArrayToIntArray((JSONArray)jSetting.get("country_code"));
						arrPsids = this.JSONArrayToIntArray((JSONArray)jSetting.get("psids"));
						arrCircularRegin = this.JSONArrayToIntArray((JSONArray)jSetting.get("circular_region"));
						type = "pse";
						count = Integer.parseInt(jSetting.get("count").toString());
						strDurationUint = duration.get("unit").toString();
						nDurationValue = Integer.parseInt(duration.get("value").toString());
					}
					break;
					
				case "ide":
					{
						String setting = certService.getIdeSetting();
						JSONObject jSetting = (JSONObject)parser.parse(setting);
						JSONObject duration = (JSONObject)((JSONObject)jSetting.get("validity_period")).get("duration");
											
						arrContryCode = this.JSONArrayToIntArray((JSONArray)jSetting.get("country_code"));
						arrPsids = this.JSONArrayToIntArray((JSONArray)jSetting.get("psids"));
						arrCircularRegin = this.JSONArrayToIntArray((JSONArray)jSetting.get("circular_region"));
						type = "ide";
						count = Integer.parseInt(jSetting.get("count").toString());
						strDurationUint = duration.get("unit").toString();
						nDurationValue = Integer.parseInt(duration.get("value").toString());
					}
					break;
				}
				
				
				
				
				//조회된 단말이 없음 또는 인증서 설정이 잘못됨
				if(deviceCertType.equals(""))
				{
					switch(deviceCertType)
					{
					case "app":
						lg.insertLog(Location.MakeApp, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, "잘못된 단말의 정보입니다."));
						break;
						
					case "pse":
						lg.insertLog(Location.MakePse, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, "잘못된 단말의 정보입니다."));
						break;
						
					case "ide":
						lg.insertLog(Location.MakeIde, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, "잘못된 단말의 정보입니다."));
						break;
						
					default:
						lg.insertLog(Location.MakeCert, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, "잘못된 단말의 정보입니다."));
						break;
						
					}
					
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", "잚소된 단말의 정보입니다.");
					resultList.add(item);
					
					continue;	
				}
				

				//RSU, OBU에 따라 APP, IDE, PSE의 생성유무를 검사한다.
				boolean bCheckDeviceType = false;
				if(deviceCertType.equals("app"))
				{
					//RSU: APP 	rsu_2:CMM801
					if(deviceType.get("device_type").equals("rsu_2") && 
							deviceType.get("device_type_group").equals("CMM801"))
					{
						bCheckDeviceType = true;
					}
				}
				else if(deviceCertType.equals("pse"))
				{
					//OBU: PSE, IDE		obu_1:CMM801
					if(deviceType.get("device_type").equals("obu_1") &&
							deviceType.get("device_type_group").equals("CMM801"))
					{
						bCheckDeviceType = true;
					}
				}
				else if(deviceCertType.equals("ide"))
				{
					//OBU: PSE, IDE		obu_1:CMM801
					if(deviceType.get("device_type").equals("obu_1") &&
							deviceType.get("device_type_group").equals("CMM801"))
					{
						bCheckDeviceType = true;
					}
				}
				else
				{
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", "잚소된 단말의 정보입니다.");
					resultList.add(item);
					
					continue;
				}
				
				//RSU, OBU에 따른 보안인증서의 종류가 맞지 않음
				if(!bCheckDeviceType)
				{
					String deviceErrorMsg = "";
					switch(deviceCertType)
					{
					case "app":
						deviceErrorMsg = "단말 타입이 RSU가 아닙니다.";
						lg.insertLog(Location.MakeApp, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, deviceErrorMsg));
						break;
						
					case "pse":
						deviceErrorMsg = "단말 타입이 OBU가 아닙니다.";
						lg.insertLog(Location.MakePse, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, deviceErrorMsg));
						break;
						
					case "ide":
						deviceErrorMsg = "단말 타입이 OBU가 아닙니다.";
						lg.insertLog(Location.MakeIde, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, deviceErrorMsg));
						break;
					}
					
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", deviceErrorMsg);
					resultList.add(item);
					continue;
				}
				
				
				CertificatesKeyVO kVo = new CertificatesKeyVO();
				CertificationVO vo = new CertificationVO();
				String key = certService.selectId();
				
				switch(deviceCertType)
				{
				case "app":
					certTypeId = "4";
					certTypeGroup = "ECG028";
					result = api.createCertApp(key, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
					break;
					
				case "pse":
					certTypeId = "1";
					certTypeGroup = "ECG028";
					result = api.createCertPse(key, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
					break;
					
				case "ide":
					certTypeId = "2";
					certTypeGroup = "ECG028";
					result = api.createCertIde(key, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
					break;
				}
				
				if(result.get("success").equals("Y"))
				{
					DeviceVO dVo = new DeviceVO();
				
					dVo.setDevice_id(deviceID);
					dVo.setDevice_cert_type("M");
					
					vo.setCert_id(key);
					vo.setDevice_id(Long.parseLong(deviceID));
					vo.setCreate_id(userId);
					vo.setCert_state("2");
					vo.setCert_state_group_id("ECG005");
					vo.setExpiry_date(result.get("enrol_validity_end").toString());
					vo.setIssue_date(result.get("enrol_validity_start").toString());
					vo.setCert_type(certTypeId);
					vo.setCert_type_group(certTypeGroup);
					
					kVo.setDevice_id(Long.parseLong(deviceID));
					kVo.setCert_enrol_id(key);
					kVo.setCert_sec_id(key);
					kVo.setRequest_hash(result.get("request_hash").toString());
					kVo.setPublic_key(result.get("public_key").toString());				
				
					this.certService.insertCert(vo);
					this.certService.insertCertKey(kVo);
					this.certService.updateDeviceCertType(dVo);
					
					SecCertWaitVO scwVo = new SecCertWaitVO(key, result.get("download_time").toString(),
							result.get("download_url").toString(), result.get("wait_time").toString(),
							result.get("request_date").toString());
					
					this.secCertWaitService.insert(scwVo);
					
					
					
					item.put("deviceSN", deviceSN);
					item.put("success", "Y");
					item.put("msg", "");
					
					switch(deviceCertType)
					{
					case "app":
						lg.insertLog(Location.MakeApp, Common.Success, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
						break;
						
					case "pse":
						lg.insertLog(Location.MakePse, Common.Success, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
						break;
						
					case "ide":
						lg.insertLog(Location.MakeIde, Common.Success, PermissionManagement.getUserId(req), 
								lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
						break;
					}
				}
				else
				{
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", result.get("msg").toString());
				}
			}
			catch(Exception e)
			{
				item.put("deviceSN", deviceSN);
				item.put("success", "N");
				item.put("msg", lh.error(e).toString());
				
				switch(deviceCertType)
				{
				case "app":
					lg.insertLog(Location.MakeApp, Common.Error, PermissionManagement.getUserId(req), 
							lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
							);
					break;
					
				case "pse":
					lg.insertLog(Location.MakePse, Common.Error, PermissionManagement.getUserId(req), 
							lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
						);
					break;
					
				case "ide":
					lg.insertLog(Location.MakeIde, Common.Error, PermissionManagement.getUserId(req), 
							lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
						);
					break;
					
				default:
					lg.insertLog(Location.MakeCert, Common.Fail, PermissionManagement.getUserId(req), 
							lh.makeCert(req, deviceCertType, deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
							);
					break;
				}
			}
			
			resultList.add(item);
		}

		String errMsg = "";
		String succMsg = "";
		for(int i = 0; i<resultList.size(); i++)
		{
			JSONObject item = (JSONObject)resultList.get(i);			
			if(item.get("success").equals("Y"))
			{
				succMsg += item.get("deviceSN") + ", ";
			}
			else
			{
				errMsg += item.get("deviceSN") + ", ";
			}
		}
		
		if(errMsg.equals(""))
		{
			errMsg = null;
		}
		else
		{
			errMsg = "보안 인증서를 생성하지 못한 단말: " + errMsg;	
		}
		
		
		if(succMsg.equals(""))
		{	
			succMsg = null;
		}
		else
		{
			succMsg = "보안 인증서가 생성된 단말: " + succMsg;
		}
		
		PermissionManagement.setSessionValue(req, "errMsg", errMsg);
		PermissionManagement.setSessionValue(req, "succMsg", succMsg);
		
		//retval.put("List", resultList);
		//req.getSession().setAttribute("result", retval.toString());
		model.addAttribute("result", retval);
		//*/
		
		return "redirect:/cert/makeList.do";
	}
	
	@ResponseBody
	@RequestMapping(value = {"/cert/enrol/delete.do", "/cert/make/delete.do"}, produces="application/text;charset=utf8;")
	public String certDelete(@RequestParam("arrCert") String certs, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{		
		Location loc = null;
		String uri = req.getRequestURI();
		JSONObject json = new JSONObject(); 
		
		if(uri.indexOf("/enrol/delete") != -1)
		{
			loc = Location.EnrolDelete;
		}
		else
		{
			loc = Location.MakeDelete;
		}
		
		if(certs == null)
		{
			json.put("success", "N");
			json.put("msg", "폐기할 인증서 정보가 잘못 되었습니다.");
			/*
			PermissionManagement.setSessionValue(req, "errMsg", "파라미터 오류");
			
			if(loc == Location.MakeDelete)
			{
				return "redirect:/cert/make/list.do";
			}
			else
			{
				return "redirect:/cert/enrol/list.do";	
			}
			*/
		}

		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				//return "redirect:/login.do";
			}
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				//PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				//return PermissionManagement.getRedirectUrl(perResult);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			//return "redirect:/login.do";
		}
		
		//*
		String userId = (String) req.getSession().getAttribute("user_id");
		JSONObject retval = new JSONObject();
		JSONArray resultList = new JSONArray();
		String errMsg = "";
		String succMsg = "";
		
		JSONParser parser = new JSONParser();

		if(req.getSession().getAttribute("api") == null) {
			json.put("success", "N");
			json.put("session", "N");
			json.put("msg", "세션이 만료되었습니다.");
		}else {
			try
			{
				//certs = org.springframework.web.util.HtmlUtils.htmlUnescape(certs);
				String[] arrCert = certs.split(",");
				
				RestApi api = PermissionManagement.getApi(req);
				for(int i = 0; i<arrCert.length; i++)
				{
					String[] arrTemp = arrCert[i].toString().split(":");
					String certId = arrTemp[0];
					DeviceAndCertListVO daclVo = certService.selectKey(certId);
					
					//*
					JSONObject result = api.certBlacklist(daclVo.getPublic_key());
					if(result.get("success").equals("Y"))
					{
						DeviceVO dVO = new DeviceVO();
						
						dVO.setDevice_cert_type(null);
						dVO.setDevice_id(daclVo.getDevice_id().toString());
						
						certService.deleteCertification(daclVo.getCert_key_id());
						certService.updateDeviceCertType(dVO);
						certService.deleteCertIssue(certId);
						
						lg.insertLog(loc, Common.Success, userId, lh.deleteEnrol(req, daclVo.getCert_key_id(), certId, daclVo.getPublic_key(), daclVo.getDevice_id(), daclVo.getDevice_sn(), ""));
						succMsg += daclVo.getDevice_sn() + ", ";
						
					}
					else
					{
						Long log = lg.insertLog(loc, Common.Fail, userId, 
								lh.deleteEnrol(req, daclVo.getCert_key_id(), certId, daclVo.getPublic_key(), daclVo.getDevice_id(), daclVo.getDevice_sn(), result.get("msg").toString()));
						errMsg += daclVo.getDevice_sn() + "(" + log + "), ";
					}
					//*/
				}
				
				succMsg += " 등록 인증서 삭제";
				
				json.put("success", "Y");
				json.put("msg", "폐기 되었습니다.");
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Long log = lg.insertLog(loc, Common.Error, PermissionManagement.getUserId(req), 
						lh.error(req, e.getMessage()));
				
				errMsg = "오류가 발생했습니다. 자세한 사항은 " + log + "번을 참조해주세요";
			}
			
			if(errMsg.equals(""))
				errMsg = null;
			else
				//PermissionManagement.setSessionValue(req, "errMsg", errMsg);
				json.put("success", "N");
				//json.put("msg", errMsg);
			
			if(succMsg.equals(""))
				succMsg = null;
			else
				//PermissionManagement.setSessionValue(req, "succMsg", succMsg);
				json.put("success", "Y");
				//json.put("msg", succMsg);
			
			/*
			if(loc == Location.MakeDelete)
			{
				return "redirect:/cert/make/list.do";
			}
			else
			{
				return "redirect:/cert/enrol/list.do";	
			}
			*/
		}
		return json.toString();
	}
	
	@RequestMapping(value = {"/cert/enrol/download.do", "/cert/make/download/enrol.do"}, method = RequestMethod.POST)
	public void enrolDownload(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		OutputStream os = null;
		String certID = req.getParameter("cert");
		String deviceID = req.getParameter("device");
		Long nDeviceID = null;
		
		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>window.opener.location.reload();window.close()</script>").getBytes());
				os.flush();
				os.close();
			}
			
			String requestUri = req.getRequestURI();
			Location loc = null;
			if(requestUri.indexOf("enroldownload") != -1)
			{
				loc = Location.EnrolDownload;
			}
			else
			{
				loc = Location.MakeEnrolDownload;
			}
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				
				resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>alert('" + PermissionManagement.getMsg(perResult) + "')</script>").getBytes());
				os.flush();
				os.close();
				return;
			}
		}
		catch(Exception e)
		{
			//return "redirect:/login.do";
			resp.setContentType("text/html;charset=UTF-8");
			os = resp.getOutputStream();
			os.write(("<script>alert('" + "세션이 만료 되었습니다." + "');window.close();</script>").getBytes());
			os.flush();
			os.close();
			
			e.printStackTrace();
			
			return;
		}
		
		
		try
		{
			
			byte[] fileData = null;
			String fileName = null;
			CertificationVO vo = new CertificationVO();
			nDeviceID = Long.parseLong(deviceID);
			
			vo.setDevice_id(nDeviceID);
			vo.setCert_id(certID);
			Map<String, Object> item = certService.selectCert(vo);
			Object publicKey = item.get("public_key");
			os = resp.getOutputStream();
			
			if(publicKey != null)
			{
				RestApi api = PermissionManagement.getApi(req);
				
				JSONObject result = api.downloadEnrol(publicKey.toString());
				
				if(result.get("success").equals("Y"))
				{
					fileName = result.get("fileName").toString();
					fileData = (byte[])result.get("fileData");
					
					resp.setContentType("application/cert");
					resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
					resp.setHeader("Content-Transfer-Encoding", "binary");
					
					os.write(fileData);
					
					if(req.getRequestURI().indexOf("cert/enrol/download") != -1)
					{
						lg.insertLog(Location.EnrolDownload, Common.Success, 
								PermissionManagement.getUserId(req), lh.enrolDownload(req, publicKey.toString(), nDeviceID, certID, "")
								);	
					}
					else
					{
						lg.insertLog(Location.MakeEnrolDownload, Common.Success, 
								PermissionManagement.getUserId(req), lh.enrolDownload(req, publicKey.toString(), nDeviceID, certID, ""));
					}
				}
				else
				{
					if(req.getRequestURI().indexOf("cert/enrol/download") != -1)
					{
						lg.insertLog(Location.EnrolDownload, Common.Fail,
								PermissionManagement.getUserId(req), lh.enrolDownload(req, publicKey.toString(), nDeviceID, certID, result.toString())
								);	
					}
					else
					{
						lg.insertLog(Location.MakeEnrolDownload, Common.Fail, 
								PermissionManagement.getUserId(req), lh.enrolDownload(req, publicKey.toString(), nDeviceID, certID, result.toString())
								);	
					}
				}
				
			}
			else
			{
				resp.setContentType("text/html;charset=UTF-8");
				os.write("<script>alert('등록인증서가 생성되지 않은 단말입니다.')</script>".getBytes());
				
				if(req.getRequestURI().indexOf("cert/enrol/download") != -1)
				{
					lg.insertLog(Location.EnrolDownload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.enrolDownload(req, "", nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다.")
							);
				}
				else
				{
					lg.insertLog(Location.MakeEnrolDownload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.enrolDownload(req, "", nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다.")
							);
				}
				
			}
			
			os.flush();
			os.close();
		}
		catch(Exception e)
		{
			if(os != null)
			{
				os.flush();
				os.close();
			}
			
			if(req.getRequestURI().indexOf("cert/enrol/download") != -1)
			{
				lg.insertLog(Location.EnrolDownload, Common.Error, PermissionManagement.getUserId(req), 
						lh.enrolDownload(req, "", nDeviceID, certID, lh.error(e).toString())
						);
			}
			else
			{
				lg.insertLog(Location.MakeEnrolDownload, Common.Error, PermissionManagement.getUserId(req), 
						lh.enrolDownload(req, "", nDeviceID, certID, lh.error(e).toString())
						);
			}
		}
	}

	@RequestMapping(value = {"/cert/enrol/bootstrap.do", "/cert/make/download/bootstrap.do"}, method = RequestMethod.POST)
	public void bootstrapDownload(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		OutputStream os = null;
		String certID = req.getParameter("cert");
		String deviceID = req.getParameter("device");
		Long nDeviceID = null;
		
		try
		{			
			Location loc = null;
			String requestUri = req.getRequestURI();
			if(requestUri.indexOf("enrol/bootstrap") != -1)
			{
				loc = Location.EnrolBoostrapDownload;
			}
			else
			{
				loc = Location.MakeBootstrapDownload;
			}
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				
				//resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>alert('" + PermissionManagement.getMsg(perResult) + "')</script>").getBytes());
				os.flush();
				os.close();
				
				return;
			}
		}
		catch(Exception e)
		{
			//resp.setContentType("text/html;charset=UTF-8");
			os = resp.getOutputStream();
			os.write(("<script>alert('" + "세션이 만료 되었습니다." + "');window.close();</script>").getBytes());
			os.flush();
			os.close();
			
			e.printStackTrace();
			return;
		}
		
		try
		{
			byte[] fileData = null;
			String fileName = null;
			CertificationVO vo = new CertificationVO();
			nDeviceID = Long.parseLong(deviceID);
					
			vo.setDevice_id(nDeviceID);
			vo.setCert_id(certID);
			Map<String, Object> item = certService.selectCert(vo);
			Object requestHash = item.get("request_hash");
	
			os = resp.getOutputStream();
			if(requestHash != null)
			{
				RestApi api = PermissionManagement.getApi(req);
				
				JSONObject result = api.downloadBootstrap(requestHash.toString());
				if(result.get("success").equals("Y"))
				{
					fileName = result.get("fileName").toString();
					fileData = (byte[])result.get("fileData");
					
					resp.setContentType("application/zip");
					resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
					resp.setHeader("Content-Transfer-Encoding", "binary");
					os.write(fileData);
					
					if(req.getRequestURI().indexOf("cert/enrol/bootstrap") != -1)
					{
						lg.insertLog(Location.EnrolBoostrapDownload, Common.Success, PermissionManagement.getUserId(req), 
								lh.bootstrapDownload(req, requestHash.toString(), nDeviceID, certID, ""));
					}
					else
					{
						lg.insertLog(Location.MakeBootstrapDownload, Common.Success, PermissionManagement.getUserId(req),
								lh.bootstrapDownload(req, requestHash.toString(), nDeviceID, certID, ""));
					}
				}
				else
				{
					resp.setContentType("text/html;charset=UTF-8");
					os = resp.getOutputStream();
					os.write(("<script>alert('" + result.get("msg") + "')</script>").getBytes());
					os.flush();
					os.close();
				}
				
			}
			else
			{
				resp.setContentType("text/html;charset=UTF-8");
				os.write("<script>alert('등록인증서가 생성되지 않은 단말입니다.');</script>".getBytes());
				
				if(req.getRequestURI().indexOf("cert/enrol/bootstrap") != -1)
				{
					lg.insertLog(Location.EnrolBoostrapDownload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.bootstrapDownload(req, "", nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다."));
				}
				else
				{
					lg.insertLog(Location.MakeBootstrapDownload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.bootstrapDownload(req, "", nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다."));
				}
			}
			
			os.flush();
			os.close();
			
			
		}
		catch(Exception e)
		{
			if(os != null)
			{
				os.flush();
				os.close();
			}
			
			if(req.getRequestURI().indexOf("cert/enrol/bootstrap") != -1)
			{
				lg.insertLog(Location.EnrolBoostrapDownload, Common.Error, PermissionManagement.getUserId(req), 
						lh.bootstrapDownload(req, "", nDeviceID, certID, lh.error(e).toString()));
			}
			else
			{
				lg.insertLog(Location.MakeBootstrapDownload, Common.Error, PermissionManagement.getUserId(req), 
						lh.bootstrapDownload(req, "", nDeviceID, certID, lh.error(e).toString()));
			}
		}
	}
	
	@RequestMapping(value = {
			"/cert/make/download/app.do", "/cert/make/download/ide.do", 
			"/cert/make/download/pse.do"}, method = RequestMethod.POST)
	public void makeDownload(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		OutputStream os = null;
		String certID = req.getParameter("cert");
		String deviceID = req.getParameter("device");
		Long nDeviceID = null;
		String publicKey = "";
		
		
		try
		{
			
			LogGenerator.Common perResult = LogGenerator.Common.Error;
			
			if(req.getRequestURI().indexOf("app.do") != -1)
			{
				perResult = pm.isPermission(req, lg, lh, Location.MakeAppDownload);
			}
			else if(req.getRequestURI().indexOf("ide.do") != -1)
			{
				perResult = pm.isPermission(req, lg, lh, Location.MakeIdeDownload);
			}
			else if(req.getRequestURI().indexOf("pse.do") != -1)
			{
				perResult = pm.isPermission(req, lg, lh, Location.MakePseDownload);
			}
			
			if(LogGenerator.Common.Success != perResult)
			{
				
				resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>alert('" + PermissionManagement.getMsg(perResult) + "')</script>").getBytes());
				os.flush();
				os.close();
				
				return;
			}
		}
		catch(Exception e)
		{
			//return "redirect:/login.do";
			resp.setContentType("text/html;charset=UTF-8");
			os = resp.getOutputStream();
			os.write(("<script>alert('" + "세션이 만료 되었습니다." + "');window.close();</script>").getBytes());
			os.flush();
			os.close();
			
			e.printStackTrace();
			return;
		}
		
		try
		{
			//*

			byte[] fileData = null;
			String fileName = null;
			CertificationVO vo = new CertificationVO();
			nDeviceID = Long.parseLong(deviceID);
			
			vo.setDevice_id(nDeviceID);
			vo.setCert_id(certID);
			Map<String, Object> item = certService.selectCert(vo);
			Object key = item.get("public_key");
			
			
			os = resp.getOutputStream();
			if(key != null)
			{
				RestApi api = PermissionManagement.getApi(req);
				
				try
				{
					DateHelper dateHelper = new DateHelper();
					HashMap<String, Object> wait = this.secCertWaitService.select(certID);
					String strDlTimeGmt = wait.get("download_time_gmt").toString();
					Date now = new Date();
					Date dateDlTimeGmt = dateHelper.getDate(strDlTimeGmt, null);
					
					long nNow = now.getTime();
					long nDlTimeGmt = dateDlTimeGmt.getTime();
					
					logger.info("nNow => " + nNow);
					logger.info("nDlTimeGmt => " + nDlTimeGmt);	
					logger.info("nNow < nDlTimeGmt => " + (nNow < nDlTimeGmt));	
					
					if(nNow < nDlTimeGmt)
					{
						//다운로드가 불가능
						long result = nDlTimeGmt - nNow;
						long min = result / 1000 / 60 - ((result/ 1000 / 60 / 60) * 60);
						long sec = (result / 1000) - ((result / 1000 / 60) * 60);
						long hour = (result/ 1000 / 60 / 60);
						
						String waitTime = "" + hour + ":" + min + ":" + sec;
						
						resp.setContentType("text/html;charset=UTF-8");
						os.write(("<script>alert('인증서를 발급을 진행하고 있습니다. 잠시만 기다려주세요. 남은 대기 시간은 " + waitTime + "입니다.');window.close();</script>").getBytes());
					
						if(req.getRequestURI().indexOf("app.do") != -1)
						{
							lg.insertLog(Location.MakeAppDownload, Common.Fail, PermissionManagement.getUserId(req),  
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "대기시간: " + waitTime)
									);
						}
						else if(req.getRequestURI().indexOf("ide.do") != -1)
						{
							lg.insertLog(Location.MakeIdeDownload, Common.Fail, PermissionManagement.getUserId(req),  
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "대기시간: " + waitTime)
									);
						}
						else if(req.getRequestURI().indexOf("pse.do") != -1)
						{
							lg.insertLog(Location.MakePseDownload, Common.Fail, PermissionManagement.getUserId(req),  
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "대기시간: " + waitTime)
									);
						}
						
					}
					else
					{
						JSONObject resultCertList = api.certList(key.toString());
						logger.info(resultCertList);
						if(resultCertList.get("success").equals("Y"))
						{
							//다운로드 가능
							JSONObject result = api.downloadCertBundle(key.toString());
							if(result.get("success").equals("Y"))
							{
								fileName = result.get("fileName").toString();
								fileData = (byte[])result.get("fileData");
								
								resp.setContentType("application/zip");
								resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
								resp.setHeader("Content-Transfer-Encoding", "binary");
								os.write(fileData);
								
								if(req.getRequestURI().indexOf("app.do") != -1)
								{
									lg.insertLog(Location.MakeAppDownload, Common.Success, PermissionManagement.getUserId(req), 
											lh.makeDownload(req, key.toString(), nDeviceID, certID, "")
											);
								}
								else if(req.getRequestURI().indexOf("ide.do") != -1)
								{
									lg.insertLog(Location.MakeIdeDownload, Common.Success, PermissionManagement.getUserId(req), 
											lh.makeDownload(req, key.toString(), nDeviceID, certID, "")
											);
								}
								else if(req.getRequestURI().indexOf("pse.do") != -1)
								{
									lg.insertLog(Location.MakePseDownload, Common.Success, PermissionManagement.getUserId(req), 
											lh.makeDownload(req, key.toString(), nDeviceID, certID, "")
											);
								}
							}
							else
							{
								Long logSeq = (long)0;
								if(req.getRequestURI().indexOf("app.do") != -1)
								{
									logSeq = lg.insertLog(Location.MakeAppDownload, Common.Error, PermissionManagement.getUserId(req),
											lh.makeDownload(req, publicKey, nDeviceID, certID, result.get("msg").toString())
											);
								}
								else if(req.getRequestURI().indexOf("ide.do") != -1)
								{
									logSeq = lg.insertLog(Location.MakeIdeDownload, Common.Error, PermissionManagement.getUserId(req),
											lh.makeDownload(req, publicKey, nDeviceID, certID, result.get("msg").toString())
											);
								}
								else if(req.getRequestURI().indexOf("pse.do") != -1)
								{
									logSeq = lg.insertLog(Location.MakePseDownload, Common.Error, PermissionManagement.getUserId(req),
											lh.makeDownload(req, publicKey, nDeviceID, certID, result.get("msg").toString())
											);
								}
								
								resp.setContentType("text/html;charset=UTF-8");
								os.write(("<script>alert('" + "오류가 발생했습니다. " + logSeq + "를 참고해주세요.');window.close();</script>").getBytes());
								os.flush();
							}
						}
						else
						{
							Long logSeq = (long)0;
							if(req.getRequestURI().indexOf("app.do") != -1)
							{
								logSeq = lg.insertLog(Location.MakeAppDownload, Common.Error, PermissionManagement.getUserId(req),
										lh.makeDownload(req, publicKey, nDeviceID, certID, resultCertList.get("msg").toString())
										);
							}
							else if(req.getRequestURI().indexOf("ide.do") != -1)
							{
								logSeq = lg.insertLog(Location.MakeIdeDownload, Common.Error, PermissionManagement.getUserId(req),
										lh.makeDownload(req, publicKey, nDeviceID, certID, resultCertList.get("msg").toString())
										);
							}
							else if(req.getRequestURI().indexOf("pse.do") != -1)
							{
								logSeq = lg.insertLog(Location.MakePseDownload, Common.Error, PermissionManagement.getUserId(req),
										lh.makeDownload(req, publicKey, nDeviceID, certID, resultCertList.get("msg").toString())
										);
							}
							
							resp.setContentType("text/html;charset=UTF-8");
							os.write(("<script>alert('" + "오류가 발생했습니다. " + logSeq + "를 참고해주세요.');window.close();</script>").getBytes());
							os.flush();
						}
						
					}
					

					os.flush();
					os.close();
					
				}
				catch(Exception e)
				{
					if(os != null)
					{
						os.flush();
						os.close();				
					}
					
					if(req.getRequestURI().indexOf("app.do") != -1)
					{
						lg.insertLog(Location.MakeAppDownload, Common.Error, PermissionManagement.getUserId(req),
								lh.makeDownload(req, publicKey, nDeviceID, certID, lh.error(e).toString())
								);
					}
					else if(req.getRequestURI().indexOf("ide.do") != -1)
					{
						lg.insertLog(Location.MakeIdeDownload, Common.Error, PermissionManagement.getUserId(req),
								lh.makeDownload(req, publicKey, nDeviceID, certID, lh.error(e).toString())
								);
					}
					else if(req.getRequestURI().indexOf("pse.do") != -1)
					{
						lg.insertLog(Location.MakePseDownload, Common.Error, PermissionManagement.getUserId(req),
								lh.makeDownload(req, publicKey, nDeviceID, certID, lh.error(e).toString())
								);
					}
				}
				
				
				/*
				JSONObject certList = api.certList(key.toString());
				if(certList.get("success").equals("Y"))
				{
					Object waitTime = certList.get("wait_time");
					
					if(waitTime == null)
					{
						JSONObject result = api.downloadCertBundle(key.toString());
						fileName = result.get("fileName").toString();
						fileData = (byte[])result.get("fileData");
						
						resp.setContentType("application/zip");
						resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
						resp.setHeader("Content-Transfer-Encoding", "binary");
						os.write(fileData);
						
						if(req.getRequestURI().indexOf("app.do") != -1)
						{
							lg.insertLog(Location.MakeAppDownload, Common.Success, PermissionManagement.getUserId(req), 
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "")
									);
						}
						else if(req.getRequestURI().indexOf("ide.do") != -1)
						{
							lg.insertLog(Location.MakeIdeDownload, Common.Success, PermissionManagement.getUserId(req), 
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "")
									);
						}
						else if(req.getRequestURI().indexOf("pse.do") != -1)
						{
							lg.insertLog(Location.MakePseDownload, Common.Success, PermissionManagement.getUserId(req), 
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "")
									);
						}
					}
					else
					{
						resp.setContentType("text/html;charset=UTF-8");
						os.write(("<script>alert('인증서를 발급을 진행하고 있습니다. 잠시만 기다려주세요. 남은 대기 시간은 " + waitTime + "입니다.');window.close();</script>").getBytes());
					
						if(req.getRequestURI().indexOf("app.do") != -1)
						{
							lg.insertLog(Location.MakeAppDownload, Common.Fail, PermissionManagement.getUserId(req),  
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "대기시간: " + waitTime)
									);
						}
						else if(req.getRequestURI().indexOf("ide.do") != -1)
						{
							lg.insertLog(Location.MakeIdeDownload, Common.Fail, PermissionManagement.getUserId(req),  
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "대기시간: " + waitTime)
									);
						}
						else if(req.getRequestURI().indexOf("pse.do") != -1)
						{
							lg.insertLog(Location.MakePseDownload, Common.Fail, PermissionManagement.getUserId(req),  
									lh.makeDownload(req, key.toString(), nDeviceID, certID, "대기시간: " + waitTime)
									);
						}
					}
				}
				else
				{
					resp.setContentType("text/html;charset=UTF-8");
					os.write(("<script>alert('" + certList.get("msg") + "');window.close();</script>").getBytes());
					
					if(req.getRequestURI().indexOf("app.do") != -1)
					{
						lg.insertLog(Location.MakeAppDownload, Common.Fail, PermissionManagement.getUserId(req),  
								lh.makeDownload(req, key.toString(), nDeviceID, certID, certList.get("msg").toString())
								);
					}
					else if(req.getRequestURI().indexOf("ide.do") != -1)
					{
						lg.insertLog(Location.MakeIdeDownload, Common.Fail, PermissionManagement.getUserId(req),  
								lh.makeDownload(req, key.toString(), nDeviceID, certID, certList.get("msg").toString())
								);
					}
					else if(req.getRequestURI().indexOf("pse.do") != -1)
					{
						lg.insertLog(Location.MakePseDownload, Common.Fail, PermissionManagement.getUserId(req),  
								lh.makeDownload(req, key.toString(), nDeviceID, certID, certList.get("msg").toString())
								);
					}
				}
				//*/
			}
			else
			{
				resp.setContentType("text/html;charset=UTF-8");
				os.write("<script>alert('등록인증서가 생성되지 않은 단말입니다.');window.close();</script>".getBytes());
				
				if(req.getRequestURI().indexOf("app.do") != -1)
				{
					lg.insertLog(Location.MakeAppDownload, Common.Fail, PermissionManagement.getUserId(req),  
							lh.makeDownload(req, key.toString(), nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다.")
							);
				}
				else if(req.getRequestURI().indexOf("ide.do") != -1)
				{
					lg.insertLog(Location.MakeIdeDownload, Common.Fail, PermissionManagement.getUserId(req),  
							lh.makeDownload(req, key.toString(), nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다.")
							);
				}
				else if(req.getRequestURI().indexOf("pse.do") != -1)
				{
					lg.insertLog(Location.MakePseDownload, Common.Fail, PermissionManagement.getUserId(req),  
							lh.makeDownload(req, key.toString(), nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다.")
							);
				}
			}
			
			os.flush();
			os.close();
		}
		catch(Exception e)
		{
			if(os != null)
			{
				os.flush();
				os.close();				
			}
			
			if(req.getRequestURI().indexOf("app.do") != -1)
			{
				lg.insertLog(Location.MakeAppDownload, Common.Error, PermissionManagement.getUserId(req),
						lh.makeDownload(req, publicKey, nDeviceID, certID, lh.error(e).toString())
						);
			}
			else if(req.getRequestURI().indexOf("ide.do") != -1)
			{
				lg.insertLog(Location.MakeIdeDownload, Common.Error, PermissionManagement.getUserId(req),
						lh.makeDownload(req, publicKey, nDeviceID, certID, lh.error(e).toString())
						);
			}
			else if(req.getRequestURI().indexOf("pse.do") != -1)
			{
				lg.insertLog(Location.MakePseDownload, Common.Error, PermissionManagement.getUserId(req),
						lh.makeDownload(req, publicKey, nDeviceID, certID, lh.error(e).toString())
						);
			}
		}
		//*/
	}
	
	
	@RequestMapping(value = "/cert/enrol/private.do", method = RequestMethod.POST)
	public void privateKeyDownload(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		OutputStream os = null;
		String certID = req.getParameter("cert");
		String deviceID = req.getParameter("device");
		Long nDeviceID = null;
		
		try
		{			
			Location loc = null;
			String requestUri = req.getRequestURI();
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				
				resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>alert('" + PermissionManagement.getMsg(perResult) + "')</script>").getBytes());
				os.flush();
				os.close();
				
				return;
			}
		}
		catch(Exception e)
		{
			//return "redirect:/login.do";
			resp.setContentType("text/html;charset=UTF-8");
			os = resp.getOutputStream();
			os.write(("<script>alert('" + "세션이 만료 되었습니다." + "');window.close();</script>").getBytes());
			os.flush();
			os.close();
			
			e.printStackTrace();
			return;
		}
		
		try
		{
			byte[] fileData = null;
			String fileName = null;
			CertificationVO vo = new CertificationVO();
			nDeviceID = Long.parseLong(deviceID);
					
			vo.setDevice_id(nDeviceID);
			vo.setCert_id(certID);
			Map<String, Object> item = certService.selectCert(vo);
			Object requestHash = item.get("request_hash");
	
			os = resp.getOutputStream();
			if(requestHash != null)
			{
				RestApi api = PermissionManagement.getApi(req);
				
				JSONObject result = api.privateKeyDownload(certID);
				if(result.get("success").equals("Y"))
				{
					fileName = result.get("fileName").toString();
					fileData = (byte[])result.get("fileData");
					
					resp.setContentType("application/zip");
					resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
					resp.setHeader("Content-Transfer-Encoding", "binary");
					os.write(fileData);
					
					if(req.getRequestURI().indexOf("cert/enrol/bootstrap") != -1)
					{
						lg.insertLog(Location.EnrolBoostrapDownload, Common.Success, PermissionManagement.getUserId(req), 
								lh.bootstrapDownload(req, requestHash.toString(), nDeviceID, certID, ""));
					}
					else
					{
						lg.insertLog(Location.MakeBootstrapDownload, Common.Success, PermissionManagement.getUserId(req),
								lh.bootstrapDownload(req, requestHash.toString(), nDeviceID, certID, ""));
					}
				}
				else
				{
					resp.setContentType("text/html;charset=UTF-8");
					os = resp.getOutputStream();
					os.write(("<script>alert('" + result.get("msg") + "');window.close();</script>").getBytes());
					os.flush();
					os.close();
				}
				
			}
			else
			{
				resp.setContentType("text/html;charset=UTF-8");
				os.write("<script>alert('등록인증서가 생성되지 않은 단말입니다.');</script>".getBytes());
				
				if(req.getRequestURI().indexOf("cert/enrol/bootstrap") != -1)
				{
					lg.insertLog(Location.EnrolBoostrapDownload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.bootstrapDownload(req, "", nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다."));
				}
				else
				{
					lg.insertLog(Location.MakeBootstrapDownload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.bootstrapDownload(req, "", nDeviceID, certID, "등록인증서가 생성되지 않은 단말입니다."));
				}
			}
			
			os.flush();
			os.close();
			
			
		}
		catch(Exception e)
		{
			if(os != null)
			{
				os.flush();
				os.close();
			}
			
			if(req.getRequestURI().indexOf("cert/enrol/bootstrap") != -1)
			{
				lg.insertLog(Location.EnrolBoostrapDownload, Common.Error, PermissionManagement.getUserId(req), 
						lh.bootstrapDownload(req, "", nDeviceID, certID, lh.error(e).toString()));
			}
			else
			{
				lg.insertLog(Location.MakeBootstrapDownload, Common.Error, PermissionManagement.getUserId(req), 
						lh.bootstrapDownload(req, "", nDeviceID, certID, lh.error(e).toString()));
			}
		}
	}
	
	// 등록인증서 생성
	@ResponseBody
	@RequestMapping(value = "/cert/enrol/create.do", method = RequestMethod.POST)
	public String checkDeviceSn(@RequestBody String json, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		if(pm == null) {
			req.getSession().invalidate();
			
			JSONObject result = new JSONObject();
			
			result.put("success", "N");
			result.put("redirect", req.getContextPath() + "/logout.do");
			result.put("msg", "서버가 재시작 됨");
			return result.toString();
		}
		
		if(certSettingService.selectSettingSecCnt() != 3) {
			JSONObject result = new JSONObject();
			result.put("success", "N");
			result.put("redirect", req.getContextPath() + "/cert/enrol/list.do");
			result.put("msg", "보안인증서 설정을 확인해주세요");
			return result.toString();
		}
		
		RestApi api = PermissionManagement.getApi(req);
		ArrayList<HashMap<String, Object>> arrEnrol = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> enrolItem = null;
		RestApiParameter param = new RestApiParameter();
		
		try {
			JSONObject retval = new JSONObject();
			JSONParser parser = new JSONParser();

			json = json.replaceAll("=", "");
			json = URLDecoder.decode(json, "utf-8");

			JSONObject result = (JSONObject)parser.parse(json);
			JSONArray arrDevice = (JSONArray)result.get("device_id");

			
			for(int i = 0; i<arrDevice.size(); i++) {
				Boolean bCheck = false;
	        	String[] splitDeviceId = arrDevice.get(i).toString().split(":");
	        	String certId = splitDeviceId[0];
	        	String deviceId = splitDeviceId[1];
	        	String deviceSN = splitDeviceId[2];
	        	
	        	DeviceVO dVO = new DeviceVO();
	        	dVO.setDevice_id(deviceId);
	        	
	        	//단말의 등록인증서 검사
	        	if(certService.selectEnrolCheck(dVO) == 0) {
		        	String key = certService.selectId();
		        	enrolItem = new HashMap<>();
		        	Map<String, Object> deviceType = certService.selectDeviceType(dVO);
		        	
		        	enrolItem.put("code_group_id", deviceType.get("device_sec_type_group").toString());
		        	enrolItem.put("code_id", deviceType.get("device_sec_type").toString());
		        	enrolItem.put("check", new Boolean(true));
		        	enrolItem.put("success", "N");
		        	enrolItem.put("title", key);
		        	enrolItem.put("device_id", Long.parseLong(deviceId));
		        	enrolItem.put("device_sn", deviceSN);
		        	arrEnrol.add(enrolItem);
	        	}else {
		        	enrolItem.put("success", "N");
		        	enrolItem.put("title", "");
		        	enrolItem.put("device_id", Long.parseLong(deviceId));
		        	enrolItem.put("device_sn", deviceSN);
		        	arrEnrol.add(enrolItem);
		        	continue;
	        	}
	        }

			RestApiParameter convert = new RestApiParameter();
        	
			//csr 생성 및 request hash, public key 얻기
	        for(int i = 0; i<arrEnrol.size(); i++) {
	        	enrolItem = arrEnrol.get(i);
	        	Object bValid = enrolItem.get("check");
	        	
	        	if(bValid != null) {
	        		
	        		if(bValid.equals(true)) {
	        			CertSettingVO setVO = new CertSettingVO();
	        			String settingCodeGroupId = enrolItem.get("code_group_id").toString();
	        			String settingCodeId = enrolItem.get("code_id").toString();
	        			
	                	setVO.setCert_type_code_group(settingCodeGroupId);
	                	setVO.setCert_type_code(settingCodeId);
	                	setVO = certSettingService.getSetting(setVO);
	        			
	        			String title = enrolItem.get("title").toString();
			        	JSONObject setting = (JSONObject)parser.parse(setVO.getSetting());
			        	JSONObject duration = (JSONObject)((JSONObject)setting.get("validity_period")).get("duration");
		
			        	int nCount =  Integer.parseInt(setting.get("count").toString());
			        	int[] arrPsid = convert.toArray((JSONArray)setting.get("psids"));
			        	int[] arrContryCode = convert.toArray((JSONArray)setting.get("country_code"));
			        	int[] arrCircularRegin = convert.toArray((JSONArray)setting.get("circular_region"));
			        	String strDurationUint = duration.get("unit").toString();
			        	int nDurationValue = Integer.parseInt(duration.get("value").toString());
			        	
			        	//*
			        	JSONObject enrolResult = api.makeEnrol(title, nCount, arrPsid, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
			        	if(enrolResult.get("success").equals("Y")) {
			        		enrolItem.put("success", "Y");
			        		enrolItem.put("public_key", enrolResult.get("public_key").toString());
			        		enrolItem.put("enrol_validity_end", enrolResult.get("enrol_validity_end").toString());
			        		enrolItem.put("enrol_validity_start", enrolResult.get("enrol_validity_start").toString());
			        		enrolItem.put("request_hash", enrolResult.get("request_hash").toString());
			        		enrolItem.put("oer_title", enrolResult.get("oer_title").toString());
			        	}else {
			        		enrolItem.put("success", "N");
			        		enrolItem.put("msg", enrolResult.get("msg").toString());
			        	}
	        		}else {
		        		enrolItem.put("success", "N");
		        		enrolItem.put("msg", "생성할 보안인증서에 따라 단말의 OBU, RSU를 정상적으로 선택해주세요");
		        	}	
	        	}else {
	        		enrolItem.put("success", "N");
	        		enrolItem.put("msg", "선택한 단말기는 등록인증서가 존재합니다.");
	        	}
	        }
	        
	        //DB에 등록
	        for(int i = 0; i<arrEnrol.size(); i++) {
	        	enrolItem = arrEnrol.get(i);
	        	String deviceSn = (String)enrolItem.get("device_sn");
	        	
	        	if(enrolItem.get("success").equals("Y")) {
	        		String key = (String)enrolItem.get("title");
		        	String requestHash = (String)enrolItem.get("request_hash");
		        	String publicKey = (String)enrolItem.get("public_key");
		        	Long deviceId = (Long)enrolItem.get("device_id");
		        	String enrolValidityEnd = (String)enrolItem.get("enrol_validity_end");
		        	String enrolValidityStart = (String)enrolItem.get("enrol_validity_start");
		        	
		        	CertificationVO vo  = new CertificationVO();
					CertificatesKeyVO kVo = new CertificatesKeyVO();
					DeviceVO dVo = new DeviceVO();
					
		        	vo.setCert_id(key);
					vo.setDevice_id(deviceId);
					vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
					vo.setCert_state("2");
					vo.setCert_state_group_id("ECG005");
					vo.setExpiry_date(enrolValidityEnd);
					vo.setIssue_date(enrolValidityStart);
					vo.setCert_type("3");
					vo.setCert_type_group("ECG028");
					
					kVo.setDevice_id(deviceId);
					kVo.setCert_enrol_id(key);
					kVo.setCert_sec_id(null);
					kVo.setRequest_hash(requestHash);
					kVo.setPublic_key(publicKey);	
					
					dVo.setDevice_id(deviceId.toString());
					dVo.setDevice_cert_type("U");
					
					this.certService.updateDeviceCertType(dVo);
					this.certService.insertCert(vo);
					this.certService.insertCertKey(kVo);

					JSONObject contents = lh.createEnrol(req, kVo.getSeq(), vo.getCert_id(), deviceId, "[ "+deviceSn+" ] 등록인증서 발급");
					lg.insertLog(Location.EnrolCreate, Common.Success, pm.getUserId(req), contents);
	        	}else {
	        		Long deviceId = (Long)enrolItem.get("device_id");
	        		JSONObject contents = lh.createEnrol(req, null, null, deviceId, "[ "+deviceSn+" ] 등록인증서 발급 실패 !!"+(String)enrolItem.get("msg"));
					Long seq = lg.insertLog(Location.EnrolCreate, Common.Fail, pm.getUserId(req), contents);
					enrolItem.put("log", seq);
	        	}
	        }
	        
			JSONArray ja = new JSONArray();
			
	        JSONArray arrRetval = new JSONArray();
	        for(HashMap<String, Object> item : arrEnrol) {
	        	Long deviceId = (Long)item.get("device_id");
	        	String succ = (String)item.get("success");
	        	String deviceSn = (String)item.get("device_sn");
	        	Long logSeq = (Long)item.get("log");
	        	JSONObject retvalItem = new JSONObject();
	        	
	        	/*=======================등록인증서 요청 후 부트스트랩 다운로드 start===================================*/
				// 부트스트랩 다운로드
    			String certID = item.get("title").toString();
    			
    			try {
    				String bootFileName = "";
    				byte[] bootFileData = null;
    				String PKFileName = "";
    				byte[] PKFileData = null;
    				
    				CertificationVO vo = new CertificationVO();
    						
    				vo.setDevice_id(deviceId);
    				vo.setCert_id(certID);
    				Map<String, Object> item2 = certService.selectCert(vo);
    				Object requestHash = item2.get("request_hash");
    		
    				// 부트스트랩 파일
    				JSONObject bootResult = api.downloadBootstrap(requestHash.toString());
    				if(bootResult.get("success").equals("Y")) {
    					bootFileName = bootResult.get("fileName").toString();
    					bootFileData = (byte[])bootResult.get("fileData");
    					
    					// 개인키
    					JSONObject PKResult = api.privateKeyDownload(certID);
    					if(PKResult.get("success").equals("Y")) {
    						PKFileName = PKResult.get("fileName").toString();
    						PKFileData = (byte[])PKResult.get("fileData");
    						
    						JSONObject dl = new JSONObject();
    						
    						dl.put("fileData", new ZipHelper().makeEnrZip(bootFileName, bootFileData, PKFileName, PKFileData, deviceSn));
    						dl.put("fileName", deviceSn + ".zip");
    						
    						ja.add(dl);
    					}else {
    						JSONObject dl = new JSONObject();
    						
    						dl.put("fileData", new ZipHelper().makeOnlyEnrZip(bootFileName, bootFileData));
    						dl.put("fileName", deviceSn + ".zip");
    						
    						ja.add(dl);
    					}
    					
    					lg.insertLog(Location.EnrolBoostrapDownload, Common.Success, PermissionManagement.getUserId(req), 
    							lh.bootstrapDownload(req, requestHash.toString(), deviceId, certID, "[ "+deviceSn+" ] 부트스트랩 다운로드"));
    				}
    			}catch(Exception e) {
    				lg.insertLog(Location.EnrolBoostrapDownload, Common.Fail, PermissionManagement.getUserId(req), 
    						lh.bootstrapDownload(req, "", deviceId, certID, "[ "+deviceSn+" ] 부트스트랩 다운로드 실패 !!"+lh.error(e).toString()));
    			}
    			
				/*=======================등록인증서 요청 후 부트스트랩 다운로드 end===================================*/
	        	
	        	retvalItem.put("device_id", deviceId);
	        	retvalItem.put("success", succ);
	        	retvalItem.put("device_sn", deviceSn);
	        	retvalItem.put("log", logSeq);
	        	
	        	arrRetval.add(retvalItem);
	        }
	        
	        try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ZipOutputStream outputStream = null;
				ZipEntry entry = null;
				ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
				ZipOutputStream outputStream2 = null;
				ZipEntry entry2 = null;
				
				outputStream = new ZipOutputStream(baos);
				
				for(Object obj : ja) {
					JSONObject jobj = (JSONObject) obj;
					
					byte[] input = (byte[])jobj.get("fileData");
					
					entry = new ZipEntry(jobj.get("fileName").toString());
					entry.setSize(input.length);
					outputStream.putNextEntry(entry);
					outputStream.write(input);
					outputStream.closeEntry();
				}
				
				outputStream.close();
				
				String fileName = "enrol_file.zip";
				byte[] fileData = baos.toByteArray();
				
				outputStream2 = new ZipOutputStream(baos2);

				entry2 = new ZipEntry(fileName);
	            outputStream2.putNextEntry(entry2);
	            outputStream2.write(fileData);
	            outputStream2.closeEntry();
	    		outputStream2.close();
	    		
	    		retval.put("fileName", fileName);
	    		retval.put("fileData", Arrays.toString(fileData));
	    		
			}catch (Exception e) {
				e.printStackTrace();
			}
	        
	        retval.put("List", arrRetval);
	        retval.put("success", "Y");

			return retval.toString();
	
		}catch(Exception e) {
			JSONObject result = new JSONObject();
			e.printStackTrace();
			try {
				JSONObject contents = lh.createEnrol(req, null, null, null, "등록인증서 발급 실패 !!"+(String)enrolItem.get("msg"));
				lg.insertLog(Location.EnrolCreate, Common.Fail, pm.getUserId(req), contents);
			}catch(Exception e2) {
				e2.printStackTrace();
				result.put("success", "N");
				result.put("redirect", req.getContextPath() + "/logout.do");
				result.put("msg", "세");
				return result.toString();
			}
			
			result.put("success", "N");
			result.put("redirect", req.getContextPath() + "/enrol/list.do");
			result.put("msg", e.getMessage());
			return result.toString();
		}
	}
	
	@RequestMapping(value = {"/cert/enrol/bootstrap_private.do"}, method = RequestMethod.POST)
	public void DLEnrolPk(@RequestParam("dlData") String dlData, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		OutputStream os = null;
		String[] arrData = dlData.split(",");
		
		JSONArray ja = new JSONArray();
		os = resp.getOutputStream();
		try {			
			Location loc = Location.EnrolBoostrapDownload;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult) {
				resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>alert('" + PermissionManagement.getMsg(perResult) + "');window.close();</script>").getBytes());
				os.flush();
				os.close();
				
				return;
			}
		}catch(Exception e) {
			resp.setContentType("text/html;charset=UTF-8");
			os = resp.getOutputStream();
			os.write(("<script>alert('" + "세션이 만료 되었습니다." + "');window.close();</script>").getBytes());
			os.flush();
			os.close();
			
			e.printStackTrace();
			return;
		}
		
		for(String data : arrData) {
			
			String[] arrDlDate = data.split(":");
			String certID = arrDlDate[0];
			String deviceID = arrDlDate[1];
			String deviceSN = arrDlDate[2];
			Long nDeviceID = Long.parseLong(deviceID);
			
			try {
				String bootFileName = "";
				byte[] bootFileData = null;
				String PKFileName = "";
				byte[] PKFileData = null;
				
				CertificationVO vo = new CertificationVO();
						
				vo.setDevice_id(nDeviceID);
				vo.setCert_id(certID);
				Map<String, Object> item = certService.selectCert(vo);
				Object requestHash = item.get("request_hash");
		
				RestApi api = PermissionManagement.getApi(req);
				
				// 부트스트랩 파일
				JSONObject bootResult = api.downloadBootstrap(requestHash.toString());
				if(bootResult.get("success").equals("Y")) {
					bootFileName = bootResult.get("fileName").toString();
					bootFileData = (byte[])bootResult.get("fileData");
					
					// 개인키
					JSONObject PKResult = api.privateKeyDownload(certID);
					if(PKResult.get("success").equals("Y")) {
						PKFileName = PKResult.get("fileName").toString();
						PKFileData = (byte[])PKResult.get("fileData");
						
						JSONObject dl = new JSONObject();
						
						dl.put("fileData", new ZipHelper().makeEnrZip(bootFileName, bootFileData, PKFileName, PKFileData, deviceSN));
						dl.put("fileName", deviceSN + ".zip");
						
						ja.add(dl);
					}else {
						JSONObject dl = new JSONObject();
						
						dl.put("fileData", new ZipHelper().makeOnlyEnrZip(bootFileName, bootFileData));
						dl.put("fileName", deviceSN + ".zip");
						
						ja.add(dl);
					}
					
					
					lg.insertLog(Location.EnrolBoostrapDownload, Common.Success, PermissionManagement.getUserId(req), 
							lh.bootstrapDownload(req, requestHash.toString(), nDeviceID, certID, "[ "+deviceSN+" ] 부트스트랩 다운로드"));
				}else {
					resp.setContentType("text/html;charset=UTF-8");
					os = resp.getOutputStream();
					os.write(("<script>alert('" + bootResult.get("msg") + "');window.close();</script>").getBytes());
					os.flush();
					os.close();
				}
					 
			}catch(Exception e) {
				if(os != null) {
					os.flush();
					os.close();
				}
				
				lg.insertLog(Location.EnrolBoostrapDownload, Common.Fail, PermissionManagement.getUserId(req), 
						lh.bootstrapDownload(req, "", nDeviceID, certID, "[ "+deviceSN+" ] 부트스트랩 다운로드 실패 !!"+lh.error(e).toString()));
			}
		}
		
		if(ja.size() > 0) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ZipOutputStream outputStream = null;
				ZipEntry entry = null;
				ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
				ZipOutputStream outputStream2 = null;
				ZipEntry entry2 = null;
				
				outputStream = new ZipOutputStream(baos);
				
				for(Object obj : ja) {
					JSONObject jobj = (JSONObject) obj;
					
					byte[] input = (byte[])jobj.get("fileData");
					
					entry = new ZipEntry(jobj.get("fileName").toString());
					entry.setSize(input.length);
					outputStream.putNextEntry(entry);
					outputStream.write(input);
					outputStream.closeEntry();
				}
				
				outputStream.close();
				
				String fileName = "enrol_file.zip";
				byte[] fileData = baos.toByteArray();
				
				outputStream2 = new ZipOutputStream(baos2);
				
				entry2 = new ZipEntry(fileName);
				outputStream2.putNextEntry(entry2);
				outputStream2.write(fileData);
				outputStream2.closeEntry();
				outputStream2.close();
				
				resp.setContentType("application/zip");
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
				resp.setHeader("Content-Transfer-Encoding", "binary");
				os.write(fileData);
				
				os.flush();
				os.close();
			}catch (Exception e) {
				e.printStackTrace();
				
				lg.insertLog(Location.EnrolBoostrapDownload, Common.Fail, PermissionManagement.getUserId(req), 
						lh.bootstrapDownload(req, "", null, "", "부트스트랩 다운로드 실패(zip 반환 실패) !!"+lh.error(e).toString()));
				
				resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>alert('" + "zip 반환 실패" + "');window.close();</script>").getBytes());
				os.flush();
				os.close();
			}
		}
	}
}
