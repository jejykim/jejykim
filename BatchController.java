package com.decision.v2x.era.web;

import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.decision.v2x.era.service.impl.CertificationService;
import com.decision.v2x.era.service.impl.DeviceService;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.service.impl.PermissionService;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.convert.ExcelHelper;
import com.decision.v2x.era.util.convert.LogHelper;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator;
import com.decision.v2x.era.util.log.LogGenerator.Location;

@Controller
public class BatchController {

	@Resource(name = "permissionService")
	private PermissionService permissionService;
	
	@Resource(name = "logService")
	private LogService logService;
	
	/** DeviceService */
	@Resource(name = "deviceService")
	private DeviceService deviceService;
	
	/** CertificationService */
	@Resource(name = "certificationService")
	private CertificationService certService;
	
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

	@ResponseBody
	@RequestMapping(value = "/batch/upload.do")
	public String batchUpload(@RequestParam("inputExcelFile") MultipartFile mfile , HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model) throws Exception 
	{
		JSONObject sendResult = new JSONObject();
		
		try
		{
			Location loc = Location.DeviceBatch;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult)
			{
				sendResult.put("success", "N");
				sendResult.put("errMsg", PermissionManagement.getMsg(perResult));
				sendResult.put("redirectUrl", PermissionManagement.getRedirectUrl(perResult).replace("redirect:", ""));
				return sendResult.toString();
			}
		}
		catch(Exception e)
		{
			//DB오류
			e.printStackTrace();
			
			sendResult.put("success", "N");
			sendResult.put("errMsg", "DB 접속 정보를 확인해주세요");
			sendResult.put("redirectUrl", "/login.do");
			return sendResult.toString();
		}
		
		
		
		JSONObject json = new JSONObject();
		ExcelHelper eh = new ExcelHelper(permissionService, certService, logService, deviceService);
		
		boolean flag = false;
		try {
			eh.batchUpload(mfile, req, resp);
			flag = true;
		}catch (Exception e) {
			System.out.println("배치 업로드 실패");
		}
		
		json.put("flag", flag);
		
		return json.toString();
	}
}
