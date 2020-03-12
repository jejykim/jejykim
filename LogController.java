package com.decision.v2x.era.web;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.decision.v2x.era.VO.DeviceAndCertListVO;
import com.decision.v2x.era.VO.LogVO;
import com.decision.v2x.era.VO.UserVO;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.service.impl.PermissionService;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.convert.LogHelper;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator;
import com.decision.v2x.era.util.log.LogGenerator.Common;
import com.decision.v2x.era.util.log.LogGenerator.Location;

@Controller
public class LogController 
{
	Logger logger = Logger.getLogger(LogController.class);
	
	@Resource(name = "permissionService")
	private PermissionService permissionService;
	
	@Resource(name = "logService")
	private LogService logService;
	
	private PermissionManagement pm = null;
	private LogGenerator lg = null;
	private LogHelper lh = new LogHelper();
	
	static boolean isCheck(String item)
	{
		if(item != null && !item.isEmpty())
		{
			return true;
		}
		
		return false;
	}
	
	@PostConstruct
	public void init() throws Exception
	{
		pm = new PermissionManagement(permissionService);
		lg = new LogGenerator(logService);
	}
	
	public String getIntToMode(String id)
	{
		String[] arrFind = {"1", 		"2", 		"3", 		"4", 				"5", 			 "6"};
		String[] arrMode = {"Device", 	"User", 	"Access",	"UserPermission",	"UserChangePwd", "UserNoAccess"};
		String mode = "";
		
		for(int i = 0; i<arrFind.length; i++)
		{
			if(id.indexOf(arrFind[i]) != -1)
			{
				mode = arrMode[i];
				break;
			}
		}
		
		return mode;
	}
	public String getModeToInt(String mode)
	{
		String[] arrFind = {"1", 		"2", 		"3", 		"4", 				"5", 			 "6"};
		String[] arrMode = {"Device", 	"User", 	"Access",	"UserPermission",	"UserChangePwd", "UserNoAccess"};
		String retval = "";
		
		for(int i = 0; i<arrFind.length; i++)
		{
			if(mode.indexOf(arrMode[i]) != -1)
			{
				retval = arrFind[i];
				break;
			}
		}
		
		return retval;
	}
	
	@RequestMapping(value = {"/log/list.do"}, method = RequestMethod.GET)
	public String logList(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String modeId = req.getParameter("type");
		
		if(modeId != null)
		{
			try
			{
				Integer.parseInt(modeId);
			}
			catch(Exception e)
			{
				modeId = "";
				lg.insertLog(Location.LogList, Common.Error, pm.getUserId(req), lh.list(req, 0, 0, 0, "비정상적인 접근으로 해킹으로 의심됨"));
			}
		}
		else
		{
			modeId = "";
		}
		
		String[] arrFind = {"1", 		"2", 		"3", 		"4", 				"5", 			 "6"};
		String[] arrMode = {"Device", 	"User", 	"Access",	"UserPermission",	"UserChangePwd", "UserNoAccess"};
		String mode = "";
		
		for(int i = 0; i<arrFind.length; i++)
		{
			if(modeId.indexOf(arrFind[i]) != -1)
			{
				mode = arrMode[i];
				break;
			}
		}

		
		/*
		try
		{
			String[] arrFind = {"log/list", "log/device", "log/user", "log/access", "log/permission", "log/changePw", "log/unauthorized"};
			String[] arrMode = {"",		"Device", 		"User", 	"Access",	"UserPermission",	"UserChangePwd", "UserNoAccess"};
			String uri = req.getRequestURI();
			String mode = "";
			
			for(int i = 0; i<arrFind.length; i++)
			{
				if(uri.indexOf(arrFind[i]) != -1)
				{
					mode = arrMode[i];
					break;
				}
			}
	
			logger.info(uri);
			logger.info(mode);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//*/
		try
		{
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
			ss.removeAttribute("searchCreateId");
			ss.removeAttribute("searchCreateDate");
			ss.removeAttribute("searchFunc");
			ss.removeAttribute("searchCommon");
			
			LogVO vo = new LogVO((certNowPage - 1) * certCntList, certCntList);
			vo.setMode(mode);
			
			model.addAttribute("listMode", modeId);
			model.addAttribute("funcList", logService.selectFunc(vo));
			model.addAttribute("cmmFuncList", logService.selectCommonFunc(vo));
			model.addAttribute("logList", logService.selectLog(vo));
			Map<String, Object> cnt = logService.selectLogCnt(vo);
			
			
	
			//보여줄 수 있는 페이지의 최대 개수
			int maxPage = Integer.parseInt(cnt.get("totalPageCnt").toString());
			
			//DB에 기록된 인증서의 총 개수
			model.addAttribute("certTotalCnt", Integer.parseInt(cnt.get("totalCnt").toString()));
	
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
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
		return "log/log";
	}
	
	@RequestMapping(value = {"/log/list.do", "/log/search.do"}, method = RequestMethod.POST)
	public String logList2(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		try
		{
			final String splitDate = " - ";
			final String splitCode = "=";
			String createId = req.getParameter("createId");
			String createDate = req.getParameter("createDate");
			String func = req.getParameter("func");
			String common = req.getParameter("common");
			String[] arr = null;
			
			//현재 페이지
			int certNowPage = Integer.parseInt(req.getParameter("now"));
			
			//현재 페이지에서 보여줄 최대 값
			int certMaxPage = Integer.parseInt(req.getParameter("max"));
			
			//현재 페이지의 보여줄 최소 값
			int certMinPage = Integer.parseInt(req.getParameter("min"));
			
			//현재 페이지에서 보여줄 레코드의 개수
			int certCntList = Integer.parseInt(req.getParameter("row"));
			
			HttpSession ss = req.getSession();
			LogVO vo = new LogVO((certNowPage - 1) * certCntList, certCntList);
			
			String mode = req.getParameter("listMode");
			if(mode != null)
			{
				try
				{
					Integer.parseInt(mode);
				}
				catch(Exception e)
				{
					mode = "";
				}
			}
			else
			{
				mode = "";
			}
			
			vo.setMode(this.getIntToMode(mode));
			model.addAttribute("listMode", mode);
			
			if(isCheck(createId))
			{
				ss.setAttribute("searchCreateId", createId);
				vo.setCreate_id(createId);
			}
			else
			{
				ss.removeAttribute("searchCreateId");
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
			if(isCheck(func))
			{
				arr = func.split(splitCode);
				
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchFunc", func);
					vo.setLog_location_id(arr[0]);
					vo.setLog_location_group_id(arr[1]);	
				}
			}
			else
			{
				ss.removeAttribute("searchFunc");
			}
			if(isCheck(common))
			{
				arr = common.split(splitCode);
				if(!arr[0].equals(arr[1]))
				{
					ss.setAttribute("searchCommon", common);
					vo.setLog_common_id(arr[0]);
					vo.setLog_common_group_id(arr[1]);
				}
			}
			else
			{
				ss.removeAttribute("searchCommon");
			}
			
			
			Map<String, Object> cnt = logService.selectLogCnt(vo);
			int maxPage = Integer.parseInt(cnt.get("totalPageCnt").toString());
			int totalCnt = Integer.parseInt(cnt.get("totalCnt").toString());
			
			certMaxPage = maxPage > certMaxPage ? certMaxPage : maxPage;
			
			//*
			ss.setAttribute("now", certNowPage);
			ss.setAttribute("max", certMaxPage);
			ss.setAttribute("min", certMinPage);
			ss.setAttribute("row", certCntList);
			//*/
			
			model.addAttribute("funcList", logService.selectFunc(vo));
			model.addAttribute("cmmFuncList", logService.selectCommonFunc(vo));
			model.addAttribute("logList", logService.selectLog(vo));
			
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
			//lg.insertLog(Location.LogSearch, Common.Success, PermissionManagement.getUserId(req), null);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			//lg.insertLog(Location.LogSearch, Common.Fail, PermissionManagement.getUserId(req), lh.error(req, e.getMessage()));
		}
		
		return "log/log";
	}
	
	
	
	
}
