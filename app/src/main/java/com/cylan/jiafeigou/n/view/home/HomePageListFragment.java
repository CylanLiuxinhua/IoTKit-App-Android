package com.cylan.jiafeigou.n.view.home;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.n.mvp.contract.home.HomePageListContract;
import com.cylan.jiafeigou.utils.ToastUtil;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class HomePageListFragment extends Fragment implements
        HomePageListContract.View, PtrHandler {


    private static final int REFRESH_DELAY = 1500;
    @BindView(R.id.fLayout_main_content_holder)
    PtrClassicFrameLayout fLayoutMainContentHolder;
    @BindView(R.id.imgBtn_add_devices)
    ImageButton imgBtnAddDevices;

    HomePageListContract.Presenter presenter;
    @BindView(R.id.rV_devices_list)
    RecyclerView rVDevicesList;//设备列表
    /**
     * 手动完成刷新,自动完成刷新 订阅者.
     */
    private Subscription refreshCompleteSubscription;

    public static HomePageListFragment newInstance(Bundle bundle) {
        HomePageListFragment fragment = new HomePageListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle;
        if (getArguments() != null) {
            bundle = getArguments();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home_page_list, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //添加Handler
        fLayoutMainContentHolder.setPtrHandler(this);
    }

    @OnClick(R.id.imgBtn_add_devices)
    void onClickAddDevice() {
        ToastUtil.showToast(getContext(), "add devices");
    }

    @Override
    public void onStop() {
        super.onStop();
        unRegisterSubscription(refreshCompleteSubscription);
    }

    /**
     * 反注册
     *
     * @param subscriptions
     */
    private void unRegisterSubscription(Subscription... subscriptions) {
        for (Subscription subscription : subscriptions) {
            if (subscription != null)
                subscription.unsubscribe();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void setPresenter(HomePageListContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onDeviceListRsp(Object object) {
        unRegisterSubscription(refreshCompleteSubscription);
    }

    @Override
    public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
        return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
    }

    @Override
    public void onRefreshBegin(final PtrFrameLayout frame) {
        presenter.startRefresh();
        //不使用post,因为会泄露
        refreshCompleteSubscription = Observable.just(frame)
                .subscribeOn(Schedulers.newThread())
                .delay(REFRESH_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<PtrFrameLayout>() {
                    @Override
                    public void call(PtrFrameLayout frame) {
                        frame.refreshComplete();
                    }
                });
    }


}
