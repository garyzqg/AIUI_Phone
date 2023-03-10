package com.inspur.mspeech.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIMessage;
import com.iflytek.aiui.AIUISetting;
import com.inspur.mspeech.R;
import com.inspur.mspeech.adapter.MsgAdapter;
import com.inspur.mspeech.audio.AudioTrackOperator;
import com.inspur.mspeech.bean.BaseResponse;
import com.inspur.mspeech.bean.Msg;
import com.inspur.mspeech.net.SpeechNet;
import com.inspur.mspeech.utils.PrefersTool;
import com.inspur.mspeech.utils.UIHelper;
import com.inspur.mspeech.websocket.WebsocketOperator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import jaygoo.widget.wlv.WaveLineView;
import payfun.lib.basis.utils.InitUtil;
import payfun.lib.basis.utils.LogUtil;
import payfun.lib.basis.utils.ToastUtil;
import payfun.lib.dialog.DialogUtil;
import payfun.lib.dialog.listener.OnDialogButtonClickListener;
import payfun.lib.net.exception.ExceptionEngine;
import payfun.lib.net.exception.NetException;
import payfun.lib.net.rx.BaseObserver;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    // AIUI
    private AIUIAgent mAIUIAgent = null;
    // AIUI????????????
    private int mAIUIState = AIUIConstant.STATE_IDLE;

    private AudioTrackOperator mAudioTrackOperator;
    private String mIatMessage;//iat????????????
    private RecyclerView mRvChat;
    private List<Msg> msgList = new ArrayList<>();
    private MsgAdapter mAdapter;
    private WaveLineView mWaveLineView;
    private AppCompatImageView mIvVoiceball;
    private int mWakeUpFlag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //?????????Title
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //??????????????????,????????????setContentView????????????
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_new);
        UIHelper.hideBottomUIMenu(this);


        initView();

        InitUtil.init(this);

        SpeechNet.init();
        //??????
        getPermission();

//        Intent intent = new Intent(this,LoginActivity.class);
//        startActivity(intent);

    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        mWaveLineView = findViewById(R.id.waveLineView);
        mRvChat = findViewById(R.id.recyclerview_chat);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mAdapter = new MsgAdapter(msgList);

        mRvChat.setLayoutManager(layoutManager);
        mRvChat.setAdapter(mAdapter);

        mIvVoiceball = findViewById(R.id.iv_voiceball);
        Glide.with(this)
                .load(R.drawable.gif_voice_ball)
                .placeholder(R.drawable.voice_ball)
                .into(mIvVoiceball);
        mIvVoiceball.setOnClickListener(view -> {
            if (mAIUIAgent != null){
                mWakeUpFlag = 0;
                AIUIMessage wakeupMsg = new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null);
                mAIUIAgent.sendMessage(wakeupMsg);
            }
        });

        //???onOptionsItemSelected????????????
//        toolbar.setOnMenuItemClickListener(item -> {
//            switch (item.getItemId()) {
//                case R.id.setting_menu_voice_name:
//                    Intent intent = new Intent(MainActivity.this, VoiceNameSettingActivity.class);
//                    intentActivityResultLauncher.launch(intent);
//                    break;
//                case R.id.setting_menu_qa:
//                    Intent intent2 = new Intent(MainActivity.this, QaSettingActivity.class);
//                    intentActivityResultLauncher2.launch(intent2);
//                    break;
//            }
//            return true;
//        });

    }

    //???????????????????????????Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setting_action_bar,menu);
        return true;
    }
    //?????????????????????????????????????????????????????????menu???????????????icon???
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return super.onMenuOpened(featureId, menu);
    }
    //?????????????????????item????????????
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.setting_menu_voice_name:
                Intent intent = new Intent(MainActivity.this, VoiceNameSettingActivity.class);
                intentActivityResultLauncher.launch(intent);
                break;
            case R.id.setting_menu_qa:
                Intent intent2 = new Intent(MainActivity.this, QaSettingActivity.class);
                intentActivityResultLauncher2.launch(intent2);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result -> {
        //???????????????????????? ??????init ws
        initWebsocket();

        controlRecord(AIUIConstant.CMD_START_RECORD);
    });

    public ActivityResultLauncher<Intent> intentActivityResultLauncher2 = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result -> {

        controlRecord(AIUIConstant.CMD_START_RECORD);
    });

    @Override
    public void onClick(View view) {

    }

    private void initSDK() {
        // ?????????AIUI
        createAgent();
        //?????????AudioTrack
        initAudioTrack();
        //?????????websocket
        initWebsocket();

    }

    private void getPermission() {
        XXPermissions.with(MainActivity.this)
                // ??????????????????
                .permission(Permission.ACCESS_COARSE_LOCATION, Permission.MANAGE_EXTERNAL_STORAGE, Permission.RECORD_AUDIO)
                // ??????????????????
//                .permission(Permission.Group.CALENDAR)
                // ?????????????????????????????????????????????
                //.interceptor(new PermissionInterceptor())
                // ???????????????????????????????????????????????????
                //.unchecked()
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            LogUtil.i("??????????????????");
                            //??????????????????  ????????????
                            copyAssetFolder(MainActivity.this, "ivw/vtn", String.format("%s/ivw/vtn", "/sdcard/AIUI"));
                            initSDK();

                            //?????????????????????
                            getUserCount();
                        } else {
                            LogUtil.i("????????????????????????" + permissions.toString());

                            XXPermissions.startPermissionActivity(MainActivity.this, permissions);
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) {
                            LogUtil.e("????????????,???????????????");
                            // ??????????????????????????????????????????????????????????????????
                            XXPermissions.startPermissionActivity(MainActivity.this, permissions);
                        } else {
                            LogUtil.e("??????????????????");
                        }
                    }
                });
    }

    private void getUserCount() {
        SpeechNet.userCount(new BaseObserver<BaseResponse<Integer>>() {
            @Override
            public void onNext(@NonNull BaseResponse<Integer> response) {
                if(response.isSuccess()){
                    int data = response.getData();
                    PrefersTool.setAvailableCount(data);
                }else {
                    DialogUtil.showErrorDialog(MainActivity.this, "?????????????????? code = " + response.getCode(), response.getMessage(), new OnDialogButtonClickListener() {
                        @Override
                        public boolean onClick(DialogFragment baseDialog, View v) {
                            ExitApp();
                            return false;
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                NetException netException = ExceptionEngine.handleException(e);
                DialogUtil.showErrorDialog(MainActivity.this, "??????????????????", netException.getErrorTitle(), new OnDialogButtonClickListener() {
                    @Override
                    public boolean onClick(DialogFragment baseDialog, View v) {
                        ExitApp();
                        return false;
                    }
                });
            }
        });
    }

    private void ExitApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }


    private void initAudioTrack() {
        if (mAudioTrackOperator == null){
            mAudioTrackOperator = new AudioTrackOperator();
            mAudioTrackOperator.createStreamModeAudioTrack();
            mAudioTrackOperator.setStopListener(new AudioTrackOperator.IAudioTrackListener() {
                @Override
                public void onStop() {
                    //???????????? ??????????????????
                    controlRecord(AIUIConstant.CMD_START_RECORD);
                    //??????????????? ????????????
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mWaveLineView.getVisibility() != View.VISIBLE){
                                //??????????????????????????????
                                if(!WebsocketOperator.getInstance().isOpen()){
                                    if (mAIUIAgent != null){
                                        mWakeUpFlag = 1;
                                        AIUIMessage wakeupMsg = new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null);
                                        mAIUIAgent.sendMessage(wakeupMsg);
                                    }
                                }else {
                                    //???????????????AIUI???????????????
                                    controlRecord(AIUIConstant.CMD_STOP_RECORD);

                                    mAudioTrackOperator.play();
                                    mAudioTrackOperator.writeSource(MainActivity.this,"audio/ding.pcm");
                                }
                                mWaveLineView.setVisibility(View.VISIBLE);
                                mIvVoiceball.setVisibility(View.GONE);
                                mWaveLineView.startAnim();
                                mWaveLineView.setMoveSpeed(290);
                                mWaveLineView.setVolume(15);
                            }
                        }
                    });

                }

                @Override
                public void onStopResource() {
                    //???????????? ??????????????????
                    controlRecord(AIUIConstant.CMD_START_RECORD);
                    //??????????????? ????????????
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mWaveLineView.getVisibility() != View.VISIBLE){
                                mWaveLineView.setVisibility(View.VISIBLE);
                                mIvVoiceball.setVisibility(View.GONE);
                                mWaveLineView.startAnim();
                                mWaveLineView.setMoveSpeed(290);
                                mWaveLineView.setVolume(15);
                            }
                        }
                    });
                }
            });
        }
    }

    private void initWebsocket() {
        WebsocketOperator.getInstance().initWebSocket(new WebsocketOperator.IWebsocketListener() {
            @Override
            public void OnTtsData(byte[] audioData, boolean isFinish) {
                // TODO: 2023/1/30 ???????????????play CMD_STOP_RECORD?
                if (mAudioTrackOperator != null) {
                    controlRecord(AIUIConstant.CMD_STOP_RECORD);
                    mAudioTrackOperator.play();
                    mAudioTrackOperator.write(audioData, isFinish);
                }
            }

            @Override
            public void OnNlpData(String nlpString) {
                //??????????????????
                int usedCount = PrefersTool.getUsedCount();
                PrefersTool.setUsedCount(++usedCount);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        msgList.add(new Msg(nlpString, Msg.TYPE_RECEIVED));
//                            mAdapter.notifyItemInserted(msgList.size()-1);
                        mAdapter.notifyDataSetChanged();
                        mRvChat.scrollToPosition(msgList.size() - 1);
                    }
                });
            }

            @Override
            public void onOpen() {
                if (mAudioTrackOperator != null) {
                    mAudioTrackOperator.play();
//                    mAudioTrackOperator.writeSource(MainActivity.this, "audio/xiaozhong_box_wakeUpReply.pcm");

                    //???????????????AIUI???????????????
                    controlRecord(AIUIConstant.CMD_STOP_RECORD);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mWakeUpFlag == 0){
                                mAudioTrackOperator.writeSource(MainActivity.this, "audio/"+PrefersTool.getVoiceName()+"_box_wakeUpReply.pcm");
                                msgList.add(new Msg("?????????", Msg.TYPE_RECEIVED));
                                mAdapter.notifyItemInserted(msgList.size() - 1);
//                    mAdapter.notifyDataSetChanged();
                                mRvChat.scrollToPosition(msgList.size() - 1);
                            }else {
                                mAudioTrackOperator.writeSource(MainActivity.this, "audio/ding.pcm");
                            }
                            //???????????????
                            mIvVoiceball.setVisibility(View.GONE);
                            mWaveLineView.setVisibility(View.VISIBLE);
                            mWaveLineView.startAnim();
                            mWaveLineView.setVolume(15);
                        }
                    });
                }


            }

            @Override
            public void onError() {
                if (mAudioTrackOperator != null) {
                    mAudioTrackOperator.play();
                    mAudioTrackOperator.writeSource(MainActivity.this, "audio/xiaozhong_box_disconnect.pcm");
                    //???????????????AIUI???????????????
                    controlRecord(AIUIConstant.CMD_STOP_RECORD);
                }
            }

            @Override
            public void onClose() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //ws????????? ???????????????
                        if (mWaveLineView.getVisibility() == View.VISIBLE) {
                            mWaveLineView.stopAnim();
                            mWaveLineView.setVisibility(View.INVISIBLE);
                            mIvVoiceball.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }

        });

    }
    /**
     * ?????????AIUI
     */
    private void createAgent() {
        if (null == mAIUIAgent) {

            // TODO: 2022/12/5 ????????????????????????
            AIUISetting.setSystemInfo(AIUIConstant.KEY_SERIAL_NUM, "HS6103001A2106000028");

            mAIUIAgent = AIUIAgent.createAgent(this, getAIUIParams(), mAIUIListener);
        }

        if (null == mAIUIAgent) {
            LogUtil.iTag(TAG,"---------create_AIUI FAIL---------");
        } else {
            LogUtil.iTag(TAG,"---------create_AIUI SUCCESS---------");
            controlRecord(AIUIConstant.CMD_START_RECORD);
        }

    }

    private void controlRecord(int cmdConstant) {
        if (mAIUIAgent != null){
            // ??????AIUI??????????????????????????????
            String params = "sample_rate=16000,data_type=audio";
            AIUIMessage writeMsg = new AIUIMessage(cmdConstant, 0, 0, params, null );
            mAIUIAgent.sendMessage(writeMsg);
        }
    }

    private String getAIUIParams() {
        String params = "";
        AssetManager assetManager = getResources().getAssets();
        try {
            InputStream ins = assetManager.open( "cfg/aiui_phone.cfg" );
            byte[] buffer = new byte[ins.available()];

            ins.read(buffer);
            ins.close();

            params = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return params;
    }

    AIUIListener mAIUIListener = new AIUIListener() {
        @Override
        public void onEvent(AIUIEvent event) {
            switch (event.eventType) {
                case AIUIConstant.EVENT_CONNECTED_TO_SERVER:
                    LogUtil.iTag(TAG,"AIUI -- ??????????????????");
                    String uid = event.data.getString("uid");
                    break;

                case AIUIConstant.EVENT_SERVER_DISCONNECTED:
                    LogUtil.iTag(TAG,"AIUI -- ????????????????????????");
                    break;

                case AIUIConstant.EVENT_WAKEUP:
                    LogUtil.iTag(TAG,"AIUI -- WAKEUP ??????????????????");

                    int usedCount = PrefersTool.getUsedCount();
                    int availableCount = PrefersTool.getAvailableCount();
                    if (usedCount>availableCount){
                        ToastUtil.showLong("???????????????????????????");
                    }else {

                        //websocket?????? ?????????????????????????????????
                        WebsocketOperator.getInstance().connectWebSocket();


                        //???????????????????????? ?????? ???????????????????????????????????????????????????
                        if(mAudioTrackOperator != null){
                            mAudioTrackOperator.shutdownExecutor();
                            mAudioTrackOperator.stop();
                            mAudioTrackOperator.flush();

                            mAudioTrackOperator.isPlaying = false;
                        }
                    }


                    break;
                //????????????????????????????????????????????????????????????
                case AIUIConstant.EVENT_RESULT://1
                    //??????????????????
//                    LogUtil.iTag(TAG, "AIUI STATUS --- ??????INFO -- " + event.info);
//                    LogUtil.iTag(TAG, "AIUI STATUS --- ??????DATA -- " + event.data);
                    //????????????(iat)
                    //????????????(nlp)
                    //?????????????????????(tpp)
                    //??????tts??????(tts)
                    //????????????(itrans)

                    /**
                     * event.info
                     * {
                     *     "data": [{
                     *         "params": {
                     *             "sub": "iat",
                     *         },
                     *         "content": [{
                     *             "dte": "utf8",
                     *             "dtf": "json",
                     *             "cnt_id": "0"
                     *         }]
                     *     }]
                     * }
                     */
                    try {
                        JSONObject info = new JSONObject(event.info);
                        JSONObject infoData = info.optJSONArray("data").optJSONObject(0);
                        String sub = infoData.optJSONObject("params").optString("sub");
                        JSONObject content = infoData.optJSONArray("content").optJSONObject(0);

                        if (content.has("cnt_id")) {
                            String cnt_id = content.optString("cnt_id");
                            String resultString = new String(event.data.getByteArray(cnt_id), "utf-8");
                            if ("iat".equals(sub) && resultString.length() > 2) {
//                                LogUtil.iTag(TAG, "AIUI EVENT_RESULT --- resultString -- " + resultString);
                                JSONObject result = new JSONObject(resultString);
                                JSONObject text = result.optJSONObject("text");
                                boolean ls = text.optBoolean("ls");//????????????
                                int sn = text.optInt("sn");//?????????
                                JSONArray ws = text.optJSONArray("ws");
                                StringBuilder currentIatMessage = new StringBuilder();
                                for (int j = 0; j < ws.length(); j++) {
                                    JSONArray cw = ws.optJSONObject(j).optJSONArray("cw");
                                    String w = cw.optJSONObject(0).optString("w");
                                    if (!TextUtils.isEmpty(w)){
                                        currentIatMessage.append(w);
                                    }
                                }

                                LogUtil.iTag(TAG, "AIUI EVENT_RESULT --- iat -- current -- " + currentIatMessage);

                                if (currentIatMessage!= null && currentIatMessage.length()>0){
                                    mIatMessage = currentIatMessage.toString();

                                    if (WebsocketOperator.getInstance().isOpen()){

                                        if(sn == 1){
                                            //?????????????????????
                                            if (mWaveLineView.getVisibility() != View.VISIBLE){
                                                mWaveLineView.setVisibility(View.VISIBLE);
                                                mIvVoiceball.setVisibility(View.GONE);
                                                mWaveLineView.startAnim();
                                            }
                                            mWaveLineView.setVolume(70);
                                            mWaveLineView.setMoveSpeed(150);

                                            msgList.add(new Msg(mIatMessage,Msg.TYPE_SEND));
                                        }else{
                                            msgList.set(msgList.size()-1,new Msg(mIatMessage,Msg.TYPE_SEND));
                                        }

//                                    mAdapter.notifyItemInserted(msgList.size()-1);
                                        mAdapter.notifyDataSetChanged();
                                        mRvChat.scrollToPosition(msgList.size()-1);
                                    }

                                }

                                if (ls){
                                    LogUtil.iTag(TAG, "AIUI EVENT_RESULT --- iat -- final -- " + mIatMessage);

                                    WebsocketOperator.getInstance().sendMessage(mIatMessage);

                                    if (WebsocketOperator.getInstance().isOpen()) {

                                        mWaveLineView.setVisibility(View.INVISIBLE);
                                        mWaveLineView.setVolume(15);
                                        mWaveLineView.setMoveSpeed(290);
                                        mWaveLineView.stopAnim();
                                        mIvVoiceball.setVisibility(View.VISIBLE);

                                        mAudioTrackOperator.isPlaying = true;
                                        //???????????????AIUI???????????????
//                                        controlRecord(AIUIConstant.CMD_STOP_RECORD);
//
                                    }


                                }

                            }
                        }


                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    break;
                case AIUIConstant.EVENT_SLEEP:
                    LogUtil.iTag(TAG, "AIUI -- ??????????????????");
                    break;

                case AIUIConstant.EVENT_START_RECORD:
                    LogUtil.iTag(TAG, "AIUI -- ???????????????");
                    break;

                case AIUIConstant.EVENT_STOP_RECORD:
                    LogUtil.iTag(TAG, "AIUI -- ???????????????");
                    break;
                // ????????????
                case AIUIConstant.EVENT_STATE://3
                    mAIUIState = event.arg1;
                    if (AIUIConstant.STATE_IDLE == mAIUIState) {
                        // ???????????????AIUI?????????
                        LogUtil.iTag(TAG, "AIUI -- STATE_IDLE");
                    } else if (AIUIConstant.STATE_READY == mAIUIState) {
                        // AIUI????????????????????????
                        LogUtil.iTag(TAG, "AIUI -- STATE_READY");
                    } else if (AIUIConstant.STATE_WORKING == mAIUIState) {
                        // AIUI???????????????????????????
                        LogUtil.iTag(TAG, "AIUI -- STATE_WORKING");
                    }
                    break;
                case AIUIConstant.EVENT_TTS: {
                    switch (event.arg1) {
                        case AIUIConstant.TTS_SPEAK_BEGIN:
                            LogUtil.iTag(TAG, "TTS --- ????????????");
                            break;

                        case AIUIConstant.TTS_SPEAK_PROGRESS:
                            LogUtil.iTag(TAG, "TTS --- ???????????????" + event.data.getInt("percent"));
                            break;

                        case AIUIConstant.TTS_SPEAK_PAUSED:
                            LogUtil.iTag(TAG, "TTS --- ????????????");
                            break;

                        case AIUIConstant.TTS_SPEAK_RESUMED:
                            LogUtil.iTag(TAG, "TTS --- ????????????");
                            break;

                        case AIUIConstant.TTS_SPEAK_COMPLETED:
                            LogUtil.iTag(TAG, "TTS --- ????????????");
                            break;

                        default:
                            break;
                    }
                }
                    break;
                //????????????
                case AIUIConstant.EVENT_ERROR://2
                    // TODO: 2023/2/3 ?????????11217 ?????? ?????????????????????
                    LogUtil.iTag(TAG,"??????: " + event.arg1 + "\n" + event.info);
                    LogUtil.iTag(TAG,"---------error_aiui---------");
                    break;
            }
        }
    };

    public static boolean copyAssetFolder(Context context, String srcName, String dstName) {
        try {
            boolean result = true;
            String fileList[] = context.getAssets().list(srcName);
            if (fileList == null) return false;

            if (fileList.length == 0) {
                result = copyAssetFile(context, srcName, dstName);
            } else {
                File file = new File(dstName);
                result = file.mkdirs();
                for (String filename : fileList) {
                    result &= copyAssetFolder(context, srcName + File.separator + filename, dstName + File.separator + filename);
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyAssetFile(Context context, String srcName, String dstName) {
        try {
            InputStream in = context.getAssets().open(srcName);
            File outFile = new File(dstName);
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            OutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWaveLineView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWaveLineView.onPause();

        //????????????????????? ws?????? audiotrack????????????
        WebsocketOperator.getInstance().close();

        mWakeUpFlag = 0;
        if(mAudioTrackOperator != null){
            mAudioTrackOperator.shutdownExecutor();
            mAudioTrackOperator.stop();
            mAudioTrackOperator.flush();

            mAudioTrackOperator.isPlaying = false;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWaveLineView.release();
    }
}