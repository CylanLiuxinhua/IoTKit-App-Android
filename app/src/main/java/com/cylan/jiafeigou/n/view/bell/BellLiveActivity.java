package com.cylan.jiafeigou.n.view.bell;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.cylan.entity.jniCall.JFGMsgVideoResolution;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.NewHomeActivity;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.BaseFullScreenActivity;
import com.cylan.jiafeigou.base.injector.component.ActivityComponent;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.base.view.CallablePresenter;
import com.cylan.jiafeigou.base.view.JFGSourceManager;
import com.cylan.jiafeigou.cache.db.module.Device;
import com.cylan.jiafeigou.misc.AlertDialogManager;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JError;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.n.base.BaseApplication;
import com.cylan.jiafeigou.n.mvp.contract.bell.BellLiveContract;
import com.cylan.jiafeigou.n.view.media.NormalMediaFragment;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.ActivityUtils;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.MiscUtils;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.utils.ViewUtils;
import com.cylan.jiafeigou.widget.CustomToolbar;
import com.cylan.jiafeigou.widget.bell.DragLayout;
import com.cylan.jiafeigou.widget.dialog.BaseDialog;
import com.cylan.jiafeigou.widget.dialog.SimpleDialogFragment;
import com.cylan.jiafeigou.widget.glide.RoundedCornersTransformation;
import com.cylan.jiafeigou.widget.live.ILiveControl;
import com.cylan.jiafeigou.widget.pop.RelativePopupWindow;
import com.cylan.jiafeigou.widget.pop.RoundCardPopup;
import com.cylan.jiafeigou.widget.video.VideoViewFactory;
import com.cylan.panorama.CameraParam;
import com.cylan.panorama.Panoramic360View;
import com.cylan.panorama.Panoramic360ViewRS;
import com.jakewharton.rxbinding.view.RxView;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_IDLE;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_LOADING_FAILED;
import static com.cylan.jiafeigou.misc.JConstant.PLAY_STATE_PREPARE;

@RuntimePermissions
public class BellLiveActivity extends BaseFullScreenActivity<BellLiveContract.Presenter>
        implements DragLayout.OnDragReleaseListener, View.OnClickListener
        , BellLiveContract.View, ILiveControl.Action {

    @BindView(R.id.fLayout_bell_live_holder)
    FrameLayout fLayoutBellLiveHolder;
    @BindView(R.id.tv_bell_live_flow)
    TextView mBellFlow;
    @BindView(R.id.imgv_bell_live_switch_to_land)
    ImageView imgvBellLiveSwitchToLand;
    @BindView(R.id.dLayout_bell_hot_seat)
    DragLayout dLayoutBellHotSeat;
    @BindView(R.id.imgv_bell_live_capture)
    ImageView imgvBellLiveCapture;
    @BindView(R.id.imgv_bell_live_hang_up)
    ImageView imgvBellLiveHangUp;
    @BindView(R.id.imgv_bell_live_speaker)
    ImageView imgvBellLiveSpeaker;
    @BindView(R.id.fLayout_bell_after_live)
    FrameLayout fLayoutBellAfterLive;
    @BindView(R.id.act_bell_live_video_picture)
    ImageView mBellLiveVideoPicture;
    @BindView(R.id.act_bell_live_video_view_container)
    FrameLayout mVideoViewContainer;
    @BindView(R.id.act_bell_live_video_play_controller)
    ILiveControl mVideoPlayController;
    //    @BindView(R.id.act_bell_live_back)
//    TextView mBellLiveBack;
    @BindView(R.id.view_bell_handle)
    ImageView mBellhandle;
    private ImageView mLandBellLiveSpeaker;

    @BindView(R.id.custom_toolbar)
    CustomToolbar customToolbar;
    @BindView(R.id.act_bell_live_back)
    TextView landBack;
    /**
     * 水平方向的view
     */
    private WeakReference<View> fLayoutLandHolderRef;

    private SurfaceView mSurfaceView;

    private String mNewCallHandle;

    private String mLiveTitle = "宝宝的房间";

    private boolean isLandMode = false;
    //    private boolean isLanchFromBellCall = false;
    private MediaPlayer mediaPlayer;
    private RoundCardPopup roundCardPopup;
    @Inject
    JFGSourceManager sourceManager;
    private float ratio;

    private boolean isAnswerd = false;
    private boolean isSpeakerON = false;
    private boolean onUserLeaveHint = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getContentViewID() {
        return R.layout.activity_bell_live;
    }

    @Override
    protected void initViewAndListener() {
        //锁屏状态下显示门铃呼叫
        registSreenStatusReceiver();
        initHeadSetEventReceiver();
        initHomeListenReceiver();
        Device device = sourceManager.getDevice(uuid);
        if (device != null) {
            mLiveTitle = TextUtils.isEmpty(device.alias) ? device.uuid : device.alias;
        }
        customToolbar.setToolbarLeftTitle(mLiveTitle);
        dLayoutBellHotSeat.setOnDragReleaseListener(this);
        mVideoPlayController.setAction(this);
        customToolbar.setBackAction(this::onBack);
        newCall();
    }

    private void initHomeListenReceiver() {
        homeListen = new HomeListen();
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(homeListen, homeFilter);
    }

    @OnClick(R.id.act_bell_live_back)
    void onBack(View view) {
        super.onBackPressed();
    }

    private void initHeadSetEventReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetPlugReceiver, intentFilter);
    }

    private BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                AppLogger.d("收到耳机事件");
                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        AppLogger.d("耳机以移除");
                        BellLiveActivityPermissionsDispatcher.handleHeadsetDisconnectedWithCheck(BellLiveActivity.this);
                    } else if (intent.getIntExtra("state", 0) == 1) {
                        AppLogger.e("耳机已连接");
                        BellLiveActivityPermissionsDispatcher.handleHeadsetConnectedWithCheck(BellLiveActivity.this);
                    }
                }
            }
        }

    };

    @NeedsPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)
    void handleHeadsetConnected() {
        AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        manager.setSpeakerphoneOn(false);

    }

    @OnPermissionDenied(Manifest.permission.MODIFY_AUDIO_SETTINGS)
    void onModifyAudioSettingFaild() {
        AppLogger.d("切换模式失败");
    }

    @NeedsPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)
    void handleHeadsetDisconnected() {
        AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        manager.setSpeakerphoneOn(true);
    }

    public void hideStatusBar() {
        mVideoViewContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    @Override
    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
////        setIntent(intent);//直接無視新的呼叫
////        newCall();
//        finish();
//        startActivity(intent);
//        parse(intent);
    }

    private void parse(Intent intent) {
        String uuid = intent.getStringExtra(JConstant.KEY_DEVICE_ITEM_UUID);
        String url = intent.getStringExtra(JConstant.VIEW_CALL_WAY_EXTRA);
        if (TextUtils.equals(uuid, this.uuid) && url != null) {
            presenter.loadPreview(url);
        }
    }

    private void newCall() {
        onSpeaker(false);
        long time = getIntent().getLongExtra(JConstant.VIEW_CALL_WAY_TIME, System.currentTimeMillis());
        CallablePresenter.Caller caller = new CallablePresenter.Caller();
        caller.caller = uuid;
        caller.callTime = time;
        presenter.newCall(caller);
        parse(getIntent());

    }

    @Override
    protected void onStart() {
        super.onStart();
        onUserLeaveHint = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(1, 1);
        }
        landBack.setVisibility(isLand() ? View.VISIBLE : View.GONE);
        customToolbar.setVisibility(isLand() ? View.GONE : View.VISIBLE);
        muteAudio(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(0, 0);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        this.onUserLeaveHint = true;
//        if (presenter.getLiveAction().hasStarted) {
//            presenter.dismiss();
//        }
//        // TODO: 2017/8/31 home键,最近任务键 会调用这个,但是系统权限弹窗也会调用这个,不好判断
//        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        muteAudio(false);

        if (presenter.getLiveAction().hasStarted || onUserLeaveHint) {
            if (mSurfaceView != null && mSurfaceView instanceof GLSurfaceView) {
                ((GLSurfaceView) mSurfaceView).onPause();
                mVideoViewContainer.removeAllViews();
                mSurfaceView = null;
                AppLogger.d("finish manually");
            }
            presenter.dismiss();
            finish();//115763 //门铃呼叫 弹出呼叫界面后，退到后台/打开其他软件时，再返回app时，需要断开门铃弹窗
        }
    }


    private void clearHeadSetEventReceiver() {
        if (headsetPlugReceiver != null) {
            unregisterReceiver(headsetPlugReceiver);
        }
    }

    @Override
    public void onScreenRotationChanged(boolean land) {
        isLandMode = land;
        handleScreenUpdate(!land);
    }

    @Override
    protected void onPrepareToExit(Action action) {
        presenter.dismiss();
        finishExt();
        action.actionDone();
    }

    private void handleScreenUpdate(final boolean port) {
        initLandView();
        fLayoutLandHolderRef.get().setVisibility(port ? View.GONE : View.VISIBLE);

        if (port) {
            customToolbar.setVisibility(View.VISIBLE);
            landBack.setVisibility(View.GONE);
            ViewUtils.updateViewHeight(fLayoutBellLiveHolder, ratio);
//            mBellLiveBack.setText(null);
            imgvBellLiveSwitchToLand.setVisibility(View.VISIBLE);
            mVideoViewContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        } else {
            hideStatusBar();
//            setHideBackMargin();
            customToolbar.setVisibility(View.GONE);
            landBack.setVisibility(View.VISIBLE);
            ViewUtils.updateViewMatchScreenHeight(fLayoutBellLiveHolder);
//            mBellLiveBack.setText(mLiveTitle);
            imgvBellLiveSwitchToLand.setVisibility(View.GONE);
        }
    }

    public boolean isHeadsetOn() {
        AudioManager am = (AudioManager) ContextUtils.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            AppLogger.d("isVolumeFixed: " + am.isVolumeFixed());
        return am.isWiredHeadsetOn();
    }

    /**
     * 初始化 Layer层view，横屏全屏时候，需要在上层
     */
    private void initLandView() {
        if (fLayoutLandHolderRef == null || fLayoutLandHolderRef.get() == null) {
            View view = LayoutInflater.from(getAppContext())
                    .inflate(R.layout.layout_bell_live_land_layer, null);
            if (view != null) {
                fLayoutLandHolderRef = new WeakReference<>(view);
                view.findViewById(R.id.imgv_bell_live_land_capture)
                        .setOnClickListener(this);
                view.findViewById(R.id.imgv_bell_live_land_hangup)
                        .setOnClickListener(this);
                mLandBellLiveSpeaker = (ImageView) view.findViewById(R.id.imgv_bell_live_land_mic);
                mLandBellLiveSpeaker.setOnClickListener(this);
            }
        }
        View v = fLayoutBellLiveHolder.findViewById(R.id.fLayout_bell_live_land_layer);
        if (v == null) {
            fLayoutBellLiveHolder.addView(fLayoutLandHolderRef.get());
        }

    }

    @Override
    public void onRelease(int side) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (side == 0) {
            presenter.dismiss();
        } else {
            isAnswerd = true;
            presenter.pickup();
        }
    }

    @Override
    public void onLoginState(int state) {

    }


    @OnClick({R.id.imgv_bell_live_capture,
            R.id.imgv_bell_live_hang_up,
            R.id.imgv_bell_live_speaker,
            R.id.imgv_bell_live_switch_to_land,
    })
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgv_bell_live_capture:
            case R.id.imgv_bell_live_land_capture:
                ViewUtils.deBounceClick(view);
                BellLiveActivityPermissionsDispatcher.captureWithStoragePermissionWithCheck(this);
                break;
            case R.id.imgv_bell_live_speaker:
            case R.id.imgv_bell_live_land_mic:
                BellLiveActivityPermissionsDispatcher.switchSpeakerWithPermissionWithCheck(this);
                break;
            case R.id.imgv_bell_live_land_hangup:
            case R.id.imgv_bell_live_hang_up:
                presenter.dismiss();

                break;
            case R.id.imgv_bell_live_switch_to_land:
                initLandView();
                ViewUtils.setRequestedOrientation(this,
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }

    private boolean isLand() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private float getLandFillScreen() {
        return (float) Resources.getSystem().getDisplayMetrics().heightPixels /
                Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    @Override
    public void onResolution(JFGMsgVideoResolution resolution) throws JfgException {
        initVideoView();
        BaseApplication.getAppComponent().getCmd().enableRenderSingleRemoteView(true, mSurfaceView);
        mBellLiveVideoPicture.setVisibility(View.GONE);
        mVideoPlayController.setState(JConstant.PLAY_STATE_IDLE, null);
        imgvBellLiveCapture.setEnabled(true);
        imgvBellLiveSpeaker.setEnabled(true);
        imgvBellLiveSwitchToLand.setEnabled(true);
        imgvBellLiveSwitchToLand.setVisibility(View.VISIBLE);
        if (!presenter.checkAudio(1)) {
            presenter.switchMicrophone();
        }
        if (isSpeakerON) {
            BellLiveActivityPermissionsDispatcher.switchSpeakerWithPermissionWithCheck(this);
        }

//        if (isLanchFromBellCall) {
//            BellLiveActivityPermissionsDispatcher.switchSpeakerWithPermissionWithCheck(this);
//        }
        Device device = DataSourceManager.getInstance().getDevice(uuid);
        ratio = JFGRules.isNeedNormalRadio(device.pid) ? (float) resolution.height / resolution.width : 1.0f;
        ViewUtils.updateViewHeight(fLayoutBellLiveHolder, ratio);
    }

    @Override
    public void onFlowSpeed(int speed) {
        if (mBellFlow.getVisibility() != View.VISIBLE) {
            mBellFlow.setVisibility(View.VISIBLE);
        }
        mBellFlow.setText(MiscUtils.getByteFromBitRate(speed));
    }

    @Override
    public void onLiveStop(int errId) {

    }

    @Override
    public void onTakeSnapShotSuccess(Bitmap bitmap) {
        if (bitmap != null) {
            ToastUtil.showPositiveToast(getString(R.string.SAVED_PHOTOS));
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                showPopupWindow(bitmap);

        } else {
            ToastUtil.showNegativeToast(getString(R.string.set_failed));
        }
    }

    private void showPopupWindow(Bitmap bitmap) {
        try {
            roundCardPopup = new RoundCardPopup(this, view -> {
                if (bitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Glide.with(this)
                            .load(stream.toByteArray())
                            .placeholder(R.drawable.wonderful_pic_place_holder)
                            .override((int) getResources().getDimension(R.dimen.x44),
                                    (int) getResources().getDimension(R.dimen.x30))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .bitmapTransform(new RoundedCornersTransformation(this, 10, 2))
                            .into(view);
                }
            }, v -> {
                roundCardPopup.dismiss();
                Bundle bundle = new Bundle();
                bundle.putParcelable(JConstant.KEY_SHARE_ELEMENT_BYTE, bitmap);
                bundle.putString(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
                NormalMediaFragment fragment = NormalMediaFragment.newInstance(bundle);
                ActivityUtils.addFragmentSlideInFromRight(getSupportFragmentManager(), fragment,
                        android.R.id.content);
                fragment.setCallBack(t -> getSupportFragmentManager().popBackStack());
            });
            roundCardPopup.setAutoDismissTime(3000);
            roundCardPopup.showOnAnchor(imgvBellLiveCapture, RelativePopupWindow.VerticalPosition.ABOVE, RelativePopupWindow.HorizontalPosition.CENTER);
        } catch (Exception e) {
            AppLogger.e("showPopupWindow: " + e.getLocalizedMessage());
        }

    }

    @Override
    public void onTakeSnapShotFailed() {
        ToastUtil.showPositiveToast(getString(R.string.set_failed));
    }

    @Override
    public void onDeviceUnBind() {
        AppLogger.d("当前设备已解绑");
        presenter.cancelViewer();
        AlertDialogManager.getInstance().showDialog(this, getString(R.string.Tap1_device_deleted), getString(R.string.Tap1_device_deleted),
                getString(R.string.OK), (dialog, which) -> {
                    finish();
                    Intent intent = new Intent(this, NewHomeActivity.class);
                    startActivity(intent);
                }, false);
    }

    @Override
    public void onLoading(boolean loading) {
        if (loading) {
            mVideoPlayController.setState(PLAY_STATE_PREPARE, null);
        } else {
            mVideoPlayController.setState(PLAY_STATE_IDLE, null);
        }
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    void switchSpeakerWithPermission() {
        this.isSpeakerON = true;
        if (presenter.getLiveAction().hasStarted && presenter.getLiveAction().hasResolution) {
            presenter.switchSpeaker();
        }
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    public void hasNoAudioPermission() {
        Bundle args = new Bundle();
        args.putString(BaseDialog.KEY_TITLE, "");
        args.putString(SimpleDialogFragment.KEY_LEFT_CONTENT, getString(R.string.OK));
        args.putString(SimpleDialogFragment.KEY_CONTENT_CONTENT, getString(R.string.permission_auth, getString(R.string.Microphone)));
        SimpleDialogFragment dialogFragment = SimpleDialogFragment.newInstance(args);
        dialogFragment.setAction((id, value) -> {

        });
        getSupportFragmentManager().beginTransaction().add(dialogFragment, "audio_permission").commitAllowingStateLoss();
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void captureWithStoragePermission() {
        presenter.capture();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void hasNoStoragePermission() {
        //为删除dialog设置提示信息
        Bundle args = new Bundle();
        args.putString(BaseDialog.KEY_TITLE, "");
        args.putString(SimpleDialogFragment.KEY_LEFT_CONTENT, getString(R.string.OK));
        args.putString(SimpleDialogFragment.KEY_CONTENT_CONTENT, getString(R.string.permission_auth, getString(R.string.VALID_STORAGE)));
        SimpleDialogFragment dialogFragment = SimpleDialogFragment.newInstance(args);
        dialogFragment.show(getSupportFragmentManager(), "storage_permission");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        BellLiveActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onListen() {
        dLayoutBellHotSeat.setVisibility(View.VISIBLE);
        fLayoutBellAfterLive.setVisibility(View.GONE);
        imgvBellLiveSwitchToLand.setEnabled(false);
        mBellLiveVideoPicture.setVisibility(View.VISIBLE);
        mBellLiveVideoPicture.setImageResource(R.drawable.default_diagram_mask);
        playSoundEffect();
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        mBellhandle.startAnimation(shake);
    }

    @Override
    public void onNewCallTimeOut() {
//        dismissAlert();
//        mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.Item_ConnectionFail));
//        INotify.NotifyBean notify = new INotify.NotifyBean();
//        Device device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
//        int count = 0;
//        if (device != null) {
//            DPEntity entity = MiscUtils.getMaxVersionEntity(device.getMessageByOS(1004), device.getMessageByOS(1005));
//            if (entity != null) {
//                count = entity.getValue(0);
//            }
//        }
//        notify.count = count == 0 ? 1 : count;
//        Intent intent = new Intent(this, BellLiveActivity.class);
//        intent.putExtra(JConstant.VIEW_CALL_WAY, JConstant.VIEW_CALL_WAY_VIEWER);
//        intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
//        notify.pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        NotifyManager.getNotifyManager().sendNotify(notify);
        RxView.clicks(mBellFlow).subscribe(ret -> {
        });
        onDismiss();
    }

    @Override
    public void onVideoDisconnect(int code) {
        switch (code) {
            case JError.ErrorVideoPeerInConnect://其他端在查看
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.CONNECTING));
                break;
            case JError.ErrorVideoPeerNotExist://对端不在线
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.OFFLINE_ERR));
                break;
            case JError.ErrorVideoNotLogin://本端未登录
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.OFFLINE_ERR));
                break;
            case JError.ErrorVideoPeerDisconnect://对端断开
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.Device_Disconnected));
                break;
            case JError.ErrorP2PSocket:
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.OFFLINE_ERR));
                break;
            case BAD_NET_WORK:
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.OFFLINE_ERR_1));
                break;
            case BAD_FRAME_RATE:
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.NO_NETWORK_DOOR));
                break;
            default:
                mVideoPlayController.setState(PLAY_STATE_LOADING_FAILED, getString(R.string.NO_NETWORK_DOOR));

        }
        if (mSurfaceView != null) {
            ((GLSurfaceView) mSurfaceView).onPause();
            mSurfaceView = null;
        }
        imgvBellLiveCapture.setEnabled(false);
        imgvBellLiveSpeaker.setEnabled(false);
        imgvBellLiveSwitchToLand.setEnabled(false);
        mVideoViewContainer.removeAllViews();
        mBellLiveVideoPicture.setVisibility(View.GONE);
    }

    @Override
    public void onConnectDeviceTimeOut() {
        ToastUtil.showNegativeToast(getString(R.string.NO_NETWORK_DOOR));
        onDismiss();
//        presenter.dismiss();
    }

    @Override
    public void onShowVideoPreviewPicture(String URL) {
//        mVideoPlayController.setState(PLAY_STATE_IDLE, null);
        mBellLiveVideoPicture.setVisibility(View.VISIBLE);
        Glide.with(this).load(URL)
                .asBitmap()
                .placeholder(R.drawable.default_diagram_mask)
                .error(R.drawable.default_diagram_mask)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        if (resource != null && !resource.isRecycled())
                            mBellLiveVideoPicture.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        AppLogger.d("err:" + MiscUtils.getErr(e));
                        super.onLoadFailed(e, errorDrawable);
                    }
                });
    }


    public void onViewer() {
        AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int mode = manager.getMode();
        if (mode != AudioManager.MODE_NORMAL) {
            handleHeadsetConnected();
        }
        mVideoPlayController.setState(PLAY_STATE_PREPARE, null);
        imgvBellLiveCapture.setEnabled(false);
        imgvBellLiveSpeaker.setEnabled(false);
        imgvBellLiveSwitchToLand.setEnabled(false);
        dLayoutBellHotSeat.setVisibility(View.GONE);
        fLayoutBellAfterLive.setVisibility(View.VISIBLE);

    }


    @Override
    public void onSpeaker(boolean on) {
        if (mLandBellLiveSpeaker != null) {
            mLandBellLiveSpeaker.setImageResource(on ? R.drawable.doorbell_icon_landscape_talk : R.drawable.doorbell_icon_landscape_no_talk);
        }
        imgvBellLiveSpeaker.setImageResource(on ? R.drawable.icon_port_voice_on_selector : R.drawable.icon_port_voice_off_selector);
        if (isHeadsetOn()) {
            BellLiveActivityPermissionsDispatcher.handleHeadsetConnectedWithCheck(BellLiveActivity.this);
        }
    }

    @Override
    public void onMicrophone(boolean on) {
        if (isHeadsetOn()) {
            BellLiveActivityPermissionsDispatcher.handleHeadsetConnectedWithCheck(BellLiveActivity.this);
        }
    }

    @Override
    public String onResolveViewLaunchType() {
        return getIntent().getStringExtra(JConstant.VIEW_CALL_WAY);
    }

    /**
     * 初始化videoView
     *
     * @return
     */
    private void initVideoView() {
        if (mSurfaceView == null) {
            Device device = DataSourceManager.getInstance().getDevice(uuid);
            mSurfaceView = (SurfaceView) VideoViewFactory.CreateRendererExt(device.pid, this);
            mSurfaceView.setId("IVideoView".hashCode());
            if (mSurfaceView instanceof Panoramic360View) {
                ((Panoramic360View) mSurfaceView).configV360(CameraParam.getTopPreset());
            } else if (mSurfaceView instanceof Panoramic360ViewRS) {
                ((Panoramic360ViewRS) mSurfaceView).configV360(CameraParam.getTopPreset());
            }
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mSurfaceView.setLayoutParams(params);
            mVideoViewContainer.removeAllViews();
            mVideoViewContainer.addView(mSurfaceView);
        }
        AppLogger.i("initVideoView");
//        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
//        mSurfaceView.getHolder().setFormat(PixelFormat.OPAQUE);
    }


    @Override
    public void onPickup() {
        dLayoutBellHotSeat.setVisibility(View.GONE);
        fLayoutBellAfterLive.setVisibility(View.VISIBLE);

    }

    @Override
    public void onDismiss() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        finishExt();
        if (getIntent().getBooleanExtra(JConstant.IS_IN_BACKGROUND, false)) {
            Intent intent = new Intent(this, NewHomeActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onCallAnswerInOther() {
        ToastUtil.showNegativeToast(getString(R.string.DOOR_OTHER_LISTENED));
    }

    @Override
    public void onNewCallWhenInLive(String person) {
//        mNewCallHandle = showAlert(getString(R.string.Tap1_Index_Tips_Newvisitor), getString(R.string.Tap1_Index_Tips_Newvisitor), getString(R.string.EFAMILY_CALL_ANSWER), getString(R.string.IGNORE));
    }

    @Override
    public void onViewAction(int action, String handler, Object extra) {
        if (TextUtils.equals(mNewCallHandle, handler)) {
            switch (action) {
                case VIEW_ACTION_OK:
                    presenter.startViewer();
                    break;
                case VIEW_ACTION_CANCEL:

                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            super.onBackPressed();
            presenter.dismiss();
        }
    }

    @Override
    public void clickImage(View view, int state) {
        switch (state) {
            case PLAY_STATE_LOADING_FAILED:
                presenter.startViewer();
                mVideoPlayController.setState(PLAY_STATE_PREPARE, "");
                break;
        }
    }

    @Override
    public void clickText(View view) {
    }

    @Override
    public void clickHelp(View view) {

    }

    @Override
    public void playSoundEffect() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.doorbell_called);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            AppLogger.e("onAudioFocusChange focusChange = " + focusChange);
            //被其他App切换时，把当前自己的音乐停止
        }
    };

    @Override
    public boolean muteAudio(boolean bMute) {
        boolean isSuccess = false;
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (bMute) {
            int result = am.requestAudioFocus(mOnAudioFocusChangeListener,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            isSuccess = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        } else {
            int result = am.abandonAudioFocus(mOnAudioFocusChangeListener);
            isSuccess = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        AppLogger.e("pauseMusic bMute=" + bMute + " result=" + isSuccess);
        return isSuccess;
    }

    private void registSreenStatusReceiver() {
        mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter screenStatusIF = new IntentFilter();
        screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
        screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStatusReceiver, screenStatusIF);
    }

    private ScreenStatusReceiver mScreenStatusReceiver;

    class ScreenStatusReceiver extends BroadcastReceiver {
        String SCREEN_ON = "android.intent.action.SCREEN_ON";
        String SCREEN_OFF = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (SCREEN_ON.equals(intent.getAction())) {

            } else if (SCREEN_OFF.equals(intent.getAction())) {
                presenter.dismiss();
                finish();//115763 //门铃呼叫 弹出呼叫界面后，退到后台/打开其他软件时，再返回app时，需要断开门铃弹窗
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStatusReceiver);
        if (homeListen != null) {
            unregisterReceiver(homeListen);
        }
        clearHeadSetEventReceiver();
    }

    @Override
    protected void setActivityComponent(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

//    protected void hideBottomUIMenu() {
//        //隐藏虚拟按键，并且全屏
//        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
//            View v = this.getWindow().getDecorView();
//            v.setSystemUiVisibility(View.GONE);
//        } else if (Build.VERSION.SDK_INT >= 19) {
//            //for new api versions.
//            View decorView = getWindow().getDecorView();
//            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
//            decorView.setSystemUiVisibility(uiOptions);
//        }
//    }

    private HomeListen homeListen;

    private class HomeListen extends BroadcastReceiver {

        private final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        private final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);

                if (reason == null)
                    return;
                // TODO: 2017/8/31 R11上监听不到
                // Home键
//                if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
//                    presenter.dismiss();
//                    finish();//115763 //门铃呼叫 弹出呼叫界面后，退到后台/打开其他软件时，再返回app时，需要断开门铃弹窗
//                }
//
//                // 最近任务列表键
//                if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
//                    presenter.dismiss();
//                    finish();//115763 //门铃呼叫 弹出呼叫界面后，退到后台/打开其他软件时，再返回app时，需要断开门铃弹窗
//                }
            }
        }
    }


}
