package com.windcoder.nightbook.mina.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.windcoder.nightbook.common.entity.User;
import com.windcoder.nightbook.common.service.UserService;
import com.windcoder.nightbook.common.utils.Base64Util;
import com.windcoder.nightbook.common.utils.ReturnCodeUtil;
import com.windcoder.nightbook.common.utils.StringUtillZ;
import com.windcoder.nightbook.common.utils.UUIDUtil;
import com.windcoder.nightbook.common.utils.https.HttpsUtil;
import com.windcoder.nightbook.mina.entity.MinaInfo;
import com.windcoder.nightbook.mina.entity.MinaSession;
import com.windcoder.nightbook.mina.exception.MinaAuthorizationAPIException;
import com.windcoder.nightbook.mina.utils.Constants;
import com.windcoder.nightbook.mina.utils.MinaUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MinaLoginService {

    @Autowired
    private MinaInfoService minaInfoService;
    @Autowired
    private MinaSessionService minaSessionService;
    @Autowired
    private UserService userService;

    public JSONObject doLoginMain(Long pid, String jcode, String encryptData, String iv) throws MinaAuthorizationAPIException {
        if (!StringUtillZ.allIsNotEmpyty(jcode)){
            throw new MinaAuthorizationAPIException(ReturnCodeUtil.MA_PARA_ERR,"MA_PARA_ERR");
        }
        MinaInfo appInfo = minaInfoService.findOne(pid);
        if (appInfo==null){
            throw new MinaAuthorizationAPIException(ReturnCodeUtil.MA_PARA_ERR,"MA_PARA_ERR");
        }

        String url = "https://api.weixin.qq.com/sns/jscode2session?appid="+appInfo.getAppid()+"&secret="+appInfo.getSecret()+"&js_code="+jcode+"&grant_type=authorization_code";
        JSONObject resultData = HttpsUtil.httpRequest(url, "GET", "");
        if(resultData.containsKey("data")){
            String dataStr = resultData.getString("data");
            JSONObject result = JSON.parseObject(dataStr);
            if(result.containsKey("openid")&&result.containsKey("session_key")&&result.containsKey("expires_in")){
                MinaSession session = new MinaSession();
                session.setMina(appInfo);
                session.setSkey(UUIDUtil.newUUID());
                session.setOpenId(result.getString("openid"));
                session.setSessionKey(result.getString("session_key"));
                JSONObject j = MinaUtil.decrypt(appInfo.getAppid(),session.getSessionKey(),encryptData,iv);
                if(j.containsKey("openId")){
                    String userStr = Base64Util.encode(j.toString());
                    if(userStr!=null){
                        session.setUserInfo(userStr);

                        User user = new User();
                        jsonToUser(j,user);
                        userService.save(user);
                        session.setUserId(user.getId());
                        minaSessionService.save(session);
                        JSONObject sobj = new JSONObject();
                        sobj.put("id",user.getId());
                        sobj.put("skey",session.getSkey());
                        JSONObject returnResult = new JSONObject();
                        returnResult.put(Constants.WX_SESSION_MAGIC_ID,1);
                        returnResult.put("session",sobj);
                        return returnResult;
                    }else{
                        throw new MinaAuthorizationAPIException(ReturnCodeUtil.MA_AUTH_ERR,"MA_PARA_ERR");
                    }
                }else{
                    throw new MinaAuthorizationAPIException(ReturnCodeUtil.MA_AUTH_ERR,"MA_PARA_ERR");
                }
            }
        }else{
            throw new MinaAuthorizationAPIException(ReturnCodeUtil.MA_AUTH_ERR,"MA_PARA_ERR");
        }
        return null;
    }

    private void jsonToUser(JSONObject j,User u){
        u.setAvatar(j.getString("avatarUrl"));
        u.setNickname(j.getString("nickName"));
        u.setUuid(j.getString("openId"));
    }

}
