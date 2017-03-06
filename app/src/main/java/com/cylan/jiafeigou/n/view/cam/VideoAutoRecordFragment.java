package com.cylan.jiafeigou.n.view.cam;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.dp.DpMsgMap;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.n.base.IBaseFragment;
import com.cylan.jiafeigou.n.mvp.contract.setting.VideoAutoRecordContract;
import com.cylan.jiafeigou.n.mvp.impl.setting.VideoAutoRecordPresenterImpl;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.widget.CustomToolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.cylan.jiafeigou.misc.JConstant.KEY_DEVICE_ITEM_UUID;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoAutoRecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoAutoRecordFragment extends IBaseFragment<VideoAutoRecordContract.Presenter>
        implements VideoAutoRecordContract.View {

    @BindView(R.id.rg_auto_record_mode)
    RadioGroup rgAutoRecordMode;
    @BindView(R.id.rb_motion)
    RadioButton rbMotion;
    @BindView(R.id.rb_24_hours)
    RadioButton rb24Hours;
    @BindView(R.id.rb_never)
    RadioButton rbNever;
    @BindView(R.id.custom_toolbar)
    CustomToolbar customToolbar;
    private String uuid;
    private int oldOption;

    public void onAttach(Context context) {
        super.onAttach(context);
        this.uuid = getArguments().getString(KEY_DEVICE_ITEM_UUID);
        basePresenter = new VideoAutoRecordPresenterImpl(this, uuid);
    }

    /**
     * Use this factory method to create activity_cloud_live_mesg_call_out_item new instance of
     * this fragment using the provided parameters.
     *
     * @param args Parameter 2.
     * @return A new instance of fragment SafeProtectionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VideoAutoRecordFragment newInstance(Bundle args) {
        VideoAutoRecordFragment fragment = new VideoAutoRecordFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_video_auto_record, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (JFGRules.isFreeCam(DataSourceManager.getInstance().getJFGDevice(uuid))) {
            rb24Hours.setVisibility(View.GONE);
            view.findViewById(R.id.lLayout_mode_24_hours).setVisibility(View.GONE);
        }
        customToolbar.setBackAction(v -> getFragmentManager().popBackStack());
        DpMsgDefine.DPPrimary<Integer> focus = DataSourceManager.getInstance().getValue(uuid, DpMsgMap.ID_303_DEVICE_AUTO_VIDEO_RECORD);
        oldOption = focus.$();
        rbMotion.setChecked(focus.$() == 0);
        rb24Hours.setChecked(focus.$() == 1);
        rbNever.setChecked(focus.$() == 2);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        DpMsgDefine.DPPrimary<Integer> auto = DataSourceManager.getInstance().getValue(uuid, DpMsgMap.ID_303_DEVICE_AUTO_VIDEO_RECORD);
        if (oldOption != auto.$()) {
            ToastUtil.showToast(getString(R.string.SCENE_SAVED));
        }
        if (callBack != null)
            callBack.callBack(null);
    }


    @Override
    public void setPresenter(VideoAutoRecordContract.Presenter presenter) {
        basePresenter = presenter;
    }

    @OnClick({R.id.lLayout_mode_motion, R.id.lLayout_mode_24_hours, R.id.lLayout_mode_never})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.lLayout_mode_motion: {
                if (!alarmDisable()) {
                    openAlarm(0);
                    return;
                }
                if (!hasSdcard()) {
                    ToastUtil.showToast(getString(R.string.has_not_sdcard));
                    return;
                }
                rbMotion.setChecked(true);
                DpMsgDefine.DPPrimary<Integer> flag = new DpMsgDefine.DPPrimary<>();
                flag.value = 0;
                basePresenter.updateInfoReq(flag, DpMsgMap.ID_303_DEVICE_AUTO_VIDEO_RECORD);
            }
            break;
            case R.id.lLayout_mode_24_hours: {
//                if (!alarmDisable()) {
//                    openAlarm(1);
//                    return;
//                }
                if (!hasSdcard()) {
                    ToastUtil.showToast(getString(R.string.has_not_sdcard));
                    return;
                }
                rb24Hours.setChecked(true);
                DpMsgDefine.DPPrimary<Integer> flag = new DpMsgDefine.DPPrimary<>();
                flag.value = 1;
                basePresenter.updateInfoReq(flag, DpMsgMap.ID_303_DEVICE_AUTO_VIDEO_RECORD);
            }
            break;
            case R.id.lLayout_mode_never: {
                rbNever.setChecked(true);
                DpMsgDefine.DPPrimary<Integer> flag = new DpMsgDefine.DPPrimary<>();
                flag.value = 2;
                basePresenter.updateInfoReq(flag, DpMsgMap.ID_303_DEVICE_AUTO_VIDEO_RECORD);
            }
            break;
        }
    }

    private void openAlarm(final int index) {
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.RECORD_ALARM_OPEN))
                .setPositiveButton(getString(R.string.OPEN), (DialogInterface dialog, int which) -> {
                    if (index == 0 && !hasSdcard()) {
                        ToastUtil.showToast(getString(R.string.has_not_sdcard));
                        return;
                    }
                    DpMsgDefine.DPPrimary<Boolean> wFlag = new DpMsgDefine.DPPrimary<>();
                    wFlag.value = true;
                    basePresenter.updateInfoReq(wFlag, DpMsgMap.ID_501_CAMERA_ALARM_FLAG);
                    ToastUtil.showToast(getString(R.string.SCENE_SAVED));
                    if (index == 1)
                        rb24Hours.setChecked(true);
                    else rbMotion.setChecked(true);
                    DpMsgDefine.DPPrimary<Integer> flag = new DpMsgDefine.DPPrimary<>();
                    flag.value = index;
                    basePresenter.updateInfoReq(flag, DpMsgMap.ID_303_DEVICE_AUTO_VIDEO_RECORD);
                })
                .setNegativeButton(getString(R.string.CANCEL), null)
                .show();
    }

    private boolean alarmDisable() {
        DpMsgDefine.DPPrimary<Boolean> alarm = DataSourceManager.getInstance().getValue(uuid, DpMsgMap.ID_501_CAMERA_ALARM_FLAG);
        return alarm.$();
    }

    private boolean hasSdcard() {
        DpMsgDefine.DPSdStatus status = DataSourceManager.getInstance().getValue(uuid,
                DpMsgMap.ID_204_SDCARD_STORAGE);
        return status.hasSdcard && status.err == 0;
    }
}
