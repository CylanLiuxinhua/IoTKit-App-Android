package com.cylan.jiafeigou.n.mvp.contract.home;

import com.cylan.jiafeigou.n.mvp.BasePresenter;
import com.cylan.jiafeigou.n.mvp.BaseView;
import com.cylan.jiafeigou.n.mvp.model.MineMessageBean;

import java.util.ArrayList;
import java.util.List;

import rx.Subscription;

/**
 * 作者：zsl
 * 创建时间：2016/9/5
 * 描述：
 */
public interface HomeMineMessageContract {

    interface View extends BaseView<Presenter> {

        /**
         * 初始化消息列表
         * @param list
         */
        void initRecycleView(ArrayList<MineMessageBean> list);

        /**
         * 消息为空显示
         */
        void showNoMesgView();

        /**
         * 消息不为空时显示
         */
        void hideNoMesgView();
    }

    interface Presenter extends BasePresenter {

        /**
         * 加载消息数据
         */
        void initMesgData();

        /**
         * 获取到用户的信息拿到数据库操作对象
         */
        Subscription getAccount();

        /**
         * 获取本地数据库中的所有消息记录
         * @return
         */
        List<MineMessageBean> findAllFromDb();

        /**
         * 清空本地消息记录
         */
        void clearRecoard();

        /**
         * 消息保存到数据库
         * @param bean
         */
        void saveIntoDb(MineMessageBean bean);

    }

}
