package com.cylan.jiafeigou.n.mvp.impl.bind;

import android.text.TextUtils;

import com.cylan.entity.jniCall.JFGDPMsg;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.cache.db.module.Device;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JfgCmdInsurance;
import com.cylan.jiafeigou.misc.SimulatePercent;
import com.cylan.jiafeigou.misc.bind.UdpConstant;
import com.cylan.jiafeigou.n.mvp.contract.bind.SubmitBindingInfoContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractPresenter;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.BindUtils;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import static com.cylan.jiafeigou.misc.JfgCmdInsurance.getCmd;
import static com.cylan.jiafeigou.utils.BindUtils.BIND_SUC;
import static com.cylan.jiafeigou.utils.BindUtils.BIND_TIME_OUT;

/**
 * Created by cylan-hunt on 16-11-12.
 */

public class SubmitBindingInfoContractImpl extends AbstractPresenter<SubmitBindingInfoContract.View>
        implements SubmitBindingInfoContract.Presenter, SimulatePercent.OnAction {

    private SimulatePercent simulatePercent;
    private CompositeSubscription subscription;
    private Subscription submitSubscription;
    private int bindResult;
    private long startTick;

    public SubmitBindingInfoContractImpl(SubmitBindingInfoContract.View view, String uuid) {
        super(view, uuid);
        view.setPresenter(this);
        simulatePercent = new SimulatePercent();
        simulatePercent.setOnAction(this);
        bindResult = BindUtils.BIND_PREPARED;
    }

    @Override
    public void startCounting() {
        if (simulatePercent != null)
            simulatePercent.start();
    }

    @Override
    public void endCounting() {
        if (simulatePercent != null) {
            simulatePercent.boost();
        }
    }

    @Override
    public void clean() {
        RxBus.getCacheInstance().removeStickyEvent(RxEvent.BindDeviceEvent.class);
    }

    @Override
    public void start() {
        super.start();
        Device device = DataSourceManager.getInstance().getJFGDevice(uuid);
        if (startTick == 0) {//可能是覆盖绑定.
            startTick = System.currentTimeMillis();
            //1.可能是覆盖绑定,或者设备列表中已经有了该设备,并且在线状态.
            if (device != null) {
                //2.清空net状态
                device.setValue(201, new DpMsgDefine.DPNet());//先清空
            }
        }
        if (System.currentTimeMillis() - startTick > 120 * 1000) {
            //timeout
            mView.bindState(this.bindResult = BIND_TIME_OUT);
            return;
        }
        //3.重新获取,
        DpMsgDefine.DPNet net = device == null ? null : device.$(201, new DpMsgDefine.DPNet());
        if (device != null && !TextUtils.isEmpty(uuid) && net != null && net.net > 0) {
            //4.net数据可能已经被更新了(重新进入该页面时候使用.)
            endCounting();
            AppLogger.d("finish? ;" + net);
            return;
        }
        //超时
        if (bindResult == BindUtils.BIND_PREPARED) {
            bindResult = BindUtils.BIND_ING;
            if ((subscription == null || subscription.isUnsubscribed())) {
                subscription = new CompositeSubscription();
                AppLogger.d("add sub result");
                if (submitSubscription == null || !submitSubscription.isUnsubscribed()) {
                    submitSubscription = submitBindDeviceSub();
                    subscription.add(submitSubscription);
                }
            }
        }
        if (bindResult == BindUtils.BIND_ING) {
            if (simulatePercent != null) simulatePercent.resume();
        }
    }

    private Subscription submitBindDeviceSub() {
        return Observable.interval(0, 2, TimeUnit.SECONDS)
                .map(s -> DataSourceManager.getInstance().getAJFGAccount())
                .filter(account -> account != null && account.isOnline())
                .first()
                .flatMap(s -> Observable.interval(0, 5, TimeUnit.SECONDS))
                .map(s -> {
                    try {
                        String content = PreferencesUtils.getString(JConstant.BINDING_DEVICE);
                        UdpConstant.UdpDevicePortrait portrait = new Gson().fromJson(content, UdpConstant.UdpDevicePortrait.class);
                        if (portrait != null) {
                            getCmd().bindDevice(portrait.uuid, portrait.bindCode, portrait.mac, portrait.bindFlag);
                            AppLogger.d("正在发送绑定请求:" + new Gson().toJson(portrait));
                        }
                    } catch (Exception e) {
                        AppLogger.d("err: " + e.getLocalizedMessage());
                    }
                    return DataSourceManager.getInstance().getJFGDevice(uuid);
                })
                .filter(device -> device != null)
                .first()
                .flatMap(s -> Observable.interval(0, 3, TimeUnit.SECONDS))
                .map(s -> {
                    ArrayList<JFGDPMsg> params = new ArrayList<>(1);
                    JFGDPMsg msg = new JFGDPMsg(201, 0);
                    params.add(msg);
                    try {
                        JfgCmdInsurance.getCmd().robotGetData(uuid, params, 1, false, 0);
                    } catch (JfgException e) {
                        AppLogger.d(e.getMessage());
                    }
                    Device device = DataSourceManager.getInstance().getJFGDevice(uuid);
                    DpMsgDefine.DPNet net = null;
                    if (device != null) {
                        net = device.$(201, new DpMsgDefine.DPNet());
                    }
                    AppLogger.d("正在查询设备网络状态:" + new Gson().toJson(net));
                    return net;
                })
                .filter(net -> net != null && net.net > 0)
                .first()
                .timeout(120 * 1000L - (System.currentTimeMillis() - startTick), TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(net -> {
                    AppLogger.d("当前网络状态为:" + DpMsgDefine.DPNet.getNormalString(net));
                    endCounting();
                }, e -> {
                    if (e instanceof TimeoutException) {
                        mView.bindState(this.bindResult = BIND_TIME_OUT);
                        AppLogger.d("绑定设备超时");
                    }
                    AppLogger.d("擦,出错了:" + e.getMessage());
                });
    }

    @Override
    public void stop() {
        super.stop();
        if (bindResult == BindUtils.BIND_ING) {
            if (simulatePercent != null) simulatePercent.resume();
        } else {
            if (simulatePercent != null)
                simulatePercent.stop();
        }
        if (subscription != null && subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
        bindResult = BindUtils.BIND_PREPARED;
    }


    @Override
    public void actionDone() {
        Subscription subscription = Observable.just(null)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe((Object integer) -> {
                    AppLogger.i("actionDone: " + integer);
                    getView().bindState(this.bindResult = BIND_SUC);
                });
        addSubscription(subscription, "actionDone");
    }

    @Override
    public void actionPercent(int percent) {
        Subscription subscription = Observable.just(percent)
                .filter((Integer integer) -> (getView() != null))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Integer integer) -> {
                    getView().onCounting(integer);
                });
        addSubscription(subscription, "actionPercent");
    }

}
