package com.tools.ftpshare.common;

import java.util.HashMap;

/**
 * 用户管理器
 * @author jpeng
 */
public enum  SessionManger {
    /**
     * 单实例
     */
    INS;
    private HashMap<String,Session> sessionHashMap = new HashMap<String, Session>();


    public void addSession(Session session){
        sessionHashMap.put(session.getUserid(),session);
    }


    public Session getSession(String userid){
        return sessionHashMap.get(userid);
    }

}
