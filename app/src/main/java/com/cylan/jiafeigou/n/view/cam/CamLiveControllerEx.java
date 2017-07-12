package com.cylan.jiafeigou.n.view.cam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.cylan.entity.jniCall.JFGMsgVideoResolution;
import com.cylan.entity.jniCall.JFGMsgVideoRtcp;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.view.JFGSourceManager;
import com.cylan.jiafeigou.cache.SimpleCache;
import com.cylan.jiafeigou.cache.db.module.Device;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.misc.AlertDialogManager;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JError;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.n.base.BaseApplication;
import com.cylan.jiafeigou.n.mvp.contract.cam.CamLiveContract;
import com.cylan.jiafeigou.n.view.activity.SightSettingActivity;
import com.cylan.jiafeigou.n.view.media.NormalMediaFragment;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.block.log.PerformanceUtils;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.ActivityUtils;
import com.cylan.jiafeigou.utils.MiscUtils;
import com.cylan.jiafeigou.utils.NetUtils;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.cylan.jiafeigou.utils.TimeUtils;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.utils.ViewUtils;
import com.cylan.jiafeigou.widget.LiveTimeLayout;
import com.cylan.jiafeigou.widget.Switcher;
import com.cylan.jiafeigou.widget.flip.FlipImageView;
import com.cylan.jiafeigou.widget.flip.FlipLayout;
import com.cylan.jiafeigou.widget.live.ILiveControl;
import com.cylan.jiafeigou.widget.live.LiveControlView;
import com.cylan.jiafeigou.widget.pop.RelativePopupWindow;
import com.cylan.jiafeigou.widget.pop.RoundCardPopup;
import com.cylan.jiafeigou.widget.video.LiveViewWithThumbnail;
import com.cylan.jiafeigou.widget.video.VideoViewFactory;
import com.cylan.jiafeigou.widget.wheel.ex.SuperWheelExt;
import com.cylan.panorama.CameraParam;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.cylan.jiafeigou.dp.DpMsgMap.ID_501_CAMERA_ALARM_FLAG;
import static com.cylan.jiafeigou.misc.JConstant.KEY_CAM_SIGHT_SETTING;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_IDLE;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_LOADING_FAILED;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_NET_CHANGED;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_PLAYING;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_PREPARE;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_STOP;
import static com.cylan.jiafeigou.misc.JFGRules.PlayErr.STOP_MAUNALLY;
import static com.cylan.jiafeigou.n.mvp.contract.cam.CamLiveContract.TYPE_HISTORY;
import static com.cylan.jiafeigou.n.mvp.contract.cam.CamLiveContract.TYPE_LIVE;
import static com.cylan.jiafeigou.widget.wheel.ex.SuperWheelExt.STATE_FINISH;

/**
 * Created by hds on 17-4-19.
 */

public class CamLiveControllerEx extends RelativeLayout implements ICamLiveLayer,
        View.OnClickListener {
    private String uuid;
    private static final String TAG = "CamLiveControllerEx";
    private ILiveControl.Action action;
    //横屏 top bar
    private View layoutA;
    //流量
    private View layoutB;
    //loading
    private LiveControlView layoutC;
    //防护  |直播|时间|   |全屏|
    private View layoutD;
    //历史录像条
    private ViewSwitcher layoutE;
    //|speaker|mic|capture|
    private View layoutF;
    //横屏 侧滑日历
    private View layoutG;

    private boolean isNormalView;
    private int livePlayState;
    private int livePlayType;
    private SuperWheelExt superWheelExt;
    private OnClickListener liveTextClick;//直播按钮
    private OnClickListener liveTimeRectListener;
    private OnClickListener playClickListener;
    private RoundCardPopup roundCardPopup;
    private LiveViewWithThumbnail liveViewWithThumbnail;

    private HistoryWheelHandler historyWheelHandler;

    private CamLiveContract.Presenter presenter;
    private Switcher streamSwitcher;
    private int pid;
    private String cVersion;
    private boolean isRSCam;
    private Handler handler = new Handler();
    /**
     * 设备的时区
     */
    private SimpleDateFormat liveTimeDateFormat;

    public CamLiveControllerEx(Context context) {
        this(context, null);
    }

    public CamLiveControllerEx(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CamLiveControllerEx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //竖屏 隐藏
        layoutA = findViewById(R.id.layout_a);
        layoutB = findViewById(R.id.layout_b);
        layoutC = (LiveControlView) findViewById(R.id.layout_c);
        layoutD = findViewById(R.id.layout_d);
        layoutE = (ViewSwitcher) findViewById(R.id.layout_e);
        layoutF = findViewById(R.id.layout_f);
        layoutG = findViewById(R.id.layout_g);
        liveViewWithThumbnail = (LiveViewWithThumbnail) findViewById(R.id.v_live);
        superWheelExt = (SuperWheelExt) findViewById(R.id.sw_cam_live_wheel);
        initListener();
    }

    private void initListener() {
//        PerformanceUtils.startTrace("initListener");
        //顶部
        //a.返回,speaker,mic,capture
        Context context = getContext();
        if (context instanceof FragmentActivity) {
            Log.d(TAG, TAG + " context is activity");
            layoutA.findViewById(R.id.imgV_cam_live_land_nav_back).setOnClickListener(this);
            layoutA.findViewById(R.id.imgV_land_cam_switch_speaker).setOnClickListener(this);
            layoutA.findViewById(R.id.imgV_land_cam_trigger_mic).setOnClickListener(this);
            layoutA.findViewById(R.id.imgV_land_cam_trigger_capture).setOnClickListener(this);
        }
        //isFriend.流量
        //c.loading
        (layoutC).setAction(this.action);
        //d.time
//        ((FlipLayout) layoutD.findViewById(R.id.layout_port_flip))
//                .setFlipListener(this);
        layoutD.findViewById(R.id.imgV_cam_zoom_to_full_screen)
                .setOnClickListener(this);
        //e.
        View vLandPlay = layoutE.findViewById(R.id.imgV_cam_live_land_play);
        if (vLandPlay != null) vLandPlay.setOnClickListener(this);
        View tvLive = layoutE.findViewById(R.id.tv_live);
        if (tvLive != null) tvLive.setOnClickListener(this);
        //f
        layoutF.findViewById(R.id.imgV_cam_switch_speaker).setOnClickListener(this);
        layoutF.findViewById(R.id.imgV_cam_trigger_mic).setOnClickListener(this);
        layoutF.findViewById(R.id.imgV_cam_trigger_capture).setOnClickListener(this);
        layoutE.findViewById(R.id.btn_load_history)
                .setOnClickListener(v -> {
                    AppLogger.d("点击加载历史视频");
                    layoutE.findViewById(R.id.btn_load_history).setEnabled(false);
                    livePlayState = PLAY_STATE_PREPARE;
                    setLoadingState(null, getResources().getString(R.string.LOADING));
                    Subscription subscription = Observable.just("get")
                            .subscribeOn(Schedulers.io())
                            .map(ret -> presenter.fetchHistoryDataList())
                            .flatMap(aBoolean -> Observable.concat(RxBus.getCacheInstance().toObservable(RxEvent.HistoryBack.class)
                                            .timeout(5, TimeUnit.SECONDS),
                                    RxBus.getCacheInstance().toObservable(RxEvent.HistoryEmpty.class)
                                            .timeout(5, TimeUnit.SECONDS))
                                    .first())
                            .flatMap(o -> Observable.just(o instanceof RxEvent.HistoryEmpty ? 2 : -1))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ret -> {
                                AppLogger.d("加载成功:" + ret);
                                layoutE.findViewById(R.id.btn_load_history).setEnabled(true);
                                if (ret == 2) {
                                    ToastUtil.showToast(getResources().getString(R.string.NO_CONTENTS_2));
                                    livePlayState = PLAY_STATE_STOP;
                                    setLoadingState(PLAY_STATE_STOP, null);
                                    return;
                                }
                                if (layoutE.getCurrentView() instanceof ViewGroup) {
                                    layoutE.showNext();
                                    AppLogger.d("需要展示 遮罩?需要判断 userVisible状态");
                                }
                                LiveShowCase.showHistoryWheelCase((Activity) getContext(), null);
                            }, throwable -> {
                                if (throwable instanceof TimeoutException) {
                                    layoutE.findViewById(R.id.btn_load_history).setEnabled(true);
                                    livePlayState = PLAY_STATE_STOP;
                                    setLoadingState(PLAY_STATE_STOP, null);
                                    ToastUtil.showToast(getResources().getString(R.string.Item_LoadFail));
                                }
                            });
                    presenter.addSubscription("fetchHistoryBy", subscription);
                });
    }

    @Override
    public void initLiveViewRect(float ratio, Rect rect) {
        updateLiveViewRectHeight(ratio);
        liveViewWithThumbnail.post(() -> {
            liveViewWithThumbnail.getLocalVisibleRect(rect);
            AppLogger.d("rect: " + rect);
        });
    }

    @Override
    public void initView(CamLiveContract.Presenter presenter, String uuid) {
        this.presenter = presenter;
        this.uuid = uuid;
        //disable 6个view
        setHotSeatState(-1, false, false, false, false, false, false);
        findViewById(R.id.imgV_land_cam_trigger_capture).setEnabled(false);
        findViewById(R.id.imgV_cam_trigger_capture).setEnabled(false);
        findViewById(R.id.imgV_cam_zoom_to_full_screen).setEnabled(false);
        findViewById(R.id.tv_live).setEnabled(false);
        Device device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
        this.cVersion = device.$(207, "");
        isRSCam = JFGRules.isRS(device.pid);
        if (device == null) {
            AppLogger.e("device is null");
            return;
        }
        this.pid = device.pid;
        isNormalView = JFGRules.isNeedNormalRadio(device.pid);

        VideoViewFactory.IVideoView videoView = VideoViewFactory.CreateRendererExt(!isNormalView,
                getContext(), true);
        videoView.setInterActListener(new VideoViewFactory.InterActListener() {

            @Override
            public boolean onSingleTap(float x, float y) {
//                camLiveController.tapVideoViewAction();
                onLiveRectTap();
                return true;
            }

            @Override
            public void onSnapshot(Bitmap bitmap, boolean tag) {
                Log.d("onSnapshot", "onSnapshot: " + (bitmap == null));
            }
        });
        String _509 = device.$(509, "1");
        videoView.config360(TextUtils.equals(_509, "0") ? CameraParam.getTopPreset() : CameraParam.getWallPreset());
        videoView.setMode(TextUtils.equals("0", _509) ? 0 : 1);
        liveViewWithThumbnail.setLiveView(videoView);
        initSightSetting(presenter);
        //分享用户不显示
        boolean showFlip = !presenter.isShareDevice() && JFGRules.hasProtection(device.pid);
        View flipPort = findViewById(R.id.layout_port_flip);
        flipPort.setVisibility(showFlip ? VISIBLE : INVISIBLE);
        //要根据设备属性表决定是否显示加载历史视频的按钮
        View btn = findViewById(R.id.btn_load_history);
        btn.setVisibility(JFGRules.hasHistory(device.pid) ? VISIBLE : GONE);

        findViewById(R.id.layout_land_flip).setVisibility(showFlip && MiscUtils.isLand() ? VISIBLE : GONE);
        findViewById(R.id.v_divider).setVisibility(showFlip && MiscUtils.isLand() ? VISIBLE : GONE);
        //是否显示清晰度切换
//        findViewById(R.id.sv_switch_stream)
//                .setVisibility(JFGRules.showSdHd(device.pid, cVersion) ? VISIBLE : GONE);
        streamSwitcher = ((Switcher) findViewById(R.id.sv_switch_stream));
        int mode = device.$(513, 0);
        streamSwitcher.setMode(mode);
        streamSwitcher.setSwitcherListener((view, index) -> {
            if (view.getId() == R.id.switch_hd) {
                presenter.switchStreamMode(index)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(ret -> {
                        }, AppLogger::e);
            } else if (view.getId() == R.id.switch_sd) {
                presenter.switchStreamMode(index)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(ret -> {
                        }, AppLogger::e);
            }
            if (MiscUtils.isLand()) {
                removeCallbacks(landHideRunnable);
                postDelayed(landHideRunnable, 3000);
            } else {
                removeCallbacks(portHideRunnable);
                postDelayed(portHideRunnable, 3000);
            }
        });
        AppLogger.d("需要重置清晰度");
    }

    public HistoryWheelHandler getHistoryWheelHandler(CamLiveContract.Presenter presenter) {
        reInitHistoryHandler(presenter);
        return historyWheelHandler;
    }

    private boolean isSightShow;

    public boolean isSightShow() {
        return isSightShow;
    }

    /**
     * 全景视角设置
     */
    private void initSightSetting(CamLiveContract.Presenter basePresenter) {
        if (isNormalView || basePresenter.isShareDevice()) return;
        String uuid = basePresenter.getUuid();
        isSightShow = PreferencesUtils.getBoolean(KEY_CAM_SIGHT_SETTING + uuid, true);
        Log.d("initSightSetting", "judge? " + isSightShow);
        if (!isSightShow) return;//不是第一次
        View oldLayout = liveViewWithThumbnail.findViewById(R.id.fLayout_cam_sight_setting);
        if (oldLayout == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.cam_sight_setting_overlay, null);
            liveViewWithThumbnail.addView(view);//最顶
            View layout = liveViewWithThumbnail.findViewById(R.id.fLayout_cam_sight_setting);
            ((TextView) (view.findViewById(R.id.tv_sight_setting_content)))
                    .setText(getContext().getString(R.string.Tap1_Camera_Overlook) + ": "
                            + getContext().getString(R.string.Tap1_Camera_OverlookTips));
            view.findViewById(R.id.btn_sight_setting_cancel).setOnClickListener((View v) -> {
                if (layout != null) liveViewWithThumbnail.removeView(layout);
                basePresenter.startPlay();
            });
            layout.setOnClickListener(v -> AppLogger.d("don't click me"));
            view.findViewById(R.id.btn_sight_setting_next).setOnClickListener((View v) -> {
                liveViewWithThumbnail.removeView(layout);
                Intent intent = new Intent(getContext(), SightSettingActivity.class);
                intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
                getContext().startActivity(intent);
            });
            PreferencesUtils.putBoolean(KEY_CAM_SIGHT_SETTING + uuid, false);
        } else {
            //已经添加了
            oldLayout.setVisibility(View.VISIBLE);
        }
        livePlayState = PLAY_STATE_IDLE;
        setLoadingState(null, null);
    }

    /**
     * 视频区域
     */
    private void onLiveRectTap() {
        AppLogger.e("点击,需要播放状态");
        if (isLand()) {
            removeCallbacks(landShowOrHideRunnable);
            post(landShowOrHideRunnable);
        } else {
            if (isStandBy()) {
                post(portHideRunnable);
                return;
            }
            //只有播放的时候才能操作//loading的时候 不能点击
            if (livePlayState == PLAY_STATE_PLAYING) {
//                layoutA.setTranslationY(0);
//                layoutD.setTranslationY(0);
//                layoutE.setTranslationY(0);
                boolean toHide = layoutC.isShown();
                if (toHide) {
                    removeCallbacks(portShowRunnable);
                    post(portHideRunnable);
                } else {
                    removeCallbacks(portHideRunnable);
                    post(portShowRunnable);
                }
            }
//            if (!toHide) prepareLayoutDAnimation();
        }
    }

    /**
     * 历史录像条显示逻辑
     *
     * @param show
     */
    private void showHistoryWheel(boolean show) {
        //处理显示逻辑
        Device device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
        //1.sd
        DpMsgDefine.DPSdStatus status = device.$(204, new DpMsgDefine.DPSdStatus());
        if (!status.hasSdcard || status.err != 0) {
            //隐藏
            layoutE.setVisibility(INVISIBLE);
            return;
        }
        //2.手机无网络
        int net = NetUtils.getJfgNetType();
        if (net == 0) {
            //隐藏
            layoutE.setVisibility(INVISIBLE);
            return;
        }
        //3.没有历史录像
        if (superWheelExt.getDataProvider() != null && superWheelExt.getDataProvider().getDataCount() > 0) {
            //显示
        } else {
//            layoutE.setVisibility(INVISIBLE);
            return;
        }
        //4.被分享用户不显示
        if (JFGRules.isShareDevice(device)) {
            layoutE.setVisibility(INVISIBLE);
        }
        //5.设备离线
        if (!JFGRules.isDeviceOnline(device.$(201, new DpMsgDefine.DPNet()))) {
            layoutE.setVisibility(INVISIBLE);
        }
        //3.没有历史录像
        if (superWheelExt.getDataProvider() != null && superWheelExt.getDataProvider().getDataCount() > 0) {
            //显示
            layoutE.setVisibility(VISIBLE);
            return;
        }
        if (!show) {
            layoutE.setVisibility(INVISIBLE);
        } else layoutE.setVisibility(VISIBLE);
    }

    @Override
    public void initHotRect() {

    }

    @Override
    public void onLivePrepared(int type) {
        livePlayType = type;
        livePlayState = PLAY_STATE_PREPARE;
        setLoadingState(null, null);
        findViewById(R.id.imgV_cam_zoom_to_full_screen).setEnabled(false);
        int net = NetUtils.getJfgNetType();
        if (net == 2)
            ToastUtil.showToast(getResources().getString(R.string.LIVE_DATA));
    }

    private boolean isLand() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 3s隐藏
     */
    private void prepareLayoutDAnimation(boolean touchUp) {
        if (MiscUtils.isLand()) {
            removeCallbacks(landHideRunnable);
            if (touchUp) postDelayed(landHideRunnable, 3000);
        } else {
            post(portShowRunnable);
        }
    }

    private Runnable portHideRunnable = new Runnable() {
        @Override
        public void run() {
            setLoadingState(null, null);
            streamSwitcher.setVisibility(GONE);
            layoutC.setVisibility(INVISIBLE);
            Log.d("wahat", "portHideRunnable");
        }
    };
    private Runnable portShowRunnable = new Runnable() {
        @Override
        public void run() {
            layoutD.setVisibility(VISIBLE);
            if (layoutD.getAlpha() == 0.0f)
                YoYo.with(Techniques.FadeIn)
                        .duration(200)
                        .playOn(layoutD);
            showHistoryWheel(true);
            removeCallbacks(portHideRunnable);
            postDelayed(portHideRunnable, 3000);
            setLoadingState(null, null);
            streamSwitcher.setVisibility(livePlayState == PLAY_STATE_PLAYING && JFGRules.showSdHd(pid, cVersion) ? VISIBLE : GONE);
            if (livePlayState == PLAY_STATE_PLAYING) {
                layoutC.setVisibility(VISIBLE);
            }
            Log.d("wahat", "portShowRunnable");
        }
    };

    private Runnable landHideRunnable = new Runnable() {
        @Override
        public void run() {
            setLoadingState(null, null);
            if (livePlayState == PLAY_STATE_PLAYING) {
                layoutC.setVisibility(INVISIBLE);
            }
            streamSwitcher.setVisibility(GONE);
            YoYo.with(Techniques.SlideOutUp)
                    .duration(200)
                    .playOn(layoutA);
            YoYo.with(Techniques.SlideOutDown)
                    .duration(200)
                    .playOn(layoutD);
            YoYo.with(Techniques.SlideOutDown)
                    .duration(200)
                    .playOn(layoutE);
        }
    };

    private Runnable landShowRunnable = new Runnable() {
        @Override
        public void run() {
            setLoadingState(null, null);
            if (livePlayState == PLAY_STATE_PLAYING) {
                layoutC.setVisibility(VISIBLE);
            }
            streamSwitcher.setVisibility(livePlayState == PLAY_STATE_PLAYING &&
                    JFGRules.showSdHd(pid, cVersion) ? VISIBLE : GONE);
            YoYo.with(Techniques.SlideInDown)
                    .duration(250)
                    .playOn(layoutA);
            if (!layoutD.isShown()) layoutD.setVisibility(VISIBLE);//
            YoYo.with(Techniques.SlideInUp)
                    .duration(250)
                    .playOn(layoutD);
            if (!layoutE.isShown()) layoutE.setVisibility(VISIBLE);//
            YoYo.with(Techniques.SlideInUp)
                    .duration(250)
                    .playOn(layoutE);
            postDelayed(landHideRunnable, 3000);
        }
    };

    private Runnable landShowOrHideRunnable = new Runnable() {

        @Override
        public void run() {
            float t = layoutA.getTranslationY();
            if (layoutA.getTranslationY() != 0) {
                if (t == -layoutA.getMeasuredHeight()) {
                    //显示
                    removeCallbacks(landShowRunnable);
                    removeCallbacks(landHideRunnable);
                    post(landShowRunnable);
                    Log.e(TAG, "点击 显示");
                }
            } else {
                //横屏,隐藏
                removeCallbacks(landShowRunnable);
                removeCallbacks(landHideRunnable);
                post(landHideRunnable);
                Log.e(TAG, "点击 隐藏");
            }
        }
    };

    @Override
    public void onLiveStart(CamLiveContract.Presenter presenter, Device device) {
        livePlayType = presenter.getPlayType();
        livePlayState = PLAY_STATE_PLAYING;
        boolean isPlayHistory = livePlayType == TYPE_HISTORY;
        //左下角直播,竖屏下:左下角按钮已经隐藏
        ((ImageView) findViewById(R.id.imgV_cam_live_land_play))
                .setImageResource(isPlayHistory ? R.drawable.icon_landscape_playing :
                        R.drawable.icon_landscape_stop_disable);
        findViewById(R.id.imgV_cam_live_land_play).setEnabled(isPlayHistory);
        //|直播| 按钮
        View tvLive = layoutE.findViewById(R.id.tv_live);
        if (tvLive != null) tvLive.setEnabled(isPlayHistory);
        findViewById(R.id.imgV_cam_trigger_capture).setEnabled(true);
        findViewById(R.id.imgV_land_cam_trigger_capture).setEnabled(true);
        //直播
        findViewById(R.id.tv_live).setEnabled(livePlayType == TYPE_HISTORY);
        liveViewWithThumbnail.onLiveStart();
        findViewById(R.id.imgV_cam_zoom_to_full_screen).setEnabled(true);
        post(portShowRunnable);
    }


    private void setLoadingState(String content, String subContent) {
        layoutC.setState(livePlayState, content, subContent);
        if (!TextUtils.isEmpty(content) || !TextUtils.isEmpty(subContent))
            layoutC.setVisibility(VISIBLE);
        switch (livePlayState) {
            case PLAY_STATE_LOADING_FAILED:
            case PLAY_STATE_STOP:
            case PLAY_STATE_PREPARE:
                layoutC.setVisibility(VISIBLE);
                break;
            case PLAY_STATE_IDLE:
                layoutC.setVisibility(INVISIBLE);
                break;
        }
    }

    @Override
    public void onLiveStop(CamLiveContract.Presenter presenter, Device device, int errCode) {
        livePlayState = presenter.getPlayState();
        layoutB.setVisibility(GONE);
        View vLandPlay = layoutE.findViewById(R.id.imgV_cam_live_land_play);
        if (vLandPlay != null)
            ((ImageView) vLandPlay).setImageResource(R.drawable.icon_landscape_stop);
        findViewById(R.id.v_live).setEnabled(true);
        liveViewWithThumbnail.showFlowView(false, null);
        findViewById(R.id.imgV_cam_zoom_to_full_screen).setEnabled(false);
        removeCallbacks(portHideRunnable);
        handlePlayErr(presenter, errCode);
        findViewById(R.id.imgV_land_cam_trigger_capture).setEnabled(false);
        findViewById(R.id.imgV_cam_trigger_capture).setEnabled(false);
        liveViewWithThumbnail.onLiveStop();
    }

    /**
     * 错误码 需要放在一个Map里面管理
     *
     * @param errCode
     */
    private void handlePlayErr(CamLiveContract.Presenter presenter, int errCode) {
        if (presenter.isDeviceStandby()) return;
        switch (errCode) {//这些errCode 应当写在一个map中.Map<Integer,String>
            case JFGRules.PlayErr.ERR_NETWORK:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.OFFLINE_ERR_1), getContext().getString(R.string.USER_HELP));
                break;
            case JFGRules.PlayErr.ERR_UNKOWN:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.NO_NETWORK_2), null);
                break;
            case JFGRules.PlayErr.ERR_LOW_FRAME_RATE:
                int net = NetUtils.getJfgNetType(getContext());
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.GLOBAL_NO_NETWORK), net == 0 ? getContext().getString(R.string.USER_HELP) : null);
                break;
            case STOP_MAUNALLY:
            case PLAY_STATE_STOP:
                livePlayState = PLAY_STATE_STOP;
                setLoadingState(null, null);
                break;
            case JFGRules.PlayErr.ERR_NOT_FLOW:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.NETWORK_TIMEOUT), getContext().getString(R.string.USER_HELP));
                break;
            case JError.ErrorVideoPeerDisconnect:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.Device_Disconnected), null);
                break;
            case JFGRules.PlayErr.ERR_DEVICE_OFFLINE:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.OFFLINE_ERR), getContext().getString(R.string.USER_HELP));
                break;
            case JError.ErrorVideoPeerNotExist:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.OFFLINE_ERR), getContext().getString(R.string.USER_HELP));
                break;
            case JError.ErrorVideoPeerInConnect:
                //正在直播...
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.CONNECTING), null);
                break;
            case PLAY_STATE_IDLE:
                livePlayState = PLAY_STATE_IDLE;
                setLoadingState(null, null);
                break;
            case PLAY_STATE_NET_CHANGED:
                livePlayState = PLAY_STATE_PREPARE;
                setLoadingState(null, null);
                break;
            case JError.ErrorSDHistoryAll:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.Historical_Read), null);
                if (getContext() instanceof Activity)
                    AlertDialogManager.getInstance().showDialog((Activity) getContext(),
                            getContext().getString(R.string.Historical_Read),
                            getContext().getString(R.string.Historical_Read),
                            getContext().getString(R.string.OK), (DialogInterface dialog, int which) -> {
                                CamLiveContract.LiveStream prePlayType = presenter.getLiveStream();
                                prePlayType.type = TYPE_LIVE;
                                presenter.updateLiveStream(prePlayType);
                                presenter.startPlay();
                            });
                break;
            case JError.ErrorSDFileIO:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.Historical_Failed), null);
                if (getContext() instanceof Activity)
                    AlertDialogManager.getInstance().showDialog((Activity) getContext(),
                            getContext().getString(R.string.Historical_Failed),
                            getContext().getString(R.string.Historical_Failed),
                            getContext().getString(R.string.OK), (DialogInterface dialog, int which) -> {
                                CamLiveContract.LiveStream prePlayType = presenter.getLiveStream();
                                prePlayType.type = TYPE_LIVE;
                                presenter.updateLiveStream(prePlayType);
                                presenter.startPlay();
                            });
                break;
            case JError.ErrorSDIO:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.Historical_No), null);
                if (getContext() instanceof Activity)
                    AlertDialogManager.getInstance().showDialog((Activity) getContext(),
                            getContext().getString(R.string.Historical_No),
                            getContext().getString(R.string.Historical_No),
                            getContext().getString(R.string.OK), (DialogInterface dialog, int which) -> {
                                CamLiveContract.LiveStream prePlayType = presenter.getLiveStream();
                                prePlayType.type = TYPE_LIVE;
                                presenter.updateLiveStream(prePlayType);
                                presenter.startPlay();
                            });
                break;
            default:
                livePlayState = PLAY_STATE_LOADING_FAILED;
                setLoadingState(getContext().getString(R.string.GLOBAL_NO_NETWORK), null);
                break;
        }
    }

    @Override
    public void orientationChanged(CamLiveContract.Presenter presenter, Device device, int orientation) {
        int playType = presenter.getPlayType();
        boolean isLand = isLand();
        layoutA.setVisibility(isLand ? VISIBLE : INVISIBLE);
        layoutF.setVisibility(isLand ? INVISIBLE : VISIBLE);
        //历史录像显示
        boolean showFlip = !presenter.isShareDevice() && JFGRules.hasProtection(device.pid);
        findViewById(R.id.layout_land_flip).setVisibility(showFlip && isLand ? VISIBLE : GONE);
//        findViewById(R.id.v_divider).setVisibility(showFlip && isLand ? VISIBLE : INVISIBLE);
        liveViewWithThumbnail.detectOrientationChanged(!isLand);
        //直播
        findViewById(R.id.tv_live).setEnabled(playType == TYPE_HISTORY);
        @SuppressLint("WrongViewCast") RelativeLayout.LayoutParams lp = (LayoutParams) layoutE.getLayoutParams();
        if (isLand) {
            lp.removeRule(3);//remove below rules
            lp.addRule(2, R.id.v_guide);//set above v_guide
            liveViewWithThumbnail.updateLayoutParameters(LayoutParams.MATCH_PARENT, getVideoFinalWidth());
            findViewById(R.id.imgV_cam_zoom_to_full_screen).setVisibility(INVISIBLE);
            layoutD.setBackgroundResource(android.R.color.transparent);
            layoutE.setBackgroundResource(R.color.color_4C000000);
            findViewById(R.id.layout_port_flip).setVisibility(INVISIBLE);
            //显示 昵称
            String alias = TextUtils.isEmpty(device.alias) ? device.uuid : device.alias;
            ((TextView) findViewById(R.id.imgV_cam_live_land_nav_back))
                    .setText(alias);
            findViewById(R.id.imgV_cam_live_land_play).setVisibility(VISIBLE);
        } else {
            findViewById(R.id.imgV_cam_live_land_play).setVisibility(GONE);
            lp.removeRule(2);//remove above
            lp.addRule(3, R.id.v_guide); //set below v_guide
            findViewById(R.id.imgV_cam_zoom_to_full_screen).setVisibility(VISIBLE);
            float ratio = isNormalView ? presenter.getVideoPortHeightRatio() : 1.0f;
            updateLiveViewRectHeight(ratio);
            //有条件的.
            if (presenter.getPlayState() == PLAY_STATE_PLAYING) {
                //需要根据设备属性表来决定是否显示或隐藏 portFlip
                findViewById(R.id.layout_port_flip).setVisibility(showFlip ? VISIBLE : INVISIBLE);
            }
            layoutD.setBackgroundResource(R.drawable.camera_sahdow);
            layoutE.setBackgroundResource(android.R.color.transparent);
            layoutG.setVisibility(GONE);
            if (historyWheelHandler != null) historyWheelHandler.onBackPress();
        }

        //需要根据设备属性表来决定是否显示和隐藏加载历史视频的按钮
        View btn = findViewById(R.id.btn_load_history);
        btn.setVisibility(JFGRules.hasHistory(device.pid) ? VISIBLE : GONE);
        layoutE.setLayoutParams(lp);
        resetAndPrepareNextAnimation(isLand);
    }

    private int getVideoFinalWidth() {
        if (MiscUtils.isLand()) {
            //横屏需要区分睿视
            if (isRSCam) {
                //保持4:3
                Log.d("isRSCam", "isRSCam....");
                return (int) (Resources.getSystem().getDisplayMetrics().heightPixels * (float) 4 / 3);
            }
            return ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            //竖屏 match
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    private void resetAndPrepareNextAnimation(boolean land) {
        //切换到了横屏,必须先恢复view的位置才能 重新开始动画
        layoutA.setTranslationY(0);
        layoutD.setTranslationY(0);
        layoutE.setTranslationY(0);
        if (land) {
            layoutE.setVisibility(VISIBLE);
            removeCallbacks(portHideRunnable);
            removeCallbacks(landHideRunnable);
            removeCallbacks(landShowRunnable);
            postDelayed(landHideRunnable, 3000);//3s后隐藏
        } else {
            layoutD.setVisibility(VISIBLE);
            removeCallbacks(portHideRunnable);
            removeCallbacks(landHideRunnable);
            removeCallbacks(landShowRunnable);
            post(portShowRunnable);
//            postDelayed(portHideRunnable, 3000);
        }
    }

    @Override
    public void onRtcpCallback(int type, JFGMsgVideoRtcp rtcp) {
        livePlayState = PLAY_STATE_PLAYING;
        String flow = MiscUtils.getByteFromBitRate(rtcp.bitRate);
        liveViewWithThumbnail.showFlowView(true, flow);
        //分享账号不显示啊.
        if (JFGRules.isShareDevice(uuid)) return;
        setLiveRectTime(livePlayType, rtcp.timestamp);
        //点击事件
        if (liveTimeRectListener == null) {
            liveTimeRectListener = v -> {
                int net = NetUtils.getJfgNetType();
                if (net == 0) {
                    ToastUtil.showNegativeToast(getContext().getString(R.string.NoNetworkTips));
                    return;
                }
                Device device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
                if (!JFGRules.isDeviceOnline(device.$(201, new DpMsgDefine.DPNet()))) {
                    ToastUtil.showNegativeToast(getContext().getString(R.string.OFFLINE_ERR));
                    return;
                }
                DpMsgDefine.DPSdStatus status = device.$(204, new DpMsgDefine.DPSdStatus());

                if (status.hasSdcard && status.err != 0) {
                    ToastUtil.showNegativeToast(getContext().getString(R.string.VIDEO_SD_DESC));
                    return;
                }
                if (!status.hasSdcard || status.err != 0) {
                    ToastUtil.showNegativeToast(getContext().getString(R.string.has_not_sdcard));
                    return;
                }
                if (historyWheelHandler == null || presenter.getHistoryDataProvider() == null ||
                        presenter.getHistoryDataProvider().getDataCount() == 0) {
                    ToastUtil.showToast(getResources().getString(R.string.History_video_Firstly));
                    return;
                }
                if (historyWheelHandler != null) {
                    ViewUtils.deBounceClick(v);
                    historyWheelHandler.showDatePicker(MiscUtils.isLand());
                }
            };
            (layoutD.findViewById(R.id.live_time_layout)).setOnClickListener(liveTimeRectListener);
        }
    }


    private void setLiveRectTime(int type, long timestamp) {
        //全景的时间戳是0,使用设备的时区
        //wifi狗是格林尼治时间戳,需要-8个时区.
        String content = String.format(getContext().getString(type == 1 ? R.string.Tap1_Camera_VideoLive : R.string.Tap1_Camera_Playback)
                + "|%s", getTime(timestamp == 0 || type == 1 ? System.currentTimeMillis() : timestamp * 1000L));
        ((LiveTimeLayout) layoutD.findViewById(R.id.live_time_layout))
                .setContent(content);
        if (type == TYPE_HISTORY && timestamp != 0 && presenter != null && presenter.getPlayState() == PLAY_STATE_PLAYING) {
            //移动导航条
            Log.d("TYPE_HISTORY time", "time: " + timestamp);
//            historyWheelHandler.setNav2Time(timestamp * 1000);
        }
    }

    private String getTime(long time) {
        return liveTimeDateFormat.format(new Date(time));
    }

    public void setFlipListener(FlipImageView.OnFlipListener flipListener) {
        ((FlipLayout) findViewById(R.id.layout_land_flip)).setFlipListener(flipListener);
        ((FlipLayout) findViewById(R.id.layout_port_flip)).setFlipListener(flipListener);
    }

    public void setFlipped(boolean flip) {
        ((FlipLayout) findViewById(R.id.layout_land_flip)).setFlipped(flip);
        ((FlipLayout) findViewById(R.id.layout_port_flip)).setFlipped(flip);
    }


    @Override
    public void onResolutionRsp(JFGMsgVideoResolution resolution) {
        try {
            BaseApplication.getAppComponent().getCmd().enableRenderSingleRemoteView(true, (View) liveViewWithThumbnail.getVideoView());
        } catch (JfgException e) {
            AppLogger.e("err:" + MiscUtils.getErr(e));
        }
        float ratio;
        ratio = isNormalView ? (float) resolution.height / resolution.width :
                isLand() ? (float) Resources.getSystem().getDisplayMetrics().heightPixels /
                        Resources.getSystem().getDisplayMetrics().widthPixels : 1.0f;
        updateLiveViewRectHeight(ratio);
    }

    /**
     * 分辨率 (float)h/w
     *
     * @param ratio
     */
    private void updateLiveViewRectHeight(float ratio) {
        liveViewWithThumbnail.updateLayoutParameters((int) (Resources.getSystem().getDisplayMetrics().widthPixels * ratio),
                getVideoFinalWidth());
    }

    @Override
    public void onHistoryDataRsp(CamLiveContract.Presenter presenter) {
        showHistoryWheel(true);
        reInitHistoryHandler(presenter);
        historyWheelHandler.dateUpdate();
        historyWheelHandler.setDatePickerListener((time, state) -> {
            //选择时间,更新时间区域
            post(() -> {
                setLiveRectTime(TYPE_HISTORY, time);//wheelView 回调的是毫秒时间, rtcp 回调的是秒,这里要除以1000
                prepareLayoutDAnimation(state == STATE_FINISH);
            });
        });
        findViewById(R.id.tv_cam_live_land_bottom).setVisibility(VISIBLE);
    }

    private void reInitHistoryHandler(CamLiveContract.Presenter presenter) {
        if (historyWheelHandler == null) {
            historyWheelHandler = new HistoryWheelHandler((ViewGroup) layoutG, superWheelExt, presenter);
        }
        Device device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
        historyWheelHandler.setTimeZone(JFGRules.getDeviceTimezone(device));
    }

    @Override
    public void onLiveDestroy() {
        //1.live view pause
        try {
            liveViewWithThumbnail.getVideoView().onPause();
            liveViewWithThumbnail.getVideoView().onDestroy();
        } catch (Exception e) {
        }
    }

    @Override
    public void onDeviceStandByChanged(Device device, OnClickListener clickListener) {
        //设置 standby view相关点击事件
        DpMsgDefine.DPStandby standby = device.$(508, new DpMsgDefine.DPStandby());
        DpMsgDefine.DPNet dpNet = device.$(201, new DpMsgDefine.DPNet());//http://yf.cylan.com.cn:82/redmine/issues/109805
        liveViewWithThumbnail.enableStandbyMode(standby.standby && dpNet.net > 0, clickListener, !TextUtils.isEmpty(device.shareAccount));
        if (standby.standby && dpNet.net > 0 && !isLand()) {
            post(portHideRunnable);
            setLoadingState(null, null);
        }
    }

    private boolean isStandBy() {
        Device device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
        DpMsgDefine.DPStandby standby = device.$(508, new DpMsgDefine.DPStandby());
        return standby.standby;
    }

    @Override
    public void onLoadPreviewBitmap(Bitmap bitmap) {
//        post(() -> liveViewWithThumbnail.setThumbnail(getContext(), PreferencesUtils.getString(JConstant.KEY_UUID_PREVIEW_THUMBNAIL_TOKEN + uuid, ""), bitmap));
    }

    @Override
    public void onCaptureRsp(FragmentActivity activity, Bitmap bitmap) {
        if (MiscUtils.isLand()) return;
        try {
            PerformanceUtils.startTrace("showPopupWindow");
            roundCardPopup = new RoundCardPopup(getContext(), view -> {
                view.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
            }, v -> {
                roundCardPopup.dismiss();
                Bundle bundle = new Bundle();
                bundle.putParcelable(JConstant.KEY_SHARE_ELEMENT_BYTE, bitmap);
                bundle.putString(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
//                if (isNormalView) {
                NormalMediaFragment fragment = NormalMediaFragment.newInstance(bundle);
                ActivityUtils.addFragmentSlideInFromRight(activity.getSupportFragmentManager(), fragment,
                        android.R.id.content);
                fragment.setCallBack(t -> activity.getSupportFragmentManager().popBackStack());
//                } else {
//                    PanoramicViewFragment fragment = PanoramicViewFragment.newInstance(bundle);
//                    ActivityUtils.addFragmentSlideInFromRight(activity.getSupportFragmentManager(), fragment,
//                            android.R.id.content);
//                    fragment.setCallBack(t -> activity.getSupportFragmentManager().popBackStack());
//                }
            });
            roundCardPopup.setAutoDismissTime(5 * 1000L);
            roundCardPopup.showOnAnchor(findViewById(R.id.imgV_cam_trigger_capture), RelativePopupWindow.VerticalPosition.ABOVE, RelativePopupWindow.HorizontalPosition.CENTER);
        } catch (Exception e) {
            AppLogger.e("showPopupWindow: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void setLoadingRectAction(ILiveControl.Action action) {
        this.action = action;
        (layoutC).setAction(this.action);
    }

    @Override
    public void onNetworkChanged(CamLiveContract.Presenter presenter, boolean connected) {
        if (!connected) {
            post(() -> {
                showHistoryWheel(false);
                handlePlayErr(presenter, JFGRules.PlayErr.ERR_NETWORK);
            });
        }
    }

    @Override
    public void onActivityStart(CamLiveContract.Presenter presenter, Device device) {
        boolean safeIsOpen = device.$(ID_501_CAMERA_ALARM_FLAG, false);
        removeCallbacks(portHideRunnable);
        setFlipped(!safeIsOpen);
        updateLiveViewMode(device.$(509, "1"));
        DpMsgDefine.DPNet net = device.$(201, new DpMsgDefine.DPNet());
        if (!JFGRules.isDeviceOnline(net)) return;//设备离线,不需要显示了
        Bitmap bitmap = SimpleCache.getInstance().getSimpleBitmapCache(presenter.getThumbnailKey());
        if (bitmap == null || bitmap.isRecycled()) {
            File file = new File(presenter.getThumbnailKey());
            liveViewWithThumbnail.setThumbnail(getContext(), PreferencesUtils.getString(JConstant.KEY_UUID_PREVIEW_THUMBNAIL_TOKEN + uuid, ""), Uri.fromFile(file));
        } else
            liveViewWithThumbnail.setThumbnail(getContext(), PreferencesUtils.getString(JConstant.KEY_UUID_PREVIEW_THUMBNAIL_TOKEN + uuid, ""), SimpleCache.getInstance().getSimpleBitmapCache(presenter.getThumbnailKey()));
        TimeZone timeZone = JFGRules.getDeviceTimezone(device);
        liveTimeDateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.UK);
        liveTimeDateFormat.setTimeZone(timeZone);
        AppLogger.d("得到设备时区:" + timeZone.getID() + "," + timeZone.getDisplayName());
        setHotSeatState(PLAY_STATE_STOP, false, false, false, false, false, false);
    }

    @Override
    public void onActivityResume(CamLiveContract.Presenter presenter, Device device, boolean isUserVisible) {
        final boolean judge = !isSightShow() && !isStandBy();
        Log.d("judge", "judge: " + judge);
        handler.postDelayed(() -> {
            livePlayState = judge ? PLAY_STATE_STOP : PLAY_STATE_IDLE;
            setLoadingState(null, null);
            layoutD.setVisibility(!judge ? INVISIBLE : VISIBLE);
//            showUseCase();
            if (!isUserVisible) return;
            showUseCase();
        }, 100);
    }

    public void showUseCase() {
        if (presenter.isDeviceStandby()) return;


        JFGSourceManager sourceManager = BaseApplication.getAppComponent().getSourceManager();
        Device device = sourceManager.getDevice(uuid);
        if (JFGRules.hasHistory(device.pid)) {
            LiveShowCase.showHistoryCase((Activity) getContext(), findViewById(R.id.imgV_cam_zoom_to_full_screen));
            //是否显示 历史视频使用引导 "遮罩"
        }
        if (JFGRules.hasProtection(device.pid)) {
            LiveShowCase.showSafeCase((Activity) getContext(), layoutD);
        }
    }

    @Override
    public void updateLiveViewMode(String mode) {
        liveViewWithThumbnail.getVideoView().config360(TextUtils.equals(mode, "0") ? CameraParam.getTopPreset() : CameraParam.getWallPreset());
        liveViewWithThumbnail.getVideoView().setMode(TextUtils.equals("0", mode) ? 0 : 1);
        liveViewWithThumbnail.getVideoView().detectOrientationChanged();
    }

    private int[] portMicRes = {R.drawable.icon_port_mic_off_selector,
            R.drawable.icon_port_mic_on_selector};
    private int[] landMicRes = {R.drawable.icon_land_mic_off_selector,
            R.drawable.icon_land_mic_on_selector};
    private int[] portSpeakerRes = {R.drawable.icon_port_speaker_off_selector,
            R.drawable.icon_port_speaker_on_selector,
            R.drawable.icon_port_speaker_off_selector, R.drawable.icon_port_speaker_on_selector};
    private int[] landSpeakerRes = {R.drawable.icon_land_speaker_off_selector,
            R.drawable.icon_land_speaker_on_selector,
            R.drawable.icon_land_speaker_off_selector, R.drawable.icon_land_speaker_on_selector};


    /***
     * 三个按钮的状态,不能根据UI的状态来辨别.
     * 反而UI需要根据这个状态来辨别.
     * speaker|mic|capture
     * 用3个byte表示:
     * |0(高位表示1:开,0:关)0(低位表示1:enable,0:disable)|00|00|
     */
    @Override
    public void setHotSeatState(int liveType, boolean speaker,
                                boolean speakerEnable,
                                boolean mic,
                                boolean micEnable,
                                boolean capture, boolean captureEnable) {
        ImageView pMic = (ImageView) findViewById(R.id.imgV_cam_trigger_mic);
        pMic.setEnabled(micEnable);
        pMic.setImageResource(portMicRes[mic ? 1 : 0]);
        ImageView lMic = (ImageView) findViewById(R.id.imgV_land_cam_trigger_mic);
        lMic.setEnabled(micEnable);
        lMic.setImageResource(landMicRes[mic ? 1 : 0]);
        //speaker
        ImageView pSpeaker = (ImageView) findViewById(R.id.imgV_cam_switch_speaker);
        pSpeaker.setEnabled(speakerEnable);
        pSpeaker.setImageResource(portSpeakerRes[speaker ? 1 : 0]);
        ImageView lSpeaker = (ImageView) findViewById(R.id.imgV_land_cam_switch_speaker);
        lSpeaker.setEnabled(speakerEnable);
        lSpeaker.setImageResource(landSpeakerRes[speaker ? 1 : 0]);
        //capture
        //只有 enable和disable
        ImageView pCapture = (ImageView) findViewById(R.id.imgV_cam_trigger_capture);
        pCapture.setEnabled(captureEnable);
        ImageView lCapture = (ImageView) findViewById(R.id.imgV_land_cam_trigger_capture);
        lCapture.setEnabled(captureEnable);
        Log.d(TAG, String.format(Locale.getDefault(), "hotSeat:speaker:%s,speakerEnable:%s,mic:%s,micEnable:%s", speaker, speakerEnable, mic, micEnable));
    }

    @Override
    public void setHotSeatListener(OnClickListener micListener,
                                   OnClickListener speakerListener,
                                   OnClickListener captureListener) {
        findViewById(R.id.imgV_cam_switch_speaker).setOnClickListener(speakerListener);
        findViewById(R.id.imgV_land_cam_switch_speaker).setOnClickListener(speakerListener);
        findViewById(R.id.imgV_cam_trigger_mic).setOnClickListener(micListener);
        findViewById(R.id.imgV_land_cam_trigger_mic).setOnClickListener(micListener);
        findViewById(R.id.imgV_cam_trigger_capture).setOnClickListener(captureListener);
        findViewById(R.id.imgV_land_cam_trigger_capture).setOnClickListener(captureListener);
    }

    @Override
    public int getMicState() {
        Object o = findViewById(R.id.imgV_land_cam_trigger_mic).getTag();
        if (o != null && o instanceof Integer) {
            int tag = (int) o;
            switch (tag) {
                case R.drawable.icon_land_mic_on_selector:
                    if (findViewById(R.id.imgV_land_cam_trigger_mic).isEnabled()) {
                        return 3;
                    } else return 1;
                case R.drawable.icon_land_mic_off_selector:
                    if (findViewById(R.id.imgV_land_cam_trigger_mic).isEnabled()) {
                        return 2;
                    } else return 0;
            }
        }
        return 0;
    }

    @Override
    public int getSpeakerState() {
        Object o = findViewById(R.id.imgV_land_cam_switch_speaker).getTag();
        if (o != null && o instanceof Integer) {
            int tag = (int) o;
            switch (tag) {
                case R.drawable.icon_land_speaker_on_selector:
                    if (findViewById(R.id.imgV_cam_switch_speaker).isEnabled()) {
                        return 3;
                    } else return 1;
                case R.drawable.icon_land_speaker_off_selector:
                    if (findViewById(R.id.imgV_cam_switch_speaker).isEnabled()) {
                        return 2;
                    } else return 0;
            }
        }
        return 0;
    }

    @Override
    public void resumeGoodFrame() {
        livePlayState = PLAY_STATE_PLAYING;
        setLoadingState(null, null);
        findViewById(R.id.imgV_cam_zoom_to_full_screen).setEnabled(true);
        //0:off-disable,1.on-disable,2.off-enable,3.on-enable
        if (livePlayType == TYPE_HISTORY) {
            findViewById(R.id.imgV_land_cam_trigger_mic).setEnabled(false);
            findViewById(R.id.imgV_cam_trigger_mic).setEnabled(false);
        }
        findViewById(R.id.imgV_cam_trigger_capture).setEnabled(true);
        findViewById(R.id.imgV_land_cam_trigger_capture).setEnabled(true);
    }

    @Override
    public void startBadFrame() {
        livePlayState = PLAY_STATE_PREPARE;
        setLoadingState(null, null);
        findViewById(R.id.imgV_cam_zoom_to_full_screen).setEnabled(false);
    }

    @Override
    public void reAssembleHistory(CamLiveContract.Presenter presenter, final long timeTarget) {
        long timeStart = TimeUtils.getSpecificDayStartTime(timeTarget);
        //先loading吧.
        presenter.startPlayHistory(timeTarget);
        presenter.assembleTheDay()
                .subscribeOn(Schedulers.io())
                .filter(iData -> iData != null)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> AppLogger.d("reLoad hisData: good"))
                .subscribe(iData -> {
                    HistoryWheelHandler handler = getHistoryWheelHandler(presenter);
                    AppLogger.d("历史录像导航条空?" + (handler == null));
                    if (handler != null) {
                        handler.setupHistoryData(iData);
                        handler.setNav2Time(timeTarget);
                        setLiveRectTime(TYPE_HISTORY, timeTarget / 1000);
                        AppLogger.d("目标历史录像时间?" + timeTarget);
                    }
                }, throwable -> AppLogger.e("err:" + MiscUtils.getErr(throwable)));
    }

    @Override
    public void showMobileDataCover(CamLiveContract.Presenter presenter) {
        AppLogger.d("显示遮罩");
        liveViewWithThumbnail.showMobileDataInterface(v -> {
            presenter.startPlay();
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.imgV_cam_live_land_nav_back:
                post(() -> ViewUtils.setRequestedOrientation((Activity) getContext(),
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
                break;
            case R.id.imgV_cam_zoom_to_full_screen://点击全屏
                post(() -> ViewUtils.setRequestedOrientation((Activity) getContext(),
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                break;
            case R.id.imgV_cam_live_land_play://横屏,左下角播放
                if (playClickListener != null) playClickListener.onClick(v);
                break;
            case R.id.tv_live://直播中,按钮disable.历史录像:enable
                if (liveTextClick != null) liveTextClick.onClick(v);
                break;
            case R.id.imgV_cam_switch_speaker:
            case R.id.imgV_land_cam_switch_speaker:
                break;
            case R.id.imgV_cam_trigger_mic:
            case R.id.imgV_land_cam_trigger_mic:
                break;
            case R.id.imgV_cam_trigger_capture:
            case R.id.imgV_land_cam_trigger_capture:
                break;
        }
    }

    public void setLoadingState(int state, String content) {
        setLoadingState(content, null);
    }


    public void setPlayBtnListener(OnClickListener clickListener) {
        this.playClickListener = clickListener;
    }

    public void setLiveTextClick(OnClickListener liveTextClick) {
        this.liveTextClick = liveTextClick;
    }

}
