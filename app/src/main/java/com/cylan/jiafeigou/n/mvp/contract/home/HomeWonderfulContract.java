package com.cylan.jiafeigou.n.mvp.contract.home;

import com.cylan.jiafeigou.n.mvp.BasePresenter;
import com.cylan.jiafeigou.n.mvp.BaseView;
import com.cylan.jiafeigou.n.mvp.model.MediaBean;
import com.cylan.jiafeigou.widget.wheel.WheelViewDataSet;

import java.util.List;

/**
 * Created by hunt on 16-5-23.
 */
public interface HomeWonderfulContract {

    interface View extends BaseView<Presenter> {
        void onDeviceListRsp(List<MediaBean> list);

        void onHeadBackgroundChang(int daytime);

        void timeLineDataUpdate(WheelViewDataSet wheelViewDataSet);

    }

    interface Presenter extends BasePresenter {
        void startRefresh();
    }

}
