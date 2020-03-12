package com.decision.v2x.era.web;

import java.io.OutputStream;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.decision.v2x.era.VO.DeviceVO;
import com.decision.v2x.era.VO.InfraVO;
import com.decision.v2x.era.service.impl.CertificationService;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.service.impl.PermissionService;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.convert.LogHelper;
import com.decision.v2x.era.util.convert.RestApiParameter;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator;
import com.decision.v2x.era.util.log.LogGenerator.Common;
import com.decision.v2x.era.util.log.LogGenerator.Location;
import com.decision.v2x.era.util.setting.SettingManagement;
import com.decision.v2x.era.util.setting.SettingManagement.Company;

@Controller
public class InfraCertificationController 
{
	Logger logger = Logger.getLogger(InfraCertificationController.class);
	
	@Resource(name = "permissionService")
	private PermissionService permissionService;
	
	@Resource(name = "logService")
	private LogService logService;
	
	private PermissionManagement pm = null;
	private LogGenerator lg = null;
	private LogHelper lh = new LogHelper();
	
	@Resource(name = "multipartResolver")
	CommonsMultipartResolver multipartResolver;
	
	
	@Resource(name = "certificationService")
	private CertificationService certService;
	
	@PostConstruct
	public void init() throws Exception
	{
		pm = new PermissionManagement(permissionService);
		lg = new LogGenerator(logService);
	}
	
	// 인프라 인증서 목록
	@RequestMapping(value = "/cert/infra/list.do")
	public String infraList(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String strReturn = "certifications/infraList";
		int now = 1;
		int row = 8000;


		//SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//Calendar cal = new GregorianCalendar();

		//Date to = transFormat.parse(from);
		//cal.setTime(to);
		try
		{
			Location loc = Location.InfraList;
			
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
			// 로그인 또는 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) 
			{
				strReturn = "redirect:/login.do";
				
				lg.insertLog(Location.InfraList, Common.SessionExpire, "", 
						lh.list(req, 0, 0, 0, "세션이 만료되었습니다."));
			}
			else
			{
				
				RestApi api = PermissionManagement.getApi(req);
				if(api != null)
				{
					JSONObject data = RestApiParameter.infraCertList("", "", "", "", row, now);
					JSONObject result = api.infraCertList(data);

					
					if(result.get("success").equals("Y"))
					{
						JSONArray arr = (JSONArray)((JSONObject)result.get("result")).get("req_list");
						model.addAttribute("infraList", arr);
						
						result = api.infraCertCount(data);
						if(result.get("success").equals("Y"))
						{
							String cnt = ((JSONObject)result.get("result")).get("cnt").toString();
							model.addAttribute("infraTotalCnt", cnt);
							model.addAttribute("msg", Integer.parseInt(cnt) > 0 ? cnt : "항목이 없습니다.");
							
							lg.insertLog(Location.InfraList, Common.Success, PermissionManagement.getUserId(req), 
									lh.list(req, row, now, Integer.parseInt(cnt), ""));
						}
						else
						{
							model.addAttribute("msg", result.get("msg").toString());
							model.addAttribute("infraTotalCnt", 0);
							
							lg.insertLog(Location.InfraList, Common.Fail, PermissionManagement.getUserId(req), 
									lh.list(req, row, now, 0, result.get("msg").toString()));
						}
					}
					else
					{
						model.addAttribute("infraList", null);
						model.addAttribute("msg", result.get("msg").toString());
						model.addAttribute("infraTotalCnt", 0);
						
						lg.insertLog(Location.InfraList, Common.Fail, PermissionManagement.getUserId(req), 
								lh.list(req, row, now, 0, "오류: " + result.get("msg").toString()));
					}
				}	
				else
				{
					strReturn = "redirect:/login.do";
					
					lg.insertLog(Location.InfraList, Common.SessionExpire, "", 
							lh.list(req, row, now, 0, "세션이 만료되었습니다."));
				}
			}
		}
		catch(Exception e)
		{
			lg.insertLog(Location.InfraList, Common.Error, PermissionManagement.getUserId(req), 
					lh.list(req, row, now, 0, lh.error(e).toString()));
		}
		
		return strReturn;
	}
	
	// 인프라 인증서 적용
	@RequestMapping(value = "/cert/infra/apply.do", method = RequestMethod.POST, produces="application/text;charset=utf8;")
	public String applyInfra(@ModelAttribute("infraVO") InfraVO infraVO, 
			ModelMap model, HttpServletRequest req) throws Exception
	{
		String docNo = infraVO.getDocNo();

		try
		{
			Location loc = Location.InfraApply;
			
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
		
		//*
		try
		{
			if(infraVO.getConfig().equals("Z"))
			{
				RestApi api = PermissionManagement.getApi(req);
				if(api != null)
				{
					JSONObject result = api.applyCertInfra(docNo);
					if(result.get("success").equals("Y"))
					{
						lg.insertLog(Location.InfraApply, Common.Success, PermissionManagement.getUserId(req), 
								lh.infraApply(req, docNo, ""));
					}
					else
					{
						lg.insertLog(Location.InfraApply, Common.Fail, PermissionManagement.getUserId(req), 
								lh.infraApply(req, docNo, result.get("msg").toString()));
					}
				}
				else
				{
					lg.insertLog(Location.InfraList, Common.SessionExpire, "", 
							lh.infraApply(req, docNo, "세션이 만료되었습니다."));
				}
			}
			else
			{
				lg.insertLog(Location.InfraApply, Common.Fail, PermissionManagement.getUserId(req), 
						lh.infraApply(req, docNo, "비정상적인 접근이 의심됨"));
			}
		}
		catch(Exception e)
		{
			lg.insertLog(Location.InfraList, Common.Error, PermissionManagement.getUserId(req), 
					lh.infraApply(req, docNo, lh.error(e).toString()));
		}
		//*/
		return "redirect:/cert/infra/list.do";
	}


	
	
	/**
	 * 인프라 인증서 추가
	 * form target=_blank
	 * 인프라 인증서를 요청하는 작업으로 생성 및 추가하는 작업은 아니다. 인프라 인증서의 생성은 외부에서 처리가 되어야 한다.
	 * 이 기능은 CSR을 생성하는 작업이다.
	 * 
	 * */
	@RequestMapping(value = "/cert/infra/create.do", method = RequestMethod.POST)
	public String createInfra(@ModelAttribute("infraVO") InfraVO infraVO, 
			ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				return "redirect:/login.do";
			}
			
			Location loc = Location.InfraCreate;
			
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
			String strStartDate = infraVO.getStateDate();
			
			JSONObject result = new JSONObject();
			RestApi api = PermissionManagement.getApi(req);
			if(api != null)
			{
				strStartDate = RestApiParameter.localtimeToUTC(strStartDate, TimeZone.getTimeZone("Asia/Seoul"));
				String setting = certService.getInfraSetting();

				//발생효력일을 추출
				JSONParser parser = new JSONParser();				
				JSONObject data = (JSONObject)parser.parse(setting);
				JSONObject item = (JSONObject)data.get("data");
				JSONObject permission = (JSONObject)item.get("permissions");
				JSONObject validityPeriod = (JSONObject)item.get("validity_period");
				JSONObject duration = (JSONObject)validityPeriod.get("duration");
				String seStartDate = validityPeriod.get("begin").toString();
				
				//발생효력일을 변경
				setting = setting.replaceAll(seStartDate, strStartDate);
				
				//*
				result = api.infraCertCreate(setting);
				//OutputStream os = resp.getOutputStream();
				//resp.setContentType("text/html;charset=UTF-8");
				
				if(result.get("success").equals("Y"))
				{
					//os.write("<script>alert('인프라 인증서가 추가가 되었습니다.')</script>".getBytes());
					lg.insertLog(Location.InfraCreate, Common.Success, "", 
							lh.infraCreate(req, "", ""));
					
					pm.setSessionValue(req, "succMsg", "인프라 인증서가 추가가 되었습니다.");
				}
				else
				{
					//os.write(("<script>alert('오류: " + result.get("msg") + "')</script>").getBytes());
					long logSeq = lg.insertLog(Location.InfraCreate, Common.Error, "", 
							lh.infraCreate(req, "", result.get("msg").toString()));
					
					pm.setSessionValue(req, "errMsg", "인프라 인증서를 추가하지 못했습니다. 자세한 사항은 " + logSeq + "를 참조해주세요");
				}
				//*/
				
				//os.flush();
				//os.close();
			}
			else
			{
				lg.insertLog(Location.InfraCreate, Common.SessionExpire, "", 
						lh.infraCreate(req, "","세션이 만료되었습니다."));
				
				req.getSession().invalidate();
				return "redirect:/login.do";
			}
		}
		catch(Exception e)
		{
			lg.insertLog(Location.InfraCreate, Common.Error, PermissionManagement.getUserId(req), 
					lh.infraApply(req, "", lh.error(e).toString()));
		}
		
		return "redirect:/cert/infra/list.do";
	}
	
	/**
	 * 외부에서 생성한 인프라 인증서를 업로드한다.
	 * 
	 * */
	@RequestMapping(value = "/cert/infra/upload.do", method = RequestMethod.POST, produces="application/text;charset=utf8;")
	public String uploadInfra(@ModelAttribute("infraVO") InfraVO infraVO, 
			ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String docNo = infraVO.getDocNo();

		try
		{
			if(pm == null)
			{
				req.getSession().invalidate();
				return "redirect:/login.do";
			}
			
			Location loc = Location.InfraUpload;
			
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
		
		
		
		//*
		try
		{
			RestApi api = PermissionManagement.getApi(req);
			if(api != null)
			{
				MultipartFile file = infraVO.getFile();
				
				JSONObject result = api.uploadInfraCsrEx(file.getBytes(), file.getOriginalFilename(), docNo, "self");
				//JSONObject result = api.uploadInfraCsr(file.getBytes(), file.getOriginalFilename(), docNo, "self");
				
				//OutputStream os = resp.getOutputStream();
				//resp.setContentType("text/html;charset=UTF-8");
			
				if(result.get("success").equals("Y"))
				{
					//os.write("<script>alert('인프라 인증서의 업로드가 완료되었습니다.')</script>".getBytes());
					lg.insertLog(Location.InfraUpload, Common.Success, PermissionManagement.getUserId(req), 
							lh.infraUpload(req, docNo, ""));
					
					pm.setSessionValue(req, "succMsg", "인프라 인증서의 업로드가 완료되었습니다.");
				}
				else
				{
					//os.write(("<script>alert('오류: " + result.get("msg") + "')</script>").getBytes());
					long logSeq = lg.insertLog(Location.InfraUpload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.infraUpload(req, docNo, result.get("msg").toString()));
					
					pm.setSessionValue(req, "errMsg", "인프라 인증서의 업로드가 실패했습니다. 자세한 사항은 " + logSeq + "번을 확인해주세요");
				}
			}
			else
			{
				lg.insertLog(Location.InfraUpload, Common.SessionExpire, "", 
						lh.infraUpload(req, docNo, "세션이 만료되었습니다."));
			}
		}
		catch(Exception e)
		{
			long logSeq = lg.insertLog(Location.InfraUpload, Common.Error, PermissionManagement.getUserId(req), 
					lh.infraUpload(req, docNo, lh.error(e).toString()));
			
			pm.setSessionValue(req, "errMsg", "오류가 발생했습니다. 자세한 사항은 " + logSeq + "번을 확인해주세요");
		}
		
		return "redirect:/cert/infra/list.do";
		//*/
	}
	
	/**
	 * 인프라 인증서의 CSR을 다운로드하는 기능이다. 이 CSR을 사용하여 인프라 인증서를 생성할 수 있다.
	 * */
	@RequestMapping(value = "/cert/infra/download/csr.do", method = RequestMethod.POST, produces="application/text;charset=utf8;")
	public void donwloadCsrInfra(@ModelAttribute("infraVO") InfraVO infraVO, 
			ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String docNo = infraVO.getDocNo();
		
		
		try
		{
			OutputStream os = resp.getOutputStream();
			if(pm == null)
			{
				resp.setContentType("text/html;charset=UTF-8");
				os = resp.getOutputStream();
				os.write(("<script>location.href='login.do';</script>").getBytes());
				os.flush();
				os.close();
				
				return;
			}
			
			
			Location loc = Location.InfraCsrDownload;			
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
			OutputStream os = resp.getOutputStream();
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
			RestApi api = PermissionManagement.getApi(req);
			if(api != null)
			{
				JSONObject data = RestApiParameter.downloadCertInfra(docNo);
				JSONObject result = api.downloadCertInfra(data);
				if(result.get("success").equals("Y"))
				{
					OutputStream os = resp.getOutputStream();
					String fileName = result.get("fileName").toString();
					byte[] fileData = (byte[])result.get("fileData");
					String fileExt = "octet-stream";
					
					/*
					try
					{
						fileExt = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
					}
					catch(Exception e)
					{
						fileExt = "octet-stream";
					}
					//*/
					
					resp.setContentType("application/" + fileExt);
					resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
					resp.setHeader("Content-Transfer-Encoding", "binary");
					
					os.write(fileData);
					os.flush();
					os.close();
					
					lg.insertLog(Location.InfraCsrDownload, Common.Success, PermissionManagement.getUserId(req), 
							lh.infraUpload(req, docNo, ""));
				}
				else
				{
					logger.info(result);
					lg.insertLog(Location.InfraCsrDownload, Common.Fail, PermissionManagement.getUserId(req), 
							lh.infraUpload(req, docNo, result.get("msg").toString()));
				}
			}
			else
			{
				lg.insertLog(Location.InfraCsrDownload, Common.SessionExpire, "", 
						lh.infraUpload(req, docNo, "세션이 만료되었습니다."));
			}
		}
		catch(Exception e)
		{
			lg.insertLog(Location.InfraCsrDownload, Common.Error, PermissionManagement.getUserId(req), 
					lh.infraCsrDownload(req, docNo, lh.error(e).toString()));
		}
		
		//return json.toString();
	}
}
