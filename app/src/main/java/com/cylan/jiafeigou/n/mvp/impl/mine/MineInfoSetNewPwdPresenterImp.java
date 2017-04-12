package com.cylan.jiafeigou.n.mvp.impl.mine;

import android.text.TextUtils;

import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.misc.JfgCmdInsurance;
import com.cylan.jiafeigou.n.mvp.contract.mine.MineInfoSetNewPwdContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractPresenter;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.ContextUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * 作者：zsl
 * 创建时间：2016/12/28
 * 描述：
 */
public class MineInfoSetNewPwdPresenterImp extends AbstractPresenter<MineInfoSetNewPwdContract.View> implements MineInfoSetNewPwdContract.Presenter {

    private CompositeSubscription subscription;
    private boolean isOverTime;

    public MineInfoSetNewPwdPresenterImp(MineInfoSetNewPwdContract.View view) {
        super(view);
        view.setPresenter(this);
    }
    /**
     * 注册
     * @param account
     * @param pwd
     */
    @Override
    public void openLoginRegister(String account, String pwd, String token) {
        rx.Observable.just(null)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        try {
                            if (!TextUtils.isEmpty(token)) {
//                                JfgCmdInsurance.getCmd().register(JFGRules.getLanguageType(ContextUtils.getContext()), account, pwd, JConstant.TYPE_PHONE, "");
                                int req = JfgCmdInsurance.getCmd().setPwdWithBindAccount(pwd, JConstant.TYPE_PHONE, token);
                                AppLogger.d("openLogin_bind_phone:"+req+" token:"+token);
                            } else{
//                                JfgCmdInsurance.getCmd().register(JFGRules.getLanguageType(ContextUtils.getContext()), account, pwd, JConstant.TYPE_EMAIL, "");
                                JfgCmdInsurance.getCmd().setPwdWithBindAccount(pwd,JConstant.TYPE_EMAIL,"");
                            }
                        } catch (JfgException e) {
                            e.printStackTrace();
                        }
                    }
                }, throwable -> {
                    AppLogger.e("openLoginRegister" + throwable.getLocalizedMessage());
                });
    }

    /**
     * 注册回调
     * @return
     */
    @Override
    public Subscription registerBack() {
        return RxBus.getCacheInstance().toObservable(RxEvent.OpenLogInSetPwdBack.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(openLogInSetPwdBack -> {
                    if (openLogInSetPwdBack != null) {
                        if (getView() != null) getView().registerResult(openLogInSetPwdBack.jfgResult.code);
                    }
                }, AppLogger::e);
    }

    @Override
    public Subscription timeOverCount() {
        return Observable.just(null)
                .delay(5, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o->{
                    isOverTime = true;
                },throwable -> {
                    AppLogger.e("timeOverCount_erro"+throwable.getLocalizedMessage());
                });
    }

    @Override
    public boolean checkIsOverTime() {
        return isOverTime;
    }

    @Override
    public void start() {
        super.start();
        if (subscription != null && !subscription.isUnsubscribed()){
            subscription.unsubscribe();
        }else {
            subscription = new CompositeSubscription();
            subscription.add(registerBack());
            subscription.add(timeOverCount());
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (subscription != null && !subscription.isUnsubscribed()){
            subscription.unsubscribe();
        }
    }

}
