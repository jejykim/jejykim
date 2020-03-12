package com.decision.v2x.era.web;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.acl.Permission;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.spi.DirStateFactory.Result;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.jasper.tagplugins.jstl.core.Url;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.decision.v2x.era.VO.CertSettingVO;
import com.decision.v2x.era.VO.CertificatesKeyVO;
import com.decision.v2x.era.VO.CertificationVO;
import com.decision.v2x.era.VO.DeviceAndCertListVO;
import com.decision.v2x.era.VO.DeviceVO;
import com.decision.v2x.era.VO.SecCertWaitVO;
import com.decision.v2x.era.mapper.SecCertWaitMapper;
import com.decision.v2x.era.service.impl.CertSettingService;
import com.decision.v2x.era.service.impl.CertificationService;
import com.decision.v2x.era.service.impl.CodeService;
import com.decision.v2x.era.service.impl.DeviceService;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.service.impl.PermissionService;
import com.decision.v2x.era.service.impl.SecCertWaitService;
import com.decision.v2x.era.util.Exception.EmptyUserException;
import com.decision.v2x.era.util.Exception.NullSessionException;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.convert.DateHelper;
import com.decision.v2x.era.util.convert.LogHelper;
import com.decision.v2x.era.util.convert.RestApiParameter;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator;
import com.decision.v2x.era.util.log.LogGenerator.Common;
import com.decision.v2x.era.util.log.LogGenerator.Location;
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
	
	
	
	@RequestMapping(value = {"/cert/enrol/list.do", "/cert/enrolList.do"}, method = RequestMethod.GET)
	public String enrolList(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
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
			PermissionManagement.setSessionValue(req, "errMsg", "DB와 연결을 확인해주세요");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		try
		{
			//List<HashMap<String, Object>> deviceIdTypeList = (List<HashMap<String, Object>>) codeService.getDeviceIdTypeList();
			//List<HashMap<String, Object>> deviceTypeList = (List<HashMap<String, Object>>) codeService.getDeviceTypeList();
			model.addAttribute("deviceIdTypeList", codeService.getDeviceIdTypeList());
			model.addAttribute("deviceTypeList", codeService.getDeviceTypeList());
			
			HttpSession ss = req.getSession();
			
			//현재 페이지
			int certNowPage = 1;
			
			//현재 페이지에서 보여줄 최대 값
			int certMaxPage = 5;
			
			//현재 페이지의 보여줄 최소 값
			int certMinPage = 1;
			
			//현재 페이지에서 보여줄 레코드의 개수
			int certCntList = 10;
			
			ss.setAttribute("now", certNowPage);
			ss.setAttribute("max", certMaxPage);
			ss.setAttribute("min", certMinPage);
			ss.setAttribute("row", certCntList);
			ss.removeAttribute("searchDeviceSN");
			ss.removeAttribute("searchIssueDate");
			ss.removeAttribute("searchExpireDate");
			ss.removeAttribute("searchCreateDate");
			ss.removeAttribute("searchModifyDate");
			ss.removeAttribute("searchDeviceType");
			ss.removeAttribute("searchDeviceIdType");
			ss.removeAttribute("searchCreateCertId");
			
			DeviceAndCertListVO vo = new DeviceAndCertListVO((certNowPage - 1) * certCntList, certCntList);
			Map<String, Object> cnt = certService.selectEnrolCertCnt(vo);
			
			//logger.info(cnt);
			
			//보여줄 수 있는 페이지의 최대 개수
			int maxPage = Integer.parseInt(cnt.get("totalPageCnt").toString());
			int totalCnt = Integer.parseInt(cnt.get("totalCnt").toString());
			
			//인증서 목록
 			model.addAttribute("certList", certService.selectEnrolCertList(vo));
			
			//DB에 기록된 인증서의 총 개수
			model.addAttribute("certTotalCnt", totalCnt);

			//최초 값이 5라서 아래 코드가 없을 경우 1 ~ 5까지 나온다(인증서의 개수에 상관없이).
			certMaxPage = maxPage > certMaxPage ? certMaxPage : maxPage;
			
			
			//표시가 가능한 최대 페이지의 개수
			model.addAttribute("certTotalPageCnt", maxPage);
			
			//현재 페이지의 위치
			model.addAttribute("certNowPage", certNowPage);
			
			//사용자에게 보여줄 최대 페이지
			model.addAttribute("certMaxPage", certMaxPage);
			
			//사용자에게 보여줄 최소 페이지
			model.addAttribute("certMinPage", certMinPage);
			
			lg.insertLog(Location.EnrolList, Common.Success, PermissionManagement.getUserId(req), lh.list(req, certCntList, certNowPage, totalCnt, ""));
		}
		catch(Exception e)
		{
			lg.insertLog(Location.EnrolList, Common.Error, 
					PermissionManagement.getUserId(req), 
					lh.list(req, 0, 0, 0, lh.error(e).toString())
					);
		}
		
		return "certifications/enrolList";
	}
	
	@RequestMapping(value = {"/cert/enrol/list.do", "/cert/enrol/search.do"}, method = RequestMethod.POST)
	public String enrolList2(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
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
		String issueDate = "";
		String expireDate = "";
		String createDate = "";
		String modifyDate = "";
		String deviceSN = "";
		String deviceIdType = "";
		String deviceType = "";
		String createCertId = "";
		String[] arr = null;
		int certNowPage = 0;
		int certMaxPage = 0;
		int certMinPage = 0;
		int certCntList = 0;
		int totalCnt = 0;
		
		try
		{
			model.addAttribute("deviceIdTypeList", codeService.getDeviceIdTypeList());
			model.addAttribute("deviceTypeList", codeService.getDeviceTypeList());
			
			final String splitDate = " - ";
			issueDate = req.getParameter("issueDate");
			expireDate = req.getParameter("expireDate");
			createDate = req.getParameter("createDate");
			modifyDate = req.getParameter("modifyDate");
			deviceSN = req.getParameter("deviceSN");
			deviceType = req.getParameter("deviceType");
			deviceIdType = req.getParameter("deviceIdType");
			
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
			if(isCheck(deviceType))
			{
				arr = deviceType.split("=");
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchDeviceType", deviceType);
					vo.setDevice_type(arr[0]);
					vo.setDevice_type_group(arr[1]);
				}
			}
			if(isCheck(deviceIdType))
			{
				arr = deviceIdType.split("=");
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchDeviceIdType", deviceIdType);
					vo.setDevice_sn_type_id(arr[0]);
					vo.setDevice_sn_type_group(arr[1]);
				}
			}
			if(isCheck(createCertId))
			{
				ss.setAttribute("searchCertCreateId", createCertId);
				vo.setCert_create_id(createCertId);
			}
			
			
			Map<String, Object> cnt = certService.selectEnrolCertCnt(vo);
			int maxPage = Integer.parseInt(cnt.get("totalPageCnt").toString());
			totalCnt = Integer.parseInt(cnt.get("totalCnt").toString());
			
			certMaxPage = maxPage > certMaxPage ? certMaxPage : maxPage;
			
			//*
			ss.setAttribute("now", certNowPage);
			ss.setAttribute("max", certMaxPage);
			ss.setAttribute("min", certMinPage);
			ss.setAttribute("row", certCntList);
			//*/
			
 			model.addAttribute("certList", certService.selectEnrolCertList(vo));
			
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
			
			if(req.getRequestURI().indexOf("enrol/list") != -1)
			{
				lg.insertLog(Location.EnrolList, Common.Success, PermissionManagement.getUserId(req), 
						lh.list(req, certCntList, certNowPage, totalCnt, ""));
			}
			else
			{
				lg.insertLog(Location.EnrolSearch, Common.Success, PermissionManagement.getUserId(req), 
					lh.certSearch(req, certCntList, certNowPage, totalCnt, issueDate, expireDate, createDate, modifyDate, deviceSN, "", ""));
			}
			
		}
		catch(Exception e)
		{
			if(req.getRequestURI().indexOf("enrol/list") != -1)
			{
				lg.insertLog(Location.EnrolList, Common.Error, PermissionManagement.getUserId(req), 
						lh.list(req, certCntList, certNowPage, totalCnt, 
								lh.error(e).toString()));
			}
			else
			{
				lg.insertLog(Location.EnrolSearch, Common.Error, PermissionManagement.getUserId(req), 
						lh.certSearch(req, certCntList, certNowPage, totalCnt, issueDate, expireDate, createDate, modifyDate, deviceSN, "", 
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

			if(isCheck(issueYN))
			{
				if(issueYN.equals("Y") || issueYN.equals("N"))
				{
					
				}
				else
				{
					issueYN = "";
				}
				
				logger.info(issueYN);
				ss.setAttribute("searchIssueYn", issueYN);
				vo.setIssueYn(deviceSN);
			}
			else
			{
				ss.setAttribute("searchIssueYn", "");
				vo.setIssueYn(deviceSN);
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
		String[] certType = form.get("txtCertType");
		
		if(arrDeviceId == null || certType == null)
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
			
			LogGenerator.Common perResult = LogGenerator.Common.Error;
			switch(certType[0])
			{
			case "app":
				perResult = pm.isPermission(req, lg, lh, Location.MakeApp);
				break;
				
			case "pse":
				perResult = pm.isPermission(req, lg, lh, Location.MakePse);
				break;
				
			case "ide":
				perResult = pm.isPermission(req, lg, lh, Location.MakeIde);
				break;
			}
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
		
		//*
		String userId = (String) req.getSession().getAttribute("user_id");
		JSONObject retval = new JSONObject();
		JSONArray resultList = new JSONArray();
		
		
		JSONParser parser = new JSONParser();
		
		/*
		String strDurationUint = "HOURS";
		int nDurationValue = 4320;
		int[] arrContryCode = new int[] {410};
		int[] arrCircularRegin = null;
		int[] appPsids = new int[] {38, 135, 82049, 82053, 82054, 82055, 82056, 82057, 82059};
		int[] idePsids = new int[] {32, 82058, 38, 82050, 82051, 82052, 82054, 82055, 82056, 82057};
		int[] psePsids = new int[] {32, 82058, 38, 82050, 82051, 82052, 82054, 82055, 82056, 82057};
		//*/
		
		String certTypeId = "";
		String certTypeGroup = "";		
		String strDurationUint = "HOURS";
		int nDurationValue = 4320;
		int[] arrContryCode = new int[] {410};
		int[] arrCircularRegin = null;
		int[] arrPsids = new int[] {38, 135, 82049, 82053, 82054, 82055, 82056, 82057, 82059};
		int count = 0;
		String type = "";
		
		try
		{
			RestApi api = PermissionManagement.getApi(req);
			
			try
			{
				switch(certType[0])
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
						String setting = certService.getAppSetting();
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
						String setting = certService.getAppSetting();
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
					
				default:
					throw new Exception(certType[0] + "는 존재하지 않는 보안인증서의 종류입니다.");
				}
			}
			catch(Exception e)
			{
				PermissionManagement.setSessionValue(req, "errMsg", e.getMessage());
			
				return "redirect:/cert/makeList.do";
			}
			
			
			for(int i = 0; i<arrDeviceId.length; i++)
			{
				/*
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", "기존에 등록된 보안인증서를 제거해주세요");
					resultList.add(item);
					continue;



 
				 */
				
				
				
				JSONObject result = new JSONObject();
				JSONObject item = new JSONObject();
				String[] arr = arrDeviceId[i].split("=");
				String deviceSN = arr[1];
				String deviceID = arr[0];
				DeviceVO deviceVO = new DeviceVO();
				
				deviceVO.setDevice_sn(deviceSN);
				deviceVO.setDevice_id(deviceID);
				
				
				//단말의 정보를 조회
				Map<String, Object> deviceType = certService.selectDeviceType(deviceVO);
				
				//조회된 단말이 없음
				if(deviceType == null)
				{
					switch(certType[0])
					{
					case "app":
						lg.insertLog(Location.MakeApp, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, "잘못된 단말의 정보입니다."));
						break;
						
					case "pse":
						lg.insertLog(Location.MakePse, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, "잘못된 단말의 정보입니다."));
						break;
						
					case "ide":
						lg.insertLog(Location.MakeIde, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, "잘못된 단말의 정보입니다."));
						break;
					}
					continue;	
				}
				

				//RSU, OBU에 따라 APP, IDE, PSE의 생성유무를 검사한다.
				boolean bCheckDeviceType = false;
				if(certType[0].equals("app"))
				{
					//RSU: APP 	rsu_2:CMM801
					if(deviceType.get("device_type").equals("rsu_2") && 
							deviceType.get("device_type_group").equals("CMM801"))
					{
						bCheckDeviceType = true;
					}
				}
				else if(certType[0].equals("pse"))
				{
					//OBU: PSE, IDE		obu_1:CMM801
					if(deviceType.get("device_type").equals("obu_1") &&
							deviceType.get("device_type_group").equals("CMM801"))
					{
						bCheckDeviceType = true;
					}
				}
				else if(certType[0].equals("ide"))
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
					
				}
				
				//RSU, OBU에 따른 보안인증서의 종류가 맞지 않음
				if(!bCheckDeviceType)
				{
					String deviceErrorMsg = "";
					switch(certType[0])
					{
					case "app":
						deviceErrorMsg = "단말 타입이 RSU가 아닙니다.";
						lg.insertLog(Location.MakeApp, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, deviceErrorMsg));
						break;
						
					case "pse":
						deviceErrorMsg = "단말 타입이 OBU가 아닙니다.";
						lg.insertLog(Location.MakePse, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, deviceErrorMsg));
						break;
						
					case "ide":
						deviceErrorMsg = "단말 타입이 OBU가 아닙니다.";
						lg.insertLog(Location.MakeIde, Common.Fail, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, deviceErrorMsg));
						break;
					}
					
					item.put("deviceSN", deviceSN);
					item.put("success", "N");
					item.put("msg", deviceErrorMsg);
					resultList.add(item);
					continue;
				}
				
				try
				{
					
					CertificatesKeyVO kVo = new CertificatesKeyVO();
					CertificationVO vo = new CertificationVO();
					String key = certService.selectId();
					
					switch(certType[0])
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
						
					default:
						throw new Exception("잘못된 Type입니다: " + certType[0]);
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
						
						SecCertWaitVO scwVo = new SecCertWaitVO(kVo.getSeq(), result.get("download_time").toString(),
								result.get("download_url").toString(), result.get("wait_time").toString(),
								result.get("request_date").toString());
						
						this.secCertWaitService.insert(scwVo);
						
						
						
						item.put("deviceSN", deviceSN);
						item.put("success", "Y");
						item.put("msg", "");
						
						switch(certType[0])
						{
						case "app":
							lg.insertLog(Location.MakeApp, Common.Success, PermissionManagement.getUserId(req), 
									lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
							break;
							
						case "pse":
							lg.insertLog(Location.MakePse, Common.Success, PermissionManagement.getUserId(req), 
									lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
							break;
							
						case "ide":
							lg.insertLog(Location.MakeIde, Common.Success, PermissionManagement.getUserId(req), 
									lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
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
					
					switch(certType[0])
					{
					case "app":
						lg.insertLog(Location.MakeApp, Common.Error, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
								);
						break;
						
					case "pse":
						lg.insertLog(Location.MakePse, Common.Error, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
							);
						break;
						
					case "ide":
						lg.insertLog(Location.MakeIde, Common.Error, PermissionManagement.getUserId(req), 
								lh.makeCert(req, certType[0], deviceSN, count, arrPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
							);
						break;
					}
				}
				
				resultList.add(item);
			}
		}
		catch(Exception e)
		{
			retval.put("errMsg", lh.error(e).toString());
			switch(certType[0])
			{
			case "app":
				lg.insertLog(Location.MakeApp, Common.Error, PermissionManagement.getUserId(req), 
						lh.error(req, e.getMessage()));
				break;
				
			case "pse":
				lg.insertLog(Location.MakePse, Common.Error, PermissionManagement.getUserId(req), 
						lh.error(req, e.getMessage()));
				break;
				
			case "ide":
				lg.insertLog(Location.MakeIde, Common.Error, PermissionManagement.getUserId(req), 
						lh.error(req, e.getMessage()));
				break;
			}
		}
		
		String errMsg = "보안 인증서를 생성하지 못한 단말: ";
		String succMsg = "보안 인증서가 생성된 단말: ";
		for(int i = 0; i<resultList.size(); i++)
		{
			JSONObject item = (JSONObject)resultList.get(i);
			
			logger.info(item.get("success") + " => " + item.get("deviceSN"));
			
			if(item.get("success").equals("Y"))
			{
				succMsg += item.get("deviceSN") + ", ";
			}
			else
			{
				errMsg += item.get("deviceSN") + ", ";
			}
		}
		
		PermissionManagement.setSessionValue(req, "errMsg", errMsg);
		PermissionManagement.setSessionValue(req, "succMsg", succMsg);
		
		//retval.put("List", resultList);
		//req.getSession().setAttribute("result", retval.toString());
		model.addAttribute("result", retval);
		//*/
		
		return "redirect:/cert/makeList.do";
	}
	
	@RequestMapping(value = {"/cert/enrol/delete.do", "/cert/make/delete.do"}, method = RequestMethod.POST)
	public String certDelete(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{		
		String certs = req.getParameter("certs");
		certs = org.springframework.web.util.HtmlUtils.htmlUnescape(certs);
		Location loc = null;
		String uri = req.getRequestURI();
		
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
			PermissionManagement.setSessionValue(req, "errMsg", "파라미터 오류");
///////////////////////////////////////POST 파리미터 오류
			
			if(loc == Location.MakeDelete)
			{
				return "redirect:/cert/make/list.do";
			}
			else
			{
				return "redirect:/cert/enrol/list.do";	
			}
		}

		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
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
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		//*
		String userId = (String) req.getSession().getAttribute("user_id");
		JSONObject retval = new JSONObject();
		JSONArray resultList = new JSONArray();
		String errMsg = "";
		String succMsg = "";
		
		JSONParser parser = new JSONParser();

		try
		{
			JSONObject jCerts = (JSONObject)parser.parse(certs);			
			JSONArray arrCert = (JSONArray)jCerts.get("arrCert");
			
			if(arrCert.size() == 0)
			{
				if(loc == Location.MakeDelete)
				{
					return "redirect:/cert/make/list.do";
				}
				else
				{
					return "redirect:/cert/enrol/list.do";	
				}
			}
			
			RestApi api = PermissionManagement.getApi(req);
			for(int i = 0; i<arrCert.size(); i++)
			{
				String certId = arrCert.get(i).toString();
				DeviceAndCertListVO daclVo = certService.selectKey(certId);
				
				//*
				JSONObject result = api.certBlacklist(daclVo.getPublic_key());
				if(result.get("success").equals("Y"))
				{
					certService.deleteCertification(daclVo.getCert_key_id());
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
		}
		catch(Exception e)
		{
			Long log = lg.insertLog(loc, Common.Error, PermissionManagement.getUserId(req), 
					lh.error(req, e.getMessage()));
			
			errMsg = "오류가 발생했습니다. 자세한 사항은 " + log + "번을 참조해주세요";
		}
		
		if(errMsg.equals(""))
			PermissionManagement.setSessionValue(req, "errMsg", errMsg);
		
		if(succMsg.equals(""))
			PermissionManagement.setSessionValue(req, "succMsg", succMsg);
		
		if(loc == Location.MakeDelete)
		{
			return "redirect:/cert/make/list.do";
		}
		else
		{
			return "redirect:/cert/enrol/list.do";	
		}
		
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
			os.write(("<script>alert('" + "DB와 연결이 되지 않았습니다." + "');window.close();</script>").getBytes());
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
			os.write(("<script>alert('" + "DB와 연결이 되지 않았습니다." + "');window.close();</script>").getBytes());
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
			os.write(("<script>alert('" + "DB와 연결이 되지 않았습니다." + "');window.close();</script>").getBytes());
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
			Object requestHash = item.get("request_hash");
			
			
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
						//다운로드 가능
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
			os.write(("<script>alert('" + "DB와 연결이 되지 않았습니다." + "');window.close();</script>").getBytes());
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
	
	@ResponseBody
	@RequestMapping(value = "/cert/enrol/create.do", method = RequestMethod.POST)
	public String checkDeviceSn(@RequestBody String json, ModelMap model, HttpServletRequest req) throws Exception 
	{
		if(pm == null)
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				
				JSONObject result = new JSONObject();
				
				result.put("success", "N");
				result.put("redirect", req.getContextPath() + "/logout.do");
				result.put("msg", "서버가 재시작 됨");
				return result.toString();
			}
		}
		
		try
		{
			//권한 검사
		}
		catch(Exception e)
		{
			
		}
		
		
		
		
		
		RestApi api = PermissionManagement.getApi(req);
		ArrayList<HashMap<String, Object>> arrEnrol = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> enrolItem = null;
		RestApiParameter param = new RestApiParameter();
		
		try
		{
			JSONObject retval = new JSONObject();
			JSONParser parser = new JSONParser();


			json = json.replaceAll("=", "");
			json = URLDecoder.decode(json, "utf-8");

			JSONObject result = (JSONObject)parser.parse(json);
			JSONArray arrDevice = (JSONArray)result.get("device_id");
			CertSettingVO setVO = new CertSettingVO();
			String settingCodeGroupId = result.get("code_group_id").toString();
			String settingCodeId = result.get("code_id").toString();
			
        	setVO.setCert_type_code_group(settingCodeGroupId);
        	setVO.setCert_type_code(settingCodeId);
        	setVO = certSettingService.getSetting(setVO);
			
			for(int i = 0; i<arrDevice.size(); i++)
	        {
				Boolean bCheck = false;
	        	String[] splitDeviceId = arrDevice.get(i).toString().split(":");
	        	String deviceId = splitDeviceId[0];
	        	String deviceSN = splitDeviceId[1];
	        	
	        	DeviceVO dVO = new DeviceVO();
	        	dVO.setDevice_id(arrDevice.get(i).toString());
	        	
	        	//단말의 등록인증서 검사
	        	if(certService.selectEnrolCheck(dVO) == 0)
	        	{
		        	String key = certService.selectId();
		        	enrolItem = new HashMap<>();
		        	
		        	Map<String, Object> deviceType = certService.selectDeviceType(dVO);
	
		 
		        	
		        	//익명 인증서(OBU)
		        	if((settingCodeGroupId.equals("ECG028") && settingCodeId.equals("1")) || 
		        			
		        			//식별인증서(OBU)
		        			(settingCodeGroupId.equals("ECG028") && settingCodeId.equals("2")))
		        	{
		        		//식별인증서(OBU)
			        	if(deviceType.get("device_type").equals("obu_1") &&
			        			deviceType.get("device_type_group").equals("CMM801"))
			        	{
			        		bCheck = true;	
			        	}
		        		
		        	}
		        	
		        	//기지국 인증서(RSU)
		        	else if(settingCodeGroupId.equals("ECG028") && settingCodeId.equals("4"))
		        	{
		        	
			        	if(deviceType.get("device_type").equals("rsu_2") &&
			        			deviceType.get("device_type_group").equals("CMM801"))
			        	{
			        		bCheck = true;
			        	}
		        	}
		        	else
		        	{
		        		bCheck = false;
		        	}
		        	
		        	enrolItem.put("check", bCheck);
		        	enrolItem.put("success", "N");
		        	enrolItem.put("title", key);
		        	enrolItem.put("device_id", Long.parseLong(deviceId));
		        	enrolItem.put("device_sn", deviceSN);
		        	arrEnrol.add(enrolItem);
	        	}
	        	else
	        	{
		        	enrolItem.put("success", "N");
		        	enrolItem.put("title", "");
		        	enrolItem.put("device_id", Long.parseLong(deviceId));
		        	enrolItem.put("device_sn", deviceSN);
		        	arrEnrol.add(enrolItem);
		        	continue;
	        	}
	        }

	        //*
			RestApiParameter convert = new RestApiParameter();
        	
			//csr 생성 및 request hash, public key 얻기
	        for(int i = 0; i<arrEnrol.size(); i++)
	        {
	        	enrolItem = arrEnrol.get(i);
	        	Object bValid = enrolItem.get("check");
	        	boolean bExistEnrol = bValid == null ? true : false;
	        	
	        	if(bValid != null)
	        	{
	        		if(bValid.equals(true))
	        		{
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
			        	if(enrolResult.get("success").equals("Y"))
			        	{
			        		enrolItem.put("success", "Y");
			        		enrolItem.put("public_key", enrolResult.get("public_key").toString());
			        		enrolItem.put("enrol_validity_end", enrolResult.get("enrol_validity_end").toString());
			        		enrolItem.put("enrol_validity_start", enrolResult.get("enrol_validity_start").toString());
			        		enrolItem.put("request_hash", enrolResult.get("request_hash").toString());
			        		enrolItem.put("oer_title", enrolResult.get("oer_title").toString());
			        	}
			        	else
			        	{
			        		enrolItem.put("success", "N");
			        		enrolItem.put("msg", enrolResult.get("msg").toString());
			        	}
			
	        		}
	        		else
		        	{
		        		enrolItem.put("success", "N");
		        		enrolItem.put("msg", "생성할 보안인증서에 따라 단말의 OBU, RSU를 정상적으로 선택해주세요");
		        	}	
	        	}
	        	else
	        	{
		        	if(bExistEnrol)
		        	{
		        		enrolItem.put("success", "N");
		        		enrolItem.put("msg", "등록인증서가 존재합니다.");
		        	}
	        	}
	        //*/
	        }
	        
	        //*
	        ///DB에 등록
	        for(int i = 0; i<arrEnrol.size(); i++)
	        {
	        	enrolItem = arrEnrol.get(i);
	        	if(enrolItem.get("success").equals("Y"))
	        	{
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
					
					kVo.setCert_enrol_id(key);
					kVo.setCert_sec_id(null);
					kVo.setRequest_hash(requestHash);
					kVo.setPublic_key(publicKey);	
					
					dVo.setDevice_id(deviceId.toString());
					dVo.setDevice_cert_type("U");
					
					this.certService.updateDeviceCertType(dVo);
					this.certService.insertCert(vo);
					this.certService.insertCertKey(kVo);

					JSONObject contents = lh.createEnrol(req, kVo.getSeq(), vo.getCert_id(), deviceId, "");
					lg.insertLog(Location.EnrolCreate, Common.Success, pm.getUserId(req), contents);
	        	}
	        	else
	        	{
	        		Long deviceId = (Long)enrolItem.get("device_id");
	        		JSONObject contents = lh.createEnrol(req, null, null, deviceId, (String)enrolItem.get("msg"));
					Long seq = lg.insertLog(Location.EnrolCreate, Common.Fail, pm.getUserId(req), contents);
					enrolItem.put("log", seq);
	        	}
	        }
	        //*/
	        
	        JSONArray arrRetval = new JSONArray();
	        for(HashMap<String, Object> item : arrEnrol) 
	        {
	        	Long deviceId = (Long)item.get("device_id");
	        	String succ = (String)item.get("success");
	        	String deviceSn = (String)item.get("device_sn");
	        	Long logSeq = (Long)item.get("log");
	        	JSONObject retvalItem = new JSONObject();
	        	
	        	retvalItem.put("device_id", deviceId);
	        	retvalItem.put("success", succ);
	        	retvalItem.put("device_sn", deviceSn);
	        	retvalItem.put("log", logSeq);
	        	
	        	arrRetval.add(retvalItem);
	        }
	        
	        retval.put("List", arrRetval);
	        retval.put("success", "Y");

			return retval.toString();
	
		}
		catch(Exception e)
		{
			JSONObject result = new JSONObject();
			e.printStackTrace();
			try
			{
				JSONObject contents = lh.createEnrol(req, null, null, null, (String)enrolItem.get("msg"));
				lg.insertLog(Location.EnrolCreate, Common.Fail, pm.getUserId(req), contents);
			}
			catch(Exception e2)
			{
				e2.printStackTrace();
				result.put("success", "N");
				result.put("redirect", req.getContextPath() + "/logout.do");
				result.put("msg", "DB 접속을 확인해주세요");
				return result.toString();
			}
			
			result.put("success", "N");
			result.put("redirect", req.getContextPath() + "/devices/list.do");
			result.put("msg", e.getMessage());
			return result.toString();
		}
	}
}
