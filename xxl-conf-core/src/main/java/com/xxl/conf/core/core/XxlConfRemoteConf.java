package com.xxl.conf.core.core;

import com.xxl.conf.core.exception.XxlConfException;
import com.xxl.conf.core.util.BaseHttpUtil;
import com.xxl.conf.core.util.BasicJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author xuxueli 2018-11-28
 */
public class XxlConfRemoteConf {
    private static Logger logger = LoggerFactory.getLogger(XxlConfRemoteConf.class);


    private static String adminAddress;
    private static String env;
    private static String accessToken;

    private static List<String> adminAddressArr = null;

    public static void init(String adminAddress, String env, String accessToken) {

        // valid
        if (adminAddress==null || adminAddress.trim().length()==0) {
            throw new XxlConfException("xxl-conf adminAddress can not be empty");
        }
        if (env==null || env.trim().length()==0) {
            throw new XxlConfException("xxl-conf env can not be empty");
        }


        XxlConfRemoteConf.adminAddress = adminAddress;
        XxlConfRemoteConf.env = env;
        XxlConfRemoteConf.accessToken = accessToken;


        // parse
        XxlConfRemoteConf.adminAddressArr = new ArrayList<>();
        if (adminAddress.contains(",")) {
            XxlConfRemoteConf.adminAddressArr.add(adminAddress);
        } else {
            XxlConfRemoteConf.adminAddressArr.addAll(Arrays.asList(adminAddress.split(",")));
        }

    }


    // ---------------------- rest api ----------------------

    /**
     * get and valid
     *
     * @param url
     * @param params
     * @param timeout
     * @return
     */
    private static Map<String, Object> getAndValid(String url, Map<String, String> params, int timeout){

        // param
        boolean firstParam = true;
        for (String key: params.keySet()) {
            url += firstParam?"?":"&";
            if (firstParam) {
                firstParam = false;
            }
            url += key + "=" + params.get(key);
        }

        // resp json
        String respJson = BaseHttpUtil.get(url, timeout);
        if (respJson == null) {
            return null;
        }

        // parse obj
        Map<String, Object> respObj = new BasicJsonParser().parseMap(respJson);
        int code = Integer.valueOf(String.valueOf(respObj.get("code")));
        if (code != 200) {
            logger.info("request fail, msg={}", (respObj.containsKey("msg")?respObj.get("msg"):respJson) );
            return null;
        }
        return respObj;
    }


    /**
     * find
     *
     * @param keys
     * @return
     */
    public static Map<String, String> find(Set<String> keys) {
        for (String adminAddressUrl: XxlConfRemoteConf.adminAddressArr) {

            // url + param
            String url = adminAddressUrl + "/conf/find";

            Map<String, String> params = new HashMap<>();
            params.put("env", env);
            params.put("accessToken", accessToken);
            for (String key:keys) {
                params.put("keys", key);
            }

            // get and valid
            Map<String, Object> respObj = getAndValid(url, params, 10);

            // parse
            if (respObj!=null && respObj.containsKey("data")) {
                Map<String, String> data = (Map<String, String>) respObj.get("data");
                return data;
            }
        }

        return null;
    }

    public static String find(String key) {
        Map<String, String> result = find(new HashSet<String>(Arrays.asList(key)));
        if (result!=null) {
            return result.get(key);
        }
        return null;
    }


    /**
     * monitor
     *
     * @param keys
     * @return
     */
    public static boolean monitor(Set<String> keys) {

        for (String adminAddressUrl: XxlConfRemoteConf.adminAddressArr) {

            // url + param
            String url = adminAddressUrl + "/conf/monitor";

            Map<String, String> params = new HashMap<>();
            params.put("env", env);
            params.put("accessToken", accessToken);
            for (String key:keys) {
                params.put("keys", key);
            }


            // get and valid
            Map<String, Object> respObj = getAndValid(url, params, 60);

            return respObj!=null?true:false;
        }
        return false;
    }

}