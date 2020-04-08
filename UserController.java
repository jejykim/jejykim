package com.decision.v2x.dcm.web;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.decision.v2x.dcm.VO.DeviceVO;
import com.decision.v2x.dcm.VO.PagingVO;
import com.decision.v2x.dcm.VO.PermissionVO;
import com.decision.v2x.dcm.VO.UserVO;
import com.decision.v2x.dcm.service.impl.CodeService;
import com.decision.v2x.dcm.service.impl.LogService;
import com.decision.v2x.dcm.service.impl.PermissionService;
import com.decision.v2x.dcm.service.impl.UserService;
import com.decision.v2x.dcm.util.auth.PermissionManagement;
import com.decision.v2x.dcm.util.convert.HashParameter;
import com.decision.v2x.dcm.util.convert.LogHelper;
import com.decision.v2x.dcm.util.http.RestApi;
import com.decision.v2x.dcm.util.log.LogGenerator;
import com.decision.v2x.dcm.util.log.LogGenerator.Common;
import com.decision.v2x.dcm.util.log.LogGenerator.Location;
import com.decision.v2x.dcm.util.paging.PagingControl;
import com.decision.v2x.dcm.util.random.RandomGenerator;
import com.decision.v2x.dcm.util.setting.AccountManagement;
import com.decision.v2x.dcm.util.setting.SettingManagement;

@Controller
public class UserController {

	@Resource(name = "userService")
	private UserService userService;
	
	@Resource(name = "codeService")
	private CodeService codeService;
	
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
	
	// 목록
	@RequestMapping(value = {"/users/list.do", "/users/search.do"})
	public String userList(@ModelAttribute("userVO") UserVO userVO, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String strReturn = "users/userList";
		int userListCount = 0;
		
		try {
			Location loc = Location.DeviceChange;
			String requestUri = req.getRequestURI();
			if(requestUri.indexOf("users/list") != -1) {
				loc = Location.UserList;
			}else {
				loc = Location.UserSearch;
			}
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult) {
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}catch(Exception e) {
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "세션이 만료 되었습니다.");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		try {
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) {
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.UserList, Common.SessionExpire, PermissionManagement.getUserId(req), lh.list(req, 0, 0, 0, "세션이 만료됨"));
			}else {
				if(userVO.getNow_page() != 1) {
					userVO.setPage((userVO.getNow_page()-1)*userVO.getPage_per_data());
				}
				
				List<?> userList = userService.selectUserList(userVO);
				userListCount = userService.selectUserListCount(userVO);
				int adminListCount = userService.selectAdminListCount();
				int normalUserListCount = userService.selectNormalUserListCount();
				
				PagingControl pc = new PagingControl();
				PagingVO pagingVO = pc.paging(userListCount, userVO.getNow_page(), userVO.getPage_per_data());
				
				model.addAttribute("userList", userList);
				model.addAttribute("myID", req.getSession().getAttribute("user_id"));
				model.addAttribute("userListCount", userListCount);
				model.addAttribute("adminListCount", adminListCount);
				model.addAttribute("normalUserListCount", normalUserListCount);
				model.addAttribute("userVO", userVO);
				model.addAttribute("searchStartDate", userVO.getStart_time());
				model.addAttribute("searchEndDate", userVO.getEnd_time());
				model.addAttribute("pagingVO", pagingVO);
				
				if(req.getRequestURI().indexOf("users/list") != -1) {
					lg.insertLog(Location.UserList, Common.Success, PermissionManagement.getUserId(req), 
							lh.list(req, pagingVO.getPage_per_rows(), pagingVO.getNow_page(), pagingVO.getTotal_count(), ""));
				}else {
					lg.insertLog(Location.UserList, Common.Success, PermissionManagement.getUserId(req), 
							lh.userSearch(req, pagingVO.getPage_per_rows(), pagingVO.getNow_page(), pagingVO.getTotal_count(), 
									userVO.getUser_id(), userVO.getUser_name(), userVO.getStart_time(), userVO.getEnd_time(), ""));

				}
			}
		}catch(Exception e) {
			if(req.getRequestURI().indexOf("users/list") != -1) {
				lg.insertLog(Location.UserList, Common.Fail, PermissionManagement.getUserId(req), 
						lh.list(req, userVO.getPage_per_data(), userVO.getNow_page(), userListCount, "페이지 이동 실패 !!" + LogHelper.error(e).toString()));
			}else {
				lg.insertLog(Location.UserList, Common.Fail, PermissionManagement.getUserId(req), 
						lh.userSearch(req, userVO.getPage_per_data(), userVO.getNow_page(), userListCount, 
								userVO.getUser_id(), userVO.getUser_name(), userVO.getStart_time(), userVO.getEnd_time(), "페이지 이동 실패 !!" + LogHelper.error(e).toString()));

			}
			
		}
		
		return strReturn;
	}

	// 추가
	@RequestMapping(value = "/users/add.do")
	public String addUser(HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model) throws Exception {
		String strReturn = "users/addUser";
		
		try {
			Location loc = Location.UserAdd;
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult) {
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}catch(Exception e) {
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "세션이 만료 되었습니다.");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		try {
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) {
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.UserAdd, Common.SessionExpire, PermissionManagement.getUserId(req), lh.userAdd(req, "", "", "", "", "", "세션 만료"));
			}else {
				List<?> authList = codeService.getAuthList();
				
				model.addAttribute("authList", authList);
				
				lg.insertLog(Location.UserAdd, Common.Success, PermissionManagement.getUserId(req), lh.userAdd(req, "", "", "", "", "", ""));
			}
		}catch(Exception e) {
			e.printStackTrace();
			lg.insertLog(Location.UserAdd, Common.Fail, PermissionManagement.getUserId(req), lh.userAdd(req, "", "", "", "", "", "페이지 이동 실패 !!" + LogHelper.error(e).toString()));
		}
		
		return strReturn;
	}
	
	// 추가 확인
	@RequestMapping(value = "/users/addOK.do")
	public String addUserOK(@ModelAttribute("userVO") UserVO userVO, HttpServletRequest req, HttpServletResponse resp, ModelMap model) throws Exception {
		String strReturn = "redirect:/users/list.do";
		String userAuthSplit = userVO.getAuthority_id();
		
		try {
			Location loc = Location.UserAddOk;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult) {
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}catch(Exception e) {
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "세션이 만료 되었습니다.");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		try {
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) {
				strReturn = "redirect:/login.do";
				lg.insertLog(Location.UserAddOk, Common.SessionExpire, PermissionManagement.getUserId(req), 
						lh.userAdd(req, userVO.getUser_id(), userVO.getUser_name(), 
								userVO.getAuthority_id(), userVO.getDepart(), userVO.getPosition(), ""));
			}else {
				RandomGenerator rg = new RandomGenerator();
				String dcmId = rg.getID(userVO.getUser_id());
				String dcmEmail = rg.getHostname(userVO.getUser_id());
				
				String pwd = userVO.getUser_pw();
				CheckPwd result = this.checkPwd(pwd);
				if(result != CheckPwd.Success) {
					return "redirect:/logout.do";
				}
				
				dcmId = rg.Shuffle(dcmId);
				if(dcmEmail == "") {
					dcmEmail = "scms.kr";
				}
				
				String resultId = dcmId + "@" + dcmEmail;
				String resultPw = rg.getUUID();
				
				
				RestApi api = PermissionManagement.getApi(req);
				if(api == null) {
					strReturn = "redirect:/login.do";
					lg.insertLog(Location.UserAddOk, Common.SessionExpire, PermissionManagement.getUserId(req), 
							lh.userAdd(req, userVO.getUser_id(), userVO.getUser_name(), 
									userVO.getAuthority_id(), userVO.getDepart(), userVO.getPosition(), "API is NULL"));
				}else {
					JSONObject apiResult = api.insertUser(resultId, resultPw);
					if(apiResult.get("success").equals("Y")) {
						apiResult = api.insertAuth(resultId);
						
						if(apiResult.get("success").equals("Y")) {
							String[] userAuth = userAuthSplit.split(":");
							userVO.setAuthority_id(userAuth[0]);
							userVO.setAuthority_id_group_id(userAuth[1]);						
							userVO.setUser_pw(new HashParameter().sha512(userVO.getUser_pw()));						
							userVO.setCreate_id(req.getSession().getAttribute("user_id").toString());
							userVO.setEra_id(resultId);
							userVO.setEra_key(resultPw);
							
							int iResult = userService.insertUser(userVO);
							
							if(iResult > 0) {
								lg.insertLog(Location.UserAddOk, Common.Success, PermissionManagement.getUserId(req), 
										lh.userAdd(req, userVO.getUser_id(), userVO.getUser_name(), 
												userAuthSplit, userVO.getDepart(), userVO.getPosition(), "[ "+userVO.getUser_id()+" ] 계정 추가"));
							}else {
								strReturn = "redirect:/users/add.do";
								
								lg.insertLog(Location.UserAddOk, Common.Fail, PermissionManagement.getUserId(req), 
										lh.userAdd(req, userVO.getUser_id(), userVO.getUser_name(), 
												userAuthSplit, userVO.getDepart(), userVO.getPosition(), "[ "+userVO.getUser_id()+" ] 계정 추가 실패"));
							}
						}else {
							api.deleteUser(resultId);
							
							strReturn = "redirect:/users/add.do";
							lg.insertLog(Location.UserAddOk, Common.Fail, PermissionManagement.getUserId(req), 
									lh.userAdd(req, userVO.getUser_id(), userVO.getUser_name(), 
											userAuthSplit, userVO.getDepart(), userVO.getPosition(), "[ "+userVO.getUser_id()+" ] 계정 추가 실패 !!"+apiResult.get("msg").toString()));
						}
					}else {
						strReturn = "redirect:/users/add.do";
						lg.insertLog(Location.UserAddOk, Common.Fail, PermissionManagement.getUserId(req), 
								lh.userAdd(req, userVO.getUser_id(), userVO.getUser_name(), 
										userAuthSplit, userVO.getDepart(), userVO.getPosition(), "[ "+userVO.getUser_id()+" ] 계정 추가 실패 !!"+apiResult.get("msg").toString()));
					}
				}
			}
		}catch(Exception e) {
			lg.insertLog(Location.UserAddOk, Common.Fail, PermissionManagement.getUserId(req), 
					lh.userAdd(req, userVO.getUser_id(), userVO.getUser_name(), 
							userAuthSplit, userVO.getDepart(), userVO.getPosition(), "[ "+userVO.getUser_id()+" ] 계정 추가 실패 !!" + LogHelper.error(e).toString()));
		
		}
		
		return strReturn;
	}
	
	// 상세
	@RequestMapping(value = {"/users/show.do", "/users/myInfo.do"})
	public String userInfo(@ModelAttribute("userVO") UserVO userVO, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model) throws Exception {
		String strReturn = "users/userInfo";
		
		try {
			Location loc = Location.UserDetail;
			
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult) {
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}catch(Exception e) {
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "세션이 만료 되었습니다.");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		try {
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) {
				strReturn = "redirect:/login.do";
				
				lg.insertLog(Location.UserDetail, Common.SessionExpire, PermissionManagement.getUserId(req), 
						lh.userDetail(req, userVO.getUser_id(), "세션이 만료됨"));
			}else {
				if(userVO.getUser_id().isEmpty()) {
					Map<String, ?> map = RequestContextUtils.getInputFlashMap(req);
					
					if(map != null) {
						userVO.setUser_id((String) map.get("user_id"));
						
						UserVO vo = userService.selectUserInfo(userVO);
						List<?> authList = codeService.getAuthList();
						
						model.addAttribute("authList", authList);
						model.addAttribute("userVO", vo);
						
						lg.insertLog(Location.UserDetail, Common.Success, PermissionManagement.getUserId(req), 
								lh.userDetail(req, userVO.getUser_id(), ""));
					}else if(req.getRequestURI().indexOf("myInfo.do") > 0) {
						userVO.setUser_id(req.getSession().getAttribute("user_id").toString());
						
						UserVO vo = userService.selectUserInfo(userVO);
						List<?> authList = codeService.getAuthList();
						
						model.addAttribute("authList", authList);
						model.addAttribute("userVO", vo);
						
						lg.insertLog(Location.UserDetail, Common.Success, PermissionManagement.getUserId(req), 
								lh.userDetail(req, userVO.getUser_id(), ""));
					}else {
						strReturn = "redirect:/users/list.do";
						
						lg.insertLog(Location.UserDetail, Common.Fail, PermissionManagement.getUserId(req), 
								lh.userDetail(req, userVO.getUser_id(), "페이지 이동 실패"));
					}
				}else {
					UserVO vo = userService.selectUserInfo(userVO);
					List<?> authList = codeService.getAuthList();
					
					model.addAttribute("authList", authList);
					model.addAttribute("userVO", vo);
					
					lg.insertLog(Location.UserDetail, Common.Success, PermissionManagement.getUserId(req), 
							lh.userDetail(req, userVO.getUser_id(), ""));
				}
			}
		}catch(Exception e) {
			lg.insertLog(Location.UserDetail, Common.Fail, PermissionManagement.getUserId(req), 
					lh.userDetail(req, userVO.getUser_id(), "페이지 이동 실패 !!"+lh.error(e).toString()));
		}
		
		return strReturn;
	}
	
	// 사용자 삭제
	@RequestMapping(value = "/users/delete.do")
	public String deleteUser(@RequestParam("user_id") String user_id, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model) throws Exception {
		String strReturn = "redirect:/users/list.do";
		
		try {
			Location loc = Location.UserDelete;
			LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
			if(LogGenerator.Common.Success != perResult) {
				PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
				return PermissionManagement.getRedirectUrl(perResult);
			}
		}catch(Exception e) {
			//DB오류
			PermissionManagement.setSessionValue(req, "errMsg", "세션이 만료 되었습니다.");
			e.printStackTrace();
			return "redirect:/login.do";
		}
		
		try {
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) {
				strReturn = "redirect:/login.do";
				
				lg.insertLog(Location.UserDelete, Common.SessionExpire, PermissionManagement.getUserId(req), 
						lh.userDelete(req, user_id, "세션이 만료됨"));
			}else {
				RestApi api = PermissionManagement.getApi(req);
				
				if(api != null) {
					String[] arrUser_id = user_id.split(",");
					
					for(String str : arrUser_id) {
						
						if(str.equals(AccountManagement.getSystemId())) {
							lg.insertLog(Location.UserDelete, Common.Fail, PermissionManagement.getUserId(req), 
									lh.userDelete(req, str, "System 계정은 삭제될 수 없습니다."));
						}else {
							UserVO dcmIDVO = new UserVO();
							dcmIDVO.setUser_id(str);
							
							dcmIDVO = userService.selectUserInfo(dcmIDVO);
							
							JSONObject apiResult = api.deleteAuth(dcmIDVO.getEra_id());
							
							if(apiResult.get("success").equals("Y")) {
								JSONObject result = api.deleteUser(dcmIDVO.getEra_id());
								
								if(result.get("success").equals("Y")) {
									String strResult = userService.deleteUser(str);
									
									lg.insertLog(Location.UserDelete, Common.Success, PermissionManagement.getUserId(req), 
											lh.userDelete(req, str, "[ "+str+" ] 계정 삭제"));
								}else {
									lg.insertLog(Location.UserDelete, Common.Fail, PermissionManagement.getUserId(req), 
											lh.userDelete(req, str, "[ "+str+" ] 계정 삭제 실패 !!"+result.get("msg").toString()));
								}
								
							}else {
								lg.insertLog(Location.UserDelete, Common.Fail, PermissionManagement.getUserId(req), 
										lh.userDelete(req, str, "[ "+str+" ] 계정 삭제 실패 !!"+apiResult.get("msg").toString()));
							}
						}
					}
				}else {
					lg.insertLog(Location.UserDelete, Common.SessionExpire, PermissionManagement.getUserId(req), 
							lh.userDelete(req, user_id, "세션이 만료됨"));
				}
			}
		}catch(Exception e) {
			lg.insertLog(Location.UserDelete, Common.Fail, PermissionManagement.getUserId(req), 
					lh.userDelete(req, user_id, "계정 삭제 실패 !!"+lh.error(e).toString()));
		}
		
		return strReturn;
	}
	
	// 사용자 수정
	@RequestMapping(value = "/users/change.do")
	public String modifyUser(@ModelAttribute("userVO") UserVO userVO, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model, RedirectAttributes redirectAttributes) throws Exception {
		String strReturn = "redirect:/users/show.do";
		redirectAttributes.addAttribute("user_id", userVO.getUser_id());
		
		try {
			// 로그인, 권한 확인 class 호출
			if(req.getSession().getAttribute("user_id") == null) {
				strReturn = "redirect:/login.do";
				
				lg.insertLog(Location.UserChange, Common.SessionExpire, "", 
						lh.userChange(req, userVO.getUser_id(), "", "세션이 만료됨"));
			}else {
				if(req.getSession().getAttribute("user_id").equals(userVO.getUser_id())) {
					//자기 자신의 정보를 변경하려고 한다.
				}else {
					//권한 검사
					try {
						Location loc = Location.UserChange;
						
						LogGenerator.Common perResult = pm.isPermission(req, lg, lh, loc);
						if(LogGenerator.Common.Success != perResult) {
							PermissionManagement.setSessionValue(req, "errMsg", PermissionManagement.getMsg(perResult));
							return PermissionManagement.getRedirectUrl(perResult);
						}
					}catch(Exception e) {
						//DB오류
						return "redirect:/login.do";
					}
				}
				
				if(userVO.getUser_id().equals(AccountManagement.getSystemId())) {
					lg.insertLog(Location.UserChange, Common.Fail, PermissionManagement.getUserId(req), 
							lh.userChange(req, userVO.getUser_id(), "", "System 계정은 수정될 수 없습니다."));	
				}else {
					PermissionVO perVO = new PermissionVO(userVO.getUser_id());
					String[] userAuth = userVO.getAuthority_id().split(":");
					
					HashMap<String, Object> userPerInfo = permissionService.selectUser(perVO);
					
					if(userPerInfo.get("authority_id").equals(userAuth[0]) && userPerInfo.get("authority_id_group_id").equals(userAuth[1])) {
						userVO.setAuthority_id("");
						userVO.setAuthority_id_group_id("");
					}else {
						userVO.setAuthority_id(userAuth[0]);
						userVO.setAuthority_id_group_id(userAuth[1]);
					}
				
					String pw = "";
					if(!userVO.getUser_pw().isEmpty()) {
						pw = (new HashParameter()).sha512(userVO.getUser_pw());
						userVO.setUser_pw(pw);
					}
		
					int iResult = userService.updateUser(userVO);
					
					if(iResult > 0) {
						
						lg.insertLog(Location.UserChange, Common.Success, PermissionManagement.getUserId(req), 
								lh.userChange(req, userVO.getUser_id(), "", ""));
						
						// 비밀번호 변경 + 권한 변경
						if(!userVO.getUser_pw().isEmpty() && !userVO.getAuthority_id().isEmpty()) {
							lg.insertLog(Location.UserChange, Common.Success, PermissionManagement.getUserId(req), 
									lh.userChange(req, userVO.getUser_id(), pw, "계정 상세수정, 비밀번호 변경, 권한 변경"));
						
						// 비밀번호 변경
						}else if(!userVO.getUser_pw().isEmpty()) {
							lg.insertLog(Location.UserChange, Common.Success, PermissionManagement.getUserId(req), 
									lh.userChange(req, userVO.getUser_id(), pw, "계정 상세수정, 비밀번호 변경"));
							
						// 권한 변경
						}else if(!userVO.getAuthority_id().isEmpty()) {
							lg.insertLog(Location.UserChange, Common.Success, PermissionManagement.getUserId(req), 
									lh.userChange(req, userVO.getUser_id(), 
											userPerInfo.get("authority_id").toString(), userPerInfo.get("authority_id_group_id").toString(),
											userAuth[0], userAuth[1], "계정 상세수정, 권한 변경"));	
						}else {
							lg.insertLog(Location.UserChange, Common.Success, PermissionManagement.getUserId(req), 
									lh.userChange(req, userVO.getUser_id(), "", "계정 상세수정"));
						}
						
					}else {
						lg.insertLog(Location.UserChange, Common.Fail, PermissionManagement.getUserId(req), 
								lh.userChange(req, userVO.getUser_id(), "", "계정 수정 실패"));	
					}
				}
			}
		}catch(Exception e) {
			lg.insertLog(Location.UserChange, Common.Fail, PermissionManagement.getUserId(req), 
					lh.userChange(req, userVO.getUser_id(), "", "계정 수정 실패 !!"+lh.error(e).toString()));	
		}
		
		return strReturn;
	}
	
	public enum CheckPwd {
		Success, WhileWord, UpperWord, LowerWord, SpWord, EasyWord, LineWord, Length
	}
	
	public CheckPwd checkPwd(String pwd) {
		if(pwd.matches("(.)\\1{1,}")) {
			return CheckPwd.WhileWord;
		}

		if(!pwd.matches("(.*[A-Z]{1,}.*){2,}")) {
			return CheckPwd.UpperWord;
		}
		
		if(!(pwd.matches("(.*[a-z]{1,}.*){2,}"))) {
			return CheckPwd.LowerWord;
		}
		
		if(!(pwd.matches("(.*[\\~\\`\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\_\\=\\+]{1,}.*){2,}"))) {
			return CheckPwd.SpWord;
		}
		
		String[] knowWord = {"P@ssw0rd", "1q2w3e", "1q2w3e4r", "password", "pwd", "pw", "admin", "administrator", "root", "system"};
		
		for(int i = 0; i<knowWord.length; i++) {
			
			if(pwd.indexOf(knowWord[i]) != -1) {
				//alert('파악하기 쉬운 문자열 또는 잘 알려진 단어가 포함되어 있으면 안됩니다.');
				//return false;
				return CheckPwd.EasyWord;
			}
		}
		
		String[] whileWord = {"qw", "we", "er", "rt", "ty", "yu", "ui", "io", "op", 
				"as", "sd", "df", "fg", "gh", "hj", "jk", "kl",
				"zx", "xc", "cv", "vb", "bn", "nm",
				"wq", "ew", "re", "tr", "yt", "uy", "iu", "oi", "po",
				"sa", "ds", "fd", "gf", "hg", "jh", "kj", "lk",
				"xz", "cx", "vc", "bv", "nb", "mn"};
		
		for(int i = 0; i<whileWord.length; i++) {
			if(pwd.indexOf(whileWord[i]) != -1) {
				return CheckPwd.LineWord;
			}
		}
		
		if(pwd.length() < 10) {
			return CheckPwd.Length;
		}
		
		return CheckPwd.Success;
	}
	
	@RequestMapping(value = "/users/changePwd.do")
	public String changePwd(@ModelAttribute("userVO") UserVO userVO, HttpServletRequest req, HttpServletResponse resp, Locale locale, ModelMap model, RedirectAttributes redirectAttributes) throws Exception 
	{
		String strReturn = "users/changePwd";
		
		String userId = (String) req.getSession().getAttribute("change_pwd_user_id");
		if(userId == null) {
			req.getSession().invalidate();
			return "redirect:/login.do";
		}
		
		try {
			if(userVO.getUser_pw().length() > 0 && req.getMethod().toLowerCase().equals("post")) {
				String pwd = userVO.getUser_pw();
				CheckPwd result = this.checkPwd(pwd);				
				String error = null;
				
				switch(result) {
					case Success:						
						break;
						
					case WhileWord:
						error = "반복되는 문자열이 있습니다.";
						break;
						
					case UpperWord:
						error = "대문자 2개이상 입력되어야 합니다.";
						break;
						
					case LowerWord:
						error = "소문자 2개이상 입력되어야 합니다.";
						break;
						
					case SpWord:
						error = "특수문자 2개이상 입력되어야 합니다.";
						break;
						
					case EasyWord:
						error = "너무 쉬운 문자로 구성되어 있습니다.";
						break;
						
					case LineWord:
						error = "키보드 상에서 나란히 있는 문자가 없어야 합니다.";
						break;
						
					case Length:
						error = "문자의 길이는 10자 이상이어야 합니다.";
						break;
				}
				
				if(result != CheckPwd.Success) {
					req.getSession().setAttribute("errMsg", error);
					req.getSession().setAttribute("success", "N");
					return strReturn;
				}
				
				//기존 비밀번호와 비교
				String shwUserPw = (new HashParameter()).sha512(pwd);				
				int beforePwdCnt = this.userService.selectCheckPwd(userId, shwUserPw);
				if(beforePwdCnt == 1) {
					req.getSession().setAttribute("errMsg", "기존에 사용하던 비밀번호로 변경할 수 없습니다.");
					req.getSession().setAttribute("success", "N");
					return strReturn;
				}
				
				UserVO vo = new UserVO();
				
				vo.setUser_id(userId);
				vo.setUser_pw(shwUserPw);
				
				userService.updateUserPwd(vo);
				req.getSession().setAttribute("succMsg", "비밀번호가 변경이 되었습니다.");
				req.getSession().setAttribute("success", "Y");
				
				lg.insertLog(Location.UserChange, Common.Success, userId, lh.userChange(req, userId, shwUserPw, "비밀번호 변경"));
				return strReturn;
			}
		}catch(Exception e) {
			e.printStackTrace();
			lg.insertLog(Location.UserChange, Common.Success, userId, lh.userChange(req, userId, "", "비밀번호 변경 실패 !!"+lh.error(e).toString()));
		}
		
		return strReturn;
	}
	
	// ID 중복 확인
	@ResponseBody
	@RequestMapping(value = "/users/check.do", produces="application/text;charset=utf8;")
	public String checkDeviceSn(@RequestParam("user_id") String user_id, ModelMap model, HttpServletRequest req) throws Exception {
		JSONObject json = new JSONObject();
		int iResult = userService.selectCheckUserID(user_id);
		if(iResult > 0) {
			json.put("result", "1");
			json.put("msg", "이미 등록 된 아이디 입니다.");
		}else {
			json.put("result", "0");
			json.put("msg", "등록 가능한 아이디 입니다.");
		}
		
		return json.toString();
	}
}
