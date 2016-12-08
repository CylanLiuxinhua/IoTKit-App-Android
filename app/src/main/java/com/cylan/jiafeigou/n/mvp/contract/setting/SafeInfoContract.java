package com.cylan.jiafeigou.n.mvp.contract.setting;

import android.content.Context;

import com.cylan.jiafeigou.n.mvp.BasePresenter;
import com.cylan.jiafeigou.n.mvp.BaseView;
import com.cylan.jiafeigou.n.mvp.model.BeanCamInfo;

/**
 * Created by hunt on 16-5-26.
 */

public interface SafeInfoContract {

    interface View extends BaseView<Presenter> {
        void beanUpdate(BeanCamInfo info);
    }

    interface Presenter extends BasePresenter {
        void saveCamInfoBean(BeanCamInfo beanCamInfo, int id);

        BeanCamInfo getBeanCamInfo();

        String getRepeatMode(Context context);
    }
}