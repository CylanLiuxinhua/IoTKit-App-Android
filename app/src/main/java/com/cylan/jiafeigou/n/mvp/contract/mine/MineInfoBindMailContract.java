package com.cylan.jiafeigou.n.mvp.contract.mine;

import com.cylan.entity.jniCall.JFGAccount;
import com.cylan.jiafeigou.misc.RxEvent;
import com.cylan.jiafeigou.n.mvp.BasePresenter;
import com.cylan.jiafeigou.n.mvp.BaseView;

import rx.Subscription;

/**
 * 作者：zsl
 * 创建时间：2016/9/12
 * 描述：
 */
public interface MineInfoBindMailContract {

    interface View extends BaseView<Presenter> {
        /**
         * 显示邮箱已经绑定对话框
         */
        void showMailHasBindDialog();                   //显示邮箱已经绑定

        /**
         * 显示请求发送之后的对话框
         */
        void showSendReqResult(RxEvent.GetUserInfo getUserInfo);

        /**
         * 显示账号未注册过
         */
        void showAccountUnReg();

        /**
         * 显示绑定进度
         */
        void showSendReqHint();

        /**
         * 隐藏绑定进度
         */
        void hideSendReqHint();
    }

    interface Presenter extends BasePresenter {

        boolean checkEmail(String email);               //检查邮箱的合法性

        void checkEmailIsBinded(String email);       //检验邮箱是否已经绑定过

        /**
         * 检验账号是否手机号
         * @param account
         * @return
         */
        boolean checkAccoutIsPhone(String account);

        /**
         * 发送修改用户属性请求
         */
        void sendSetAccountReq(JFGAccount account);

        /**
         * 接收到检验邮箱是否已经注册过
         * @return
         */
        Subscription getCheckAccountCallBack();


        /**
         * 修改属性后的回调
         */
        Subscription getChangeAccountCallBack();
    }
}