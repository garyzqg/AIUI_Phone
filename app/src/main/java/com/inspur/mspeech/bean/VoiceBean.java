package com.inspur.mspeech.bean;

import java.util.List;

/**
 * @author : zhangqinggong
 * date    : 2023/2/25 16:53
 * desc    :
 */
public class VoiceBean {
    private String voiceId;
    private String voiceName;//XiaohanNeural
    private String voiceAlias;//晓寒

    public List<VoiceStyleBean> getVoiceStyleList() {
        return voiceStyleList;
    }

    public void setVoiceStyleList(List<VoiceStyleBean> voiceStyleList) {
        this.voiceStyleList = voiceStyleList;
    }

    private List<VoiceStyleBean> voiceStyleList;


    public VoiceBean(String voiceId, String voiceName, String voiceAlias) {
        this.voiceId = voiceId;
        this.voiceName = voiceName;
        this.voiceAlias = voiceAlias;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceAlias() {
        return voiceAlias;
    }

    public void setVoiceAlias(String voiceAlias) {
        this.voiceAlias = voiceAlias;
    }


}
