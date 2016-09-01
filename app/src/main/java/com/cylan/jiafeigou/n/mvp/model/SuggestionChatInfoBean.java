package com.cylan.jiafeigou.n.mvp.model;

/**
 * 作者：zsl
 * 创建时间：2016/8/30
 * 描述：
 */
public class SuggestionChatInfoBean {

    public String content;
    public int type;
    public String time;

    public SuggestionChatInfoBean(String content, int type, String time) {
        this.content = content;
        this.type = type;
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }

    public String getTime() {
        return time;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
