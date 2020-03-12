package com.decision.v2x.era.util.convert;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class LogHelper 
{
	static public String getIP(HttpServletRequest request)
	{
		 
        String ip = request.getHeader("X-Forwarded-For");
 
        if (ip == null) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("WL-Proxy-Client-IP"); // 웹로직
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        
        return ip;
	}
	
	public JSONObject login(HttpServletRequest req, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		
		return retval;
	}
	
	public JSONObject logout(HttpServletRequest req, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		
		
		return retval;
	}
	
	public JSONObject list(HttpServletRequest req, int row, int now, int total, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("page", now);
		retval.put("row", row);
		retval.put("total", total);
		retval.put("msg", msg);
		
		
		return retval;
	}
	static public JSONArray error(Exception e)
	{
		JSONArray retval = new JSONArray();
		
		StackTraceElement[] arr = e.getStackTrace();
		
		retval.add(e.getMessage());
		
		for(int i = 0; i<arr.length; i++)
		{
			retval.add(arr[i].toString());
		}
		
		return retval;
	}
	
	public JSONObject error(HttpServletRequest req, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		
		
		return retval;
	}
	
	public static JSONArray toArray(int[] arr)
	{
		if(arr == null)
			return null;
		
		JSONArray retval = new JSONArray();

		for(int i = 0; i<arr.length; i++)
		{
			retval.add(arr[i]);
		}
	
		return retval;		
	}
	
	public JSONObject makeCert(HttpServletRequest req, String type, String deviceSN, int count,
			int[] psids, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("type", type);
		retval.put("psids", toArray(psids));
		retval.put("contryCode", toArray(arrContryCode));
		retval.put("circularRegin", toArray(arrCircularRegin));
		retval.put("durationUint", strDurationUint);
		retval.put("durationValue", nDurationValue);
		retval.put("count", count);
		retval.put("msg", msg);
		
		return retval;
	}
	
	public JSONObject dashboard(HttpServletRequest req, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		
		
		return retval;
	}
	public JSONObject deviceAdd(HttpServletRequest req, String deviceSN, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("deviceSN", deviceSN);
		
		
		return retval;
	}
	public JSONObject deviceAdd(HttpServletRequest req, String deviceSN, String deviceIdType, 
			String deviceType, String deviceCompany, String deviceName, 
			String deviceNumber, String deviceVersion, String csrName, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("device_sn", deviceSN);
		retval.put("device_type", deviceType);
		retval.put("device_company", deviceCompany);
		retval.put("device_name", deviceName);
		retval.put("device_number", deviceNumber);
		retval.put("device_ver", deviceVersion);
		retval.put("csr_name", csrName);
		
		
		return retval;
	}
	public JSONObject deviceDetail(HttpServletRequest req, String deviceSN, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("deviceSN", deviceSN);
		
		
		return retval;
	}
	public JSONObject deviceDelete(HttpServletRequest req, String deviceSN, String publicKey, String deviceId, String certId, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("public_key", publicKey);
		retval.put("device_sn", deviceSN);
		retval.put("device_id", Long.parseLong(deviceId));
		retval.put("cert_id", certId);
		
		
		return retval;
	}
	public JSONObject deviceChange(HttpServletRequest req, String deviceSN, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("deviceSN", deviceSN);
		
		
		return retval;
	}
	public JSONObject enrolDownload(HttpServletRequest req, String publicKey, Long deviceID, String certID, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("public_key", publicKey);
		retval.put("cert_id", certID);
		retval.put("device_id", deviceID);
		
		
		return retval;
	}
	public JSONObject bootstrapDownload(HttpServletRequest req, String requestHash, Long deviceID, String certID, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("request_hash", requestHash);
		retval.put("cert_id", certID);
		retval.put("device_id", deviceID);
		
		
		return retval;
	}
	public JSONObject makeDownload(HttpServletRequest req, String publicKey, Long deviceID, String certID, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("public_key", publicKey);
		retval.put("cert_id", certID);
		retval.put("device_id", deviceID);
		
		
		return retval;
	}
	
	public JSONObject userSearch(HttpServletRequest req,  int row, int now, int total, String id, String name, String regStart, String regEnd, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("name", name);
		retval.put("reg_start", regStart);
		retval.put("reg_end", regEnd);
		retval.put("page", now);
		retval.put("row", row);
		retval.put("total", total);
		
		return retval;
	}
	public JSONObject certSearch(HttpServletRequest req, int row, int now, int total, String createCertDate, String expireCertDate, 
			String createDeviceDate, String modifyDeviceDate, String device_sn, String certType, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("create_cert_date", createCertDate);
		retval.put("expire_cert_date", expireCertDate);
		retval.put("device_sn", device_sn);
		retval.put("create_device_date", createDeviceDate);
		retval.put("modify_device_date", modifyDeviceDate);
		retval.put("cert_type", certType);
		retval.put("page", now);
		retval.put("row", row);
		retval.put("total", total);
		
		return retval;
	}
	public JSONObject deviceSearch(HttpServletRequest req, int row, int now, int total, 
			String regStart, String regEnd, String deviceName, String deviceSN, String deviceType, String deviceIdType, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("create_date_start", regStart);
		retval.put("create_date_end", regEnd);
		retval.put("device_name", deviceName);
		retval.put("device_sn", deviceSN);
		retval.put("device_type", deviceType);
		retval.put("device_sn_type", deviceIdType);
		retval.put("page", now);
		retval.put("row", row);
		retval.put("total", total);
		
		return retval;
	}
	
	public JSONObject userAdd(HttpServletRequest req, String userId, String userName, String auth, String depart, String position, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("user_id", userId);
		retval.put("user_name", userName);
		retval.put("auth", auth);
		retval.put("depart", depart);
		retval.put("position", position);
		
		return retval;
	}
	
	public JSONObject userDetail(HttpServletRequest req, String userId, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("user_id", userId);
		
		return retval;
	}
	public JSONObject userDelete(HttpServletRequest req, String userId, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("user_id", userId);
		
		return retval;
	}
	public JSONObject userChange(HttpServletRequest req, String userId, String userPw, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		retval.put("user_id", userId);
		retval.put("user_pw", userPw);
		
		return retval;
	}
	public JSONObject userChange(HttpServletRequest req, String userId, 
			String beforeAuthId, String beforeAuthGroupId,
			String afterAuthId, String afterAuthGroupId, String msg)
	{
		JSONObject retval = new JSONObject();
		JSONObject before = new JSONObject();
		JSONObject after = new JSONObject();

		before.put("authority_id", beforeAuthId);
		before.put("authority_id_group_id", beforeAuthGroupId);
		after.put("authority_id", afterAuthId);
		after.put("authority_id_group_id", afterAuthGroupId);
		
		retval.put("before", before);
		retval.put("after", after);
		retval.put("ip", getIP(req));
		retval.put("msg", msg);

		return retval;
	}
	
	public JSONObject statistics(HttpServletRequest req, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("msg", msg);
		
		return retval;
	}
	public JSONObject infraApply(HttpServletRequest req, String docNo, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("doc_no", docNo);
		retval.put("msg", msg);
		
		return retval;
	}
	
	public JSONObject infraCreate(HttpServletRequest req, String docNo, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("doc_no", docNo);
		retval.put("msg", msg);
		
		return retval;
	}
	
	
	public JSONObject infraUpload(HttpServletRequest req, String docNo, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("doc_no", docNo);
		retval.put("msg", msg);
		
		return retval;
	}
	
	public JSONObject infraCsrDownload(HttpServletRequest req, String docNo, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("doc_no", docNo);
		retval.put("msg", msg);
		
		return retval;
	}
	
	
	
	
	public JSONObject checkPermission(HttpServletRequest req, String id, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("id", id);
		retval.put("msg", msg);
		
		return retval;
	}
	

	public JSONObject createEnrol(HttpServletRequest req, Long certKeyId, String certId, Long deviceId, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("certificateskeySeq", certKeyId);
		retval.put("cert_id", certId);
		retval.put("device_id", deviceId);
		retval.put("msg", msg);
		
		return retval;
	}
	public JSONObject deleteEnrol(HttpServletRequest req, String certKeyId, String certId, String publicKey, Long deviceId, String deviceSn, String msg)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("ip", getIP(req));
		retval.put("cert_key_id", certKeyId);
		retval.put("cert_id", certId);
		retval.put("device_id", deviceId);
		retval.put("device_sn", deviceSn);
		retval.put("msg", msg);
		
		return retval;
	}
}
