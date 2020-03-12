package com.decision.v2x.era.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.decision.v2x.era.VO.UserVO;
import com.decision.v2x.era.service.impl.CertificationService;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.service.impl.PermissionService;
import com.decision.v2x.era.service.impl.UserService;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.convert.HashParameter;
import com.decision.v2x.era.util.convert.LogHelper;
import com.decision.v2x.era.util.convert.RestApiParameter;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator;
import com.decision.v2x.era.util.log.LogGenerator.Common;
import com.decision.v2x.era.util.log.LogGenerator.Location;
import com.decision.v2x.era.util.random.RandomGenerator;
import com.decision.v2x.era.util.setting.AccountManagement;
import com.decision.v2x.era.util.setting.SettingManagement;
import com.decision.v2x.era.util.setting.SettingManagement.Company;
import com.decision.v2x.era.util.setting.SsoAgentConfig;

import egovframework.rte.fdl.property.EgovPropertyService;

@Controller
public class LoginController 
{
	Logger logger = Logger.getLogger(LoginController.class);
	

	
	//public static RestApi api = null;
	public static SettingManagement setting = null;
	
	@Resource(name = "permissionService")
	private PermissionService permissionService;
	
	@Resource(name = "logService")
	private LogService logService;
	
	private PermissionManagement pm = null;
	private LogGenerator lg = null;
	private LogHelper lh = new LogHelper();
	
	@Resource(name = "userService")
	private UserService userService;
	
	@Resource(name = "certificationService")
	private CertificationService certificationService;
	
	@PostConstruct
	public void init() throws Exception
	{
		try
		{
			setting = new SettingManagement(Company.Test);
			RestApi.init(setting.getDcmIp());
			pm = new PermissionManagement(permissionService);
			lg = new LogGenerator(logService);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// 로그인
	@RequestMapping(value = "/login.do")
	public String login(@ModelAttribute("userVO") UserVO userVO, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String strReturn = "login/login";
		req.getSession().setAttribute("settingMgmt", setting.getCompanyName());

		try
		{
			int iResult = -1;

			// 로그인 확인
			if(req.getSession().getAttribute("user_id") != null) 
			{
				strReturn = "redirect:/main/dashboard.do";
				lg.insertLog(Location.Login, Common.SessionExpire, "", 
						lh.login(req, "세션이 만료되었습니다."));
			}
			else 
			{
				if(!userVO.getUser_id().isEmpty() && !userVO.getUser_pw().isEmpty()) 
				{
					if(userVO.getUser_id().equals(AccountManagement.getSystemId()))
					{
						//localhost
						if(AccountManagement.isLocalhost(req))
						{
							userVO.setUser_pw(new HashParameter().sha512(userVO.getUser_pw()));
							iResult = userService.loginERA(userVO);							
						}
						else
						{
							lg.insertLog(Location.Login, Common.Fail, userVO.getUser_id(), 
									lh.login(req, AccountManagement.getSystemId() + " => 외부에서 접속이 시도됨"));
						}
					}
					else
					{
						userVO.setUser_pw(new HashParameter().sha512(userVO.getUser_pw()));
						iResult = userService.loginERA(userVO);
					}
				}
					
				switch (iResult) 
				{
				case 1:
					UserVO vo = new UserVO();
					vo = userService.selectUserInfo(userVO);
					
					req.getSession().setAttribute("user_id", userVO.getUser_id());
					req.getSession().setAttribute("user_name", vo.getUser_name());
					req.getSession().setAttribute("auth_name", vo.getAuthority_name());
					
					List<Map<String, Object>> permissionUrlList = pm.permissionList(req);
					req.getSession().setAttribute("permissionUrlList", permissionUrlList);
					
					req.getSession().removeAttribute("settingMgmt");
					strReturn = "redirect:/main/dashboard.do";
					
					int resultPwdDate = userService.selectCheckPwdDate(userVO.getUser_id());
					if(resultPwdDate == 1)
					{
						//JSONObject loginResult = new JSONObject();
						//loginResult.put("user_id", userVO.getUser_id());
						//loginResult.put("user_name", vo.getUser_name());
						//loginResult.put("auth_name", vo.getAuthority_name());
						
						req.getSession().invalidate();
						req.getSession().setAttribute("change_pwd_user_id", userVO.getUser_id());
						req.getSession().setAttribute("errMsg", "비밀번호를 변경해야 이용이 가능합니다.");
						//req.getSession().setAttribute("login_result", loginResult);
						
						return "redirect:/users/changePwd.do";
					}
					
					//*
					try
					{
						
						RestApi api = new RestApi(vo.getDcm_id(), vo.getDcm_key());
						req.getSession().setAttribute("api", api);
						api.DCMLogin();
						
						
						if(userService.selectCheckKeyDate(vo.getDcm_id()) > 0)
						{
							RestApiParameter convert = new RestApiParameter();
							RandomGenerator rg = new RandomGenerator();
							String newKey = rg.getUUID();
							
							JSONObject param = convert.changeUser(vo.getDcm_id(), newKey);
							JSONObject result = api.changeUser(param);
							if(result.get("success").equals("Y"))
							{
								vo.setDcm_key(newKey);
								userService.updateUserKey(vo);
							}
						}
						
						
						logger.info("DCM Agent와 연결이 되었습니다.");
						
						lg.insertLog(Location.Login, Common.Success, userVO.getUser_id(), 
								lh.login(req, ""));
						
					}
					catch(Exception e)
					{
						logger.info("DCM Agent와 연결이 되지 않았습니다.");
						
						Long logSeq = lg.insertLog(Location.Login, Common.Error, userVO.getUser_id(), 
								lh.error(req, e.getMessage()));
						pm.setSessionValue(req, "errMsg", "DCM Agent와 연결에 실패했습니다. 자세한 사항은 " + logSeq + "번을 확인해주세요.");
					}
					//*/
				
					break;
					
				case 0:
					model.addAttribute("loginMessage", "0");		// 가입하지 않은 아이디입니다.
					lg.insertLog(Location.Login, Common.Fail, userVO.getUser_id(), 
							lh.login(req, "가입하지 않은 아이디입니다."));
					break;
					//최초 로그인 창 
				case -1:
					break;
					
				default :
					model.addAttribute("loginMessage", "2");		// DB 오류	
					lg.insertLog(Location.Login, Common.Fail, userVO.getUser_id(), 
							lh.login(req, "DBMS 오류"));
					break;
				}
			}
			
			
		}
		catch(Exception e)
		{
			try
			{
				lg.insertLog(Location.Login, Common.Error, "", 
						lh.login(req, lh.error(e).toString())
						);
			}
			catch(Exception e2)
			{
				logger.error("DB 접속에 실패하였습니다. DB 접속정보를 확인해주세요");
				logger.error(e2.getMessage());
			}
		}
		
		return strReturn;
	}
	
	// 로그아웃
	@RequestMapping(value = "/logout.do")
	public String logout(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		// 로그아웃 로그 기록
		Object userId = null;
		try
		{
			if(pm == null)
			{
				return "redirect:/login.do";
			}
			
			userId = PermissionManagement.getUserId(req);
			req.getSession().removeAttribute("user_id");
			req.getSession().removeAttribute("user_name");
			req.getSession().removeAttribute("permissionUrlList");
			
			lg.insertLog(Location.Logout, Common.Success, userId == null ? "" : userId.toString(), 
					lh.logout(req, ""));
		}
		catch(Exception e)
		{
			lg.insertLog(Location.Logout, Common.Success, userId == null ? "" : userId.toString(), 
					lh.logout(req, lh.error(e).toString())
					);
		}
		
		req.getSession().invalidate();
		return "redirect:/login.do";
	}
	
	// dashboard
	@RequestMapping(value = "/main/dashboard.do")
	public String dashboard(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String strReturn = "dashboard/dashboard";

		try
		{
			// 로그인 또는 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) 
			{
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.MainDashboard, Common.SessionExpire, "", 
						lh.dashboard(req, "세션이 만료되었습니다."));
			}
			else 
			{
				Calendar cal = Calendar.getInstance();
				int thisYear = cal.get(Calendar.YEAR);
				List<?> certCountList = certificationService.certCountYear(thisYear);
				
				model.addAttribute("certCountList", certCountList);
				lg.insertLog(Location.MainDashboard, Common.Success, PermissionManagement.getUserId(req), 
						lh.dashboard(req, ""));
			}
			
			
		}
		catch(Exception e)
		{
			lg.insertLog(Location.MainDashboard, Common.Error, PermissionManagement.getUserId(req), 
					lh.dashboard(req, LogHelper.error(e).toString()));
		}

		return strReturn;
	}
	
	
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertyService;
	
	@ResponseBody
	@RequestMapping(value = "/sso/CheckAuth.do")
	public String  ssoCheckAuth(ModelMap model, HttpServletRequest request, HttpServletResponse resp) throws Exception
	{
		SsoAgentConfig ssoAgentConfigVO = new SsoAgentConfig(this.propertyService);
		String resultCode = request.getParameter("resultCode") == null ? "" : request.getParameter("resultCode");
        String secureToken = request.getParameter("secureToken") == null ? "" : request.getParameter("secureToken");
        String secureSessionId = request.getParameter("secureSessionId") == null ? "" : request.getParameter("secureSessionId");
        String clientIp = request.getRemoteAddr();
        String resultMessage = "";
        String returnUrl = "";
        /*
        // debug print
        Map<String, String[]> paramMap = request.getParameterMap();
        Set<String> setKey = paramMap.keySet();
        for(String key : setKey)
        {
        	String retval = "";
        	String[] arr = paramMap.get(key);
        	for(int i = 0; i<arr.length; i++)
        	{
        		retval = arr[i] + ", ";
        	}
        	
        	System.out.println(key + " => " + retval);
        }
        */
        /*
        Map<String, String[]> paramMap = request.getParameterMap();
        Set<String> keys = paramMap.keySet();
        for(String key : keys)
        {
        	String retval = "";
        	String[] arr = paramMap.get(key);
        	for(int i = 0; i<arr.length; i++)
        	{
        		retval = arr[i] + ", ";
        	}
        	
        	logger.debug(key + " => " + retval);
        }
        */
        //*
        logger.info("[resultCode] : " + resultCode);
        logger.info("[secureToken] : " + secureToken);
        logger.info("[secureSessionId] : " + secureSessionId);
        logger.info("[clientIp] : " + clientIp);
        //*/

        if (resultCode.equals("000000") && "".equals(secureToken) == false && "".equals(resultCode) == false) 
        {
            
        	HttpPost httpPost = null;
            CloseableHttpClient httpClient = HttpClients.createDefault();
            
            try 
            {
            	
            	// 인증서버에 토큰 검증 및 사용자 정보를 요청하기 위해 httpclient를 사용하여 전달
            	httpPost = new HttpPost(ssoAgentConfigVO.getAuthSslUrl() + ssoAgentConfigVO.getTokenAuthUrl()); 

				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("secureToken", secureToken));
				params.add(new BasicNameValuePair("secureSessionId", secureSessionId));
				params.add(new BasicNameValuePair("requestData", ssoAgentConfigVO.getRequestData()));
				params.add(new BasicNameValuePair("agentId", ssoAgentConfigVO.getAgentId()));
				params.add(new BasicNameValuePair("clientIP", clientIp));
				httpPost.setEntity(new UrlEncodedFormEntity(params));
    	      
				CloseableHttpResponse postResponse = httpClient.execute(httpPost);

				String httpResponse = "";
				try 
				{
					BufferedReader rd = new BufferedReader(new InputStreamReader(postResponse.getEntity().getContent(), "UTF-8"));  
					httpResponse = rd.readLine();
				} 
				catch(IllegalArgumentException argEx) 
				{
					logger.error("[Bad Response on Token Authorization] : " + argEx.toString());
				} 
				catch(Exception e)
				{
					logger.error("[Exception on Token Authorization] : " + e.toString());
				}
				finally 
				{
					postResponse.close();
				}

				logger.info("httpResponse : " + httpResponse);
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(httpResponse);
				
				// 사용자 요청 정보
				JSONObject dataObject = (JSONObject) jsonObject.get("user");
				  
				// 결과 코드와 메시지
				resultCode = (String) jsonObject.get("resultCode");
				resultMessage = (String) jsonObject.get("resultMessage");
				  
				// Return URL(인증서버에서 리다이렉션될 주소를 전달)
				returnUrl = (String) jsonObject.get("returnUrl");
				logger.info("returnUrl : " + returnUrl);  
				
				// check cs mode(토큰저장소에 토큰을 저장하기 위해 사용되며 CS모드일 경우는 SAVE_TOKEN_URL로 리다이렉션 됨)
				boolean useCSMode  = jsonObject.get("useCSMode") == null 
				  ? false:Boolean.valueOf(jsonObject.get("useCSMode").toString());
				boolean useNotesMode = jsonObject.get("useNotesMode") == null 
				  ? false: Boolean.valueOf(jsonObject.get("useNotesMode").toString());
				
				HttpSession session = request.getSession();
				
                // 요청 데이터 정보 추출
                if ("000000".equals(resultCode)) 
                {
                    // 검증 성공
                    String[] keys = ssoAgentConfigVO.getRequestData().split(",");

                    for (int i = 0; i < keys.length; i++) 
                    {
                        String value = (String) dataObject.get(keys[i]);
                        if (value == null) 
                        {
                            continue;
                        }

                        logger.info(keys[i] + " : " + value);
                        session.setAttribute(keys[i], value);
                    }
  
                    returnUrl = ssoAgentConfigVO.getSaveTokenUrl();             
                  
                } 
                else if ("310017".equals(resultCode) || "310012".equals(resultCode)) 
                {
                    // 서비스 접근 권한 실패(다른 서비스에 영향을 주어서는 안됨으로 로그아웃은 하지 않음)
                    returnUrl = ssoAgentConfigVO.getServiceErrorPage();
                } 
                else 
                {
                    // SSO 검증 실패(로그아웃 필요)
                    returnUrl = ssoAgentConfigVO.getErrorPage();
                }
              
                logger.info("resultCode : " + resultCode);
                logger.info("resultMessage : " + resultMessage);

                // 결과 코드와 메시지, 사용자 요청 데이터를 세션에 저장
                session.setAttribute("resultCode", resultCode);
                session.setAttribute("resultMessage", resultMessage);
            } 
            catch(ConnectTimeoutException timeOutEx) 
            {
        	    logger.error("[Connection Time Out Exception] : " + timeOutEx.toString());

        	    // TODO - 인증서버와 통신 실패 시 개별 업무로 로그인 할 수 있도록 처리 해야 합니다.
        	    returnUrl = ssoAgentConfigVO.getExceptionPage();
        	  
            } 
            catch (ClientProtocolException clientEx) 
            {
        	    logger.error("[HttpException] : " + clientEx.toString());

                // TODO - 인증서버와 통신 실패 시 개별 업무로 로그인 할 수 있도록 처리 해야 합니다.
                returnUrl = ssoAgentConfigVO.getExceptionPage();
            } 
            catch (Exception e) 
            {
        	    logger.error("[Undefined Exception] : " + e.toString());

                returnUrl = ssoAgentConfigVO.getErrorPage();
            }
            finally
            {
            	try 
            	{
            		httpPost.releaseConnection();
            	} 
            	catch(Exception e) 
            	{
            		logger.error("[Release Connection Fail] : " + e.toString());
            	}
            }

        } 
        else 
        {
            // 비정상 호출 할 경우 Business 페이지로 리다이렉션 처리
        	logger.error("[Unknown Call so Redirect to Business]");
            returnUrl = ssoAgentConfigVO.getLogOutPage();
        }

        logger.info("returnUrl : " + returnUrl);

		return "Test";
	}


	//제거되어야 한다.
	@ResponseBody
	@RequestMapping(value = "/sso/main.do")
	public String  ssoMain(ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String retval = "<!DOCTYPE html>" +
				"<html>" +
					"<head>" +
				
					"</head>" +
				
					"<body>";
		
		HttpSession ss = req.getSession();
		Enumeration<String> keys = ss.getAttributeNames();
		while(keys.hasMoreElements())
		{
			String key = keys.nextElement();
			
			retval += key + " => " + ss.getAttribute(key) + "<br>";
		}
		
		
		
		retval +=
					
					"</body>" +
				"</html>";
		
		
		
		return "OK";
	}
}
