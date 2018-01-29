package com.cylan.jiafeigou.n.mvp.impl.cam;

import android.util.Log;

import com.cylan.entity.jniCall.JFGMsgHttpResult;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.base.wrapper.BasePresenter;
import com.cylan.jiafeigou.module.Command;
import com.cylan.jiafeigou.n.mvp.contract.cam.RegisterFaceContract;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.support.OptionsImpl;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.AESUtil;
import com.cylan.jiafeigou.utils.NetUtils;
import com.lzy.okgo.OkGo;

import org.json.JSONObject;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by yanzhendong on 2018/1/26.
 */
public class RegisterFacePresenter extends BasePresenter<RegisterFaceContract.View> implements RegisterFaceContract.Presenter {

    @Inject
    public RegisterFacePresenter(RegisterFaceContract.View view) {
        super(view);
        setSSL();
    }

    private static void setSSL() {
        OkGo.getInstance().setHostnameVerifier((hostname, session) -> true);
        OkGo.getInstance().setCertificates(new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
        });
    }

    @Override
    public void performRegisterFaceAction(String nickName, String photoPath) {
        long time = System.currentTimeMillis() / 1000;
        String account = DataSourceManager.getInstance().getAccount().getAccount();
        String remotePath = String.format(Locale.getDefault(), "/long/%s/AI/%d.png", account, time);
        Subscription subscribe = Observable.create((Observable.OnSubscribe<Boolean>) subscriber -> {
            boolean hasNetwork = NetUtils.hasNetwork();
            if (!hasNetwork) {
                mView.onRegisterErrorNoNetwork();
            } else {
                subscriber.onNext(true);
            }
            subscriber.onCompleted();
        })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map(s -> {
                    try {
                        return Command.getInstance().putFileToCloud(remotePath, photoPath);
                    } catch (JfgException e) {
                        e.printStackTrace();
                        AppLogger.e(e);
                        return -1;
                    }
                })
                .flatMap(reqId -> RxBus.getCacheInstance().toObservable(JFGMsgHttpResult.class)
                        .first(jfgMsgHttpResult -> {
                            Log.e("RegisterFacePresenter", "http put file result:" + jfgMsgHttpResult.ret + ",reqid:" + jfgMsgHttpResult.requestId + ",except:" + reqId);
                            return jfgMsgHttpResult.requestId == reqId;
                        }))
                .map(s -> {
                    String authToken;
                    try {
                        String server = ("https://" + OptionsImpl.getServer() + ":8085").replace(":443", "");
                        String authPath = "/authtoken";
                        String authApi = server + authPath;
                        JSONObject tokenParams = new JSONObject();
                        String vid = OptionsImpl.getVid();
                        String serviceKey = OptionsImpl.getServiceKey(vid);
                        String serviceSeceret = OptionsImpl.getServiceSeceret(vid);
                        tokenParams.put("service_key", serviceKey);
                        tokenParams.put("time", time);
                        tokenParams.put("sign", AESUtil.HmacSHA1Encrypt(String.format(Locale.getDefault(), "%s\n%d", authPath, time), serviceSeceret));
                        Response execute = OkGo.post(authApi)
                                .requestBody(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), tokenParams.toString()))
                                .execute();
                        JSONObject jsonObject = new JSONObject(execute.body().string());
                        Log.e("RegisterFacePresenter", "get token response:" + jsonObject);
                        int code = jsonObject.getInt("code");
                        if (code != 200) {
                            return code;
                        }


                        authToken = jsonObject.getString("auth_token");
                        String aiAppApi = server + "/aiapp";
                        tokenParams = new JSONObject();
                        tokenParams.put("action", "RegisterPerson");
                        tokenParams.put("auth_token", /*authToken*/"JFG_SERVER_PASS_TOKEN_x20180124x");
                        tokenParams.put("time", time);
                        tokenParams.put("person_name", nickName);
                        tokenParams.put("account", account);
                        tokenParams.put("cid", uuid);
                        execute = OkGo.post(aiAppApi)
                                .requestBody(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), tokenParams.toString()))
                                .execute();
                        jsonObject = new JSONObject(execute.body().string());
                        Log.e("RegisterFacePresenter", "register face response:" + jsonObject);
                        code = jsonObject.getInt("code");
                        if (code != 200) {
                            return code;
                        }

                        tokenParams = new JSONObject();
                        tokenParams.put("action", "AddFace");
                        tokenParams.put("auth_token", /*authToken*/"JFG_SERVER_PASS_TOKEN_x20180124x");
                        tokenParams.put("time", time);
                        tokenParams.put("person_id", jsonObject.getString("person_id"));
                        tokenParams.put("image_url", remotePath);
                        tokenParams.put("oss_type", DataSourceManager.getInstance().getStorageType());
                        execute = OkGo.post(aiAppApi)
                                .requestBody(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), tokenParams.toString()))
                                .execute();
                        jsonObject = new JSONObject(execute.body().string());
                        code = jsonObject.getInt("code");
                        Log.e("RegisterFacePresenter", "add face response:" + jsonObject);
                        return code;
                    } catch (Exception e) {
                        e.printStackTrace();
                        AppLogger.e(e);
                    }
                    return -1;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(10, TimeUnit.SECONDS)
                .compose(applyLoading(false, R.string.LOADING))
                .subscribe(code -> {
                    switch (code) {
                        case 100: {
                            mView.onRegisterErrorPermissionDenied();
                        }
                        break;
                        case 101: {
                            mView.onRegisterErrorInvalidParams();
                        }
                        break;
                        case 102: {
                            mView.onRegisterErrorServerInternalError();
                        }
                        break;
                        case 103: {
                            mView.onRegisterErrorNoFaceError();
                        }
                        break;
                        case 104: {
                            mView.onRegisterErrorFaceSmallError();
                        }
                        break;
                        case 105: {
                            mView.onRegisterErrorMultiFaceError();
                        }
                        break;
                        case 106: {
                            mView.onRegisterErrorNoFeaturesInFaceError();
                        }
                        break;
                        case 107: {
                            mView.onRegisterErrorRegUserError();
                        }
                        break;
                        case 200: {
                            mView.onRegisterSuccessful();
                        }
                        break;
                        case -1: {
                            mView.onRegisterErrorDetectionFailed();
                        }
                        break;
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    AppLogger.e(throwable);
                    if (throwable instanceof TimeoutException) {
                        mView.onRegisterTimeout();
                    } else {
                        mView.onRegisterErrorRegisterFailed();
                    }
                });
        addStopSubscription(subscribe);
    }

}
