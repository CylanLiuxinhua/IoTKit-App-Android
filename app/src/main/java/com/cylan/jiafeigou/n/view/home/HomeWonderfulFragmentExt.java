package com.cylan.jiafeigou.n.view.home;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.UiThread;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.wrapper.BaseFragment;
import com.cylan.jiafeigou.cache.pool.GlobalDataProxy;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.misc.OnActivityReenterListener;
import com.cylan.jiafeigou.misc.SharedElementCallBackListener;
import com.cylan.jiafeigou.n.mvp.contract.home.HomeWonderfulContract;
import com.cylan.jiafeigou.n.mvp.impl.home.HomeWonderfulPresenterImpl;
import com.cylan.jiafeigou.n.mvp.model.MediaBean;
import com.cylan.jiafeigou.n.view.activity.MediaActivity;
import com.cylan.jiafeigou.n.view.adapter.HomeWonderfulAdapter;
import com.cylan.jiafeigou.n.view.record.DelayRecordActivity;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.AnimatorUtils;
import com.cylan.jiafeigou.utils.TimeUtils;
import com.cylan.jiafeigou.utils.ViewUtils;
import com.cylan.jiafeigou.widget.ShadowFrameLayout;
import com.cylan.jiafeigou.widget.dialog.BaseDialog;
import com.cylan.jiafeigou.widget.dialog.SimpleDialogFragment;
import com.cylan.jiafeigou.widget.textview.WonderfulTitleHead;
import com.cylan.jiafeigou.widget.wheel.TimeWheelView;
import com.cylan.jiafeigou.widget.wheel.WheelView;
import com.cylan.superadapter.internal.SuperViewHolder;
import com.cylan.utils.ListUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;

import static com.cylan.jiafeigou.n.mvp.contract.record.DelayRecordContract.View.VIEW_LAUNCH_WAY_WONDERFUL;

public class HomeWonderfulFragmentExt extends BaseFragment<HomeWonderfulContract.Presenter> implements
        HomeWonderfulContract.View, SwipeRefreshLayout.OnRefreshListener,
        HomeWonderfulAdapter.WonderfulItemClickListener,
        HomeWonderfulAdapter.WonderfulItemLongClickListener,
        BaseDialog.BaseDialogAction,
        AppBarLayout.OnOffsetChangedListener,
        SharedElementCallBackListener,
        OnActivityReenterListener, TimeWheelView.OnTimeLineChangeListener {


    @BindView(R.id.fl_date_bg_head_wonder)
    FrameLayout flDateBgHeadWonder;
    @BindView(R.id.rV_wonderful_list)
    RecyclerView rVDevicesList;
    @BindView(R.id.fLayout_main_content_holder)
    SwipeRefreshLayout srLayoutMainContentHolder;
    @BindView(R.id.fLayoutHomeWonderfulHeaderContainer)
    FrameLayout fLayoutHomeHeaderContainer;
    @BindView(R.id.fLayout_date_head_wonder)
    FrameLayout fLayoutDateHeadWonder;
    @BindView(R.id.rl_top_head_wonder)
    RelativeLayout rlTopHeadWonder;
    @BindView(R.id.img_wonderful_title_cover)
    ImageView imgWonderfulTitleCover;
    @BindView(R.id.tv_date_item_head_wonder)
    WonderfulTitleHead tvDateItemHeadWonder;
    @BindView(R.id.imgWonderfulTopBg)
    ImageView imgWonderfulTopBg;
    @BindView(R.id.tv_title_head_wonder)
    TextView tvTitleHeadWonder;
    @BindView(R.id.fLayout_empty_view_container)
    FrameLayout mWonderfulEmptyViewContainer;
    @BindView(R.id.fragment_wonderful_empty)
    ViewGroup mWonderfulEmptyContainer;
    @BindView(R.id.fragment_wonderful_guide)
    ViewGroup mWonderfulGuideContainer;

    private WeakReference<TimeWheelView> wheelViewWeakReference;
    private WeakReference<ShareDialogFragment> shareDialogFragmentWeakReference;
    private WeakReference<SimpleDialogFragment> deleteDialogFragmentWeakReference;

    @BindView(R.id.tv_sec_title_head_wonder)
    TextView tvSecTitleHeadWonder;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.appbar)
    AppBarLayout appbar;

    /**
     * 加载更多
     */
    private boolean endlessLoading = true;


    private HomeWonderfulAdapter homeWonderAdapter;
    public boolean isShowTimeLine;
    private LinearLayoutManager mLinearLayoutManager;
    private View mContentView;
    private ShadowFrameLayout mParent;


    public static HomeWonderfulFragmentExt newInstance(Bundle bundle) {
        HomeWonderfulFragmentExt fragment = new HomeWonderfulFragmentExt();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void initViewAndListener() {
        homeWonderAdapter = new HomeWonderfulAdapter(getContext(), null, null);
        homeWonderAdapter.setWonderfulItemClickListener(this);
        homeWonderAdapter.setWonderfulItemLongClickListener(this);
        appbar.addOnOffsetChangedListener(this);
        srLayoutMainContentHolder.setNestedScrollingEnabled(false);
        initView();

        initProgressBarPosition();

        initHeaderView();

        initSomeViewMargin();
    }

    private SimpleDialogFragment initDeleteDialog() {
        if (deleteDialogFragmentWeakReference == null || deleteDialogFragmentWeakReference.get() == null) {
            //为删除dialog设置提示信息
            Bundle args = new Bundle();
            args.putString(BaseDialog.KEY_TITLE, "");
            args.putString(SimpleDialogFragment.KEY_LEFT_CONTENT, "");
            args.putString(SimpleDialogFragment.KEY_RIGHT_CONTENT, "");
            args.putString(SimpleDialogFragment.KEY_CONTENT_CONTENT,
                    this.getString(R.string.Tips_SureDelete));
            deleteDialogFragmentWeakReference = new WeakReference<>(SimpleDialogFragment.newInstance(null));
            deleteDialogFragmentWeakReference.get().setAction(this);
        }
        return deleteDialogFragmentWeakReference.get();
    }

    private ShareDialogFragment initShareDialog() {
        if (shareDialogFragmentWeakReference == null || shareDialogFragmentWeakReference.get() == null) {
            shareDialogFragmentWeakReference = new WeakReference<>(ShareDialogFragment.newInstance((Bundle) null));
        }
        return shareDialogFragmentWeakReference.get();
    }

    @Override
    public void onResume() {
        super.onResume();
        onTimeTick(JFGRules.getTimeRule());
    }

    @Override
    protected HomeWonderfulContract.Presenter onCreatePresenter() {
        return new HomeWonderfulPresenterImpl();
    }

    @Override
    protected int getContentViewID() {
        return R.layout.fragment_home_wonderful_ext;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dismissShareDialog();
        mPresenter.unregisterWechat();
    }

    private void dismissShareDialog() {
        if (getActivity() != null && getActivity().getSupportFragmentManager() != null) {
            Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag("ShareDialogFragment");
            if (fragment != null && fragment instanceof ShareDialogFragment) {
                ((ShareDialogFragment) fragment).dismiss();
            }
        }
    }

    private void initSomeViewMargin() {
        ViewUtils.setViewMarginStatusBar(tvTitleHeadWonder);
        ViewUtils.setViewMarginStatusBar(toolbar);
    }


    private void initView() {
        //添加Handler
        srLayoutMainContentHolder.setOnRefreshListener(this);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        rVDevicesList.setLayoutManager(mLinearLayoutManager);
        rVDevicesList.setAdapter(homeWonderAdapter);
        rVDevicesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int pastVisibleItems, visibleItemCount, totalItemCount;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                pastVisibleItems = mLinearLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0) { //check for scroll down
                    visibleItemCount = mLinearLayoutManager.getChildCount();
                    totalItemCount = mLinearLayoutManager.getItemCount();
                    if (endlessLoading) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            endlessLoading = false;
                            mPresenter.startLoadMore();
                            AppLogger.v("Last Item Wow !");
                            //Do pagination.. i.e. fetch new data
                        }
                    }
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                //同步更新时间轴的数据
                int position = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
                if (newState == RecyclerView.SCROLL_STATE_IDLE && position != -1) {
                    MediaBean item = homeWonderAdapter.getItem(position);
                    getWheelView().updateDay(item.time * 1000L);//蛋疼
                    tvDateItemHeadWonder.setText(TimeUtils.getDayString(item.time * 1000L));

                }
            }
        });
    }

    private void initHeaderView() {
        tvDateItemHeadWonder.post(() -> tvDateItemHeadWonder.setText(TimeUtils.getTodayString()));
    }


    /**
     * 初始化,progressBar的位置.
     */
    private void initProgressBarPosition() {
        srLayoutMainContentHolder.setColorSchemeColors(R.color.COLOR_CACACA);
    }

    @UiThread
    @Override
    public void onMediaListRsp(List<MediaBean> resultList) {
        endlessLoading = true;
        srLayoutMainContentHolder.setRefreshing(false);
        if (resultList == null || resultList.size() == 0) {
//            homeWonderAdapter.clear();
            return;
        }
        handleFootView();
        homeWonderAdapter.addAll(resultList);
        addFootView();
        srLayoutMainContentHolder.setNestedScrollingEnabled(homeWonderAdapter.getCount() > 2);

    }

    private void handleFootView() {
        final int itemCount = homeWonderAdapter.getItemCount();
        if (itemCount > 0 && homeWonderAdapter.getItem(itemCount - 1).msgType == MediaBean.TYPE_LOAD) {
            homeWonderAdapter.remove(itemCount - 1);
        }
    }

    private void addFootView() {
        homeWonderAdapter.add(MediaBean.getEmptyLoadTypeBean());
    }

    @Override
    public void onHeadBackgroundChang(int daytime) {
        imgWonderfulTopBg.setBackgroundResource(daytime == 0 ? R.drawable.bg_wonderful_daytime : R.drawable.bg_wonderful_night);
    }

    @Override
    public void onTimeLineDataUpdate(List<Long> wheelViewDataSet) {
        View view = getWheelView();
        if (view == null)
            return;
        ((TimeWheelView) view).append(wheelViewDataSet);
        ((TimeWheelView) view).addTimeLineListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onTimeTick(int dayTime) {

        //需要优化
        int drawableId = dayTime == JFGRules.RULE_DAY_TIME
                ? R.drawable.bg_wonderful_daytime : R.drawable.bg_wonderful_night;
        Glide.with(this)
                .load(drawableId)
                .asBitmap()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .into(imgWonderfulTopBg);
        AppLogger.d("onTimeTick: " + dayTime);
    }

    @Override
    public void onPageScrolled() {
        final TimeWheelView view = getWheelView();
        if (view != null && view.isShown()) {
            AppLogger.e("onPageScrolled");
            AnimatorUtils.slide(view, new AnimatorUtils.OnEndListener() {

                @Override
                public void onAnimationEnd(boolean gone) {
                    if (!gone) view.showTimeLinePopWindow();
                    if (isShowTimeLine) {
                        hideTimeLine();
                    } else {
                        showTimeLine();
                    }
                }

                @Override
                public void onAnimationStart(boolean gone) {
                    if (gone) view.hideTimeLinePopWindow();
                }
            });
        }
        if (srLayoutMainContentHolder != null)
            srLayoutMainContentHolder.setRefreshing(false);
    }

    @Override
    public void onWechatCheckRsp(boolean installed) {

    }

    @OnClick(R.id.item_wonderful_to_start)
    public void openWonderful() {
        if (GlobalDataProxy.getInstance().isOnline()) {//在线表示已登录
            Intent intent = new Intent(getActivityContext(), DelayRecordActivity.class);
            intent.putExtra(JConstant.VIEW_CALL_WAY, VIEW_LAUNCH_WAY_WONDERFUL);
            intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, mUUID);
            startActivity(intent);
        } else {//不在线表示还未登录
            RxBus.getCacheInstance().post(new RxEvent.NeedLoginEvent(null));
        }
    }

    @OnClick(R.id.tv_wonderful_item_share)
    public void shareWonderful() {//分享官方演示视频
        MediaBean bean = MediaBean.getGuideBean();
        onShareWonderfulContent(bean);
    }

    @OnClick(R.id.iv_wonderful_item_content)
    public void viewWonderful(View view) {
        MediaBean bean = MediaBean.getGuideBean();
        ArrayList<Parcelable> list = new ArrayList<>();
        list.add(bean);
        onEnterWonderfulContent(list, 0, view);
    }

    @OnClick(R.id.tv_wonderful_item_delete)
    public void removeAnymore() {
        mPresenter.removeGuideAnymore();
    }

    @Override
    public void chooseEmptyView(int type) {
        switch (type) {
            case VIEW_TYPE_HIDE: {//hide
                mWonderfulEmptyViewContainer.setVisibility(View.GONE);
            }
            break;
            case VIEW_TYPE_EMPTY: {//empty
                mWonderfulEmptyViewContainer.setVisibility(View.VISIBLE);
                mWonderfulGuideContainer.setVisibility(View.GONE);
                mWonderfulEmptyContainer.setVisibility(View.VISIBLE);
            }
            break;
            case VIEW_TYPE_GUIDE: {//guide
                mWonderfulEmptyViewContainer.setVisibility(View.VISIBLE);
                mWonderfulEmptyContainer.setVisibility(View.GONE);
                mWonderfulGuideContainer.setVisibility(View.VISIBLE);
            }
            break;
        }
    }

    @Override
    public void onRefresh() {
        mPresenter.startRefresh();
        //不使用post,因为会泄露
        srLayoutMainContentHolder.setRefreshing(true);
    }

    private void onEnterWonderfulContent(ArrayList<? extends Parcelable> list, int position, View v) {
        final Intent intent = new Intent(getActivity(), MediaActivity.class);
        // Pass data object in the bundle and populate details activity.
        intent.putParcelableArrayListExtra(JConstant.KEY_SHARED_ELEMENT_LIST, list);
        intent.putExtra(JConstant.KEY_SHARED_ELEMENT_STARTED_POSITION, position);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mParent = (ShadowFrameLayout) v.getParent();
            mParent.adjustSize(true);
            ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), v, v.getTransitionName());
            startActivity(intent, compat.toBundle());
        } else {
            startActivity(intent);
        }
        AppLogger.d("transition:getName " + ViewCompat.getTransitionName(v));

    }

    private void onShareWonderfulContent(MediaBean bean) {
        boolean installed = false;
        installed = mPresenter.checkWechat();
        if (!installed) {
            Toast.makeText(getActivity(), "微信没有安装", Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(ShareDialogFragment.KEY_MEDIA_CONTENT, bean);
        ShareDialogFragment fragment = initShareDialog();
        fragment.setArguments(bundle);
        fragment.show(getActivity().getSupportFragmentManager(), "ShareDialogFragment");
    }

    private void onDeleteWonderfulContent(MediaBean bean, int position) {
        SimpleDialogFragment deleteF = initDeleteDialog();
        deleteF.setValue(position);
        deleteF.setArguments(new Bundle());
        deleteF.show(getActivity().getSupportFragmentManager(), "deleteDialogFragment");
    }

    @Override
    public void onClick(final View v) {
        final int position = ViewUtils.getParentAdapterPosition(rVDevicesList, v, R.id.lLayout_item_wonderful);
        if (position < 0 || position > homeWonderAdapter.getCount()) {
            AppLogger.d("woo,position is invalid: " + position);
            return;
        }
        switch (v.getId()) {
            case R.id.iv_wonderful_item_content:
                ArrayList<? extends Parcelable> list = (ArrayList<? extends Parcelable>) homeWonderAdapter.getList();
                onEnterWonderfulContent(list, position, v);
                break;
            case R.id.tv_wonderful_item_share:
                MediaBean bean = homeWonderAdapter.getItem(position);
                onShareWonderfulContent(bean);
                break;
            case R.id.tv_wonderful_item_delete:
                onDeleteWonderfulContent(null, position);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        final int position = ViewUtils.getParentAdapterPosition(rVDevicesList, v, R.id.lLayout_item_wonderful);
        if (position < 0 || position > homeWonderAdapter.getCount()) {
            AppLogger.d("woo,position is invalid: " + position);
            return false;
        }
        deleteItem(position);
        return true;
    }


    private void deleteItem(final int position) {
        initDeleteDialog();
        SimpleDialogFragment fragment = deleteDialogFragmentWeakReference.get();
        fragment.setValue(position);
        fragment.show(getActivity().getSupportFragmentManager(), "ShareDialogFragment");
    }

    @OnClick(R.id.fLayout_date_head_wonder)
    public void onClick() {
        if (true) return;//doNothing
        final TimeWheelView view = getWheelView();
        if (view != null && !ListUtils.isEmpty(homeWonderAdapter.getList())) {
            if (!view.isShown()) {
                int position = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
                long time = homeWonderAdapter.getItem(position).time;
                view.setCurrentDay(time);
            }
            AnimatorUtils.slide(view, new AnimatorUtils.OnEndListener() {

                @Override
                public void onAnimationEnd(boolean gone) {
                    if (!gone) view.showTimeLinePopWindow();
                    if (isShowTimeLine) {
                        hideTimeLine();
                    } else {
                        showTimeLine();
                    }
                }

                @Override
                public void onAnimationStart(boolean gone) {
                    if (gone) view.hideTimeLinePopWindow();
                }
            });
        } else return;
        tvDateItemHeadWonder.setTimeLineShow(isShowTimeLine);
        tvDateItemHeadWonder.setBackgroundToRight();
    }


    /**
     * {@link WheelView}
     *
     * @return
     */
    private TimeWheelView getWheelView() {
        if (wheelViewWeakReference == null || wheelViewWeakReference.get() == null) {
            if (getActivity() != null) {
                TimeWheelView wheelView = (TimeWheelView) getActivity().findViewById(R.id.wv_wonderful_timeline);
                wheelViewWeakReference = new WeakReference<>(wheelView);
                wheelView.addTimeLineListener(this);
            }
        }
        return wheelViewWeakReference.get();
    }

    private void showTimeLine() {
        //do something presenter.xxx
        isShowTimeLine = true;
    }

    private void hideTimeLine() {
        //do something presenter.xxx
        isShowTimeLine = false;
    }


    @Override
    public void onDialogAction(int id, Object value) {
        if (id == R.id.tv_dialog_btn_right)
            return;
        if (value == null || !(value instanceof Integer)) {
            AppLogger.i("value is null :" + value);
            return;
        }
        final int position = (int) value;
        if (position >= 0 && position < homeWonderAdapter.getCount()) {
            long time = homeWonderAdapter.getItem(position).time;
            mPresenter.deleteTimeline(time);
            homeWonderAdapter.remove((Integer) value);
            getWheelView().delete(time);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        final float ratio = (appbar.getTotalScrollRange() + verticalOffset) * 1.0f
                / appbar.getTotalScrollRange();
        final float alpha = 1.0f - ratio;
        if (imgWonderfulTitleCover.getAlpha() != alpha) {
            imgWonderfulTitleCover.setAlpha(alpha);
        }
    }


    @Override
    public void onSharedElementCallBack(List<String> names, Map<String, View> sharedElements) {
        if (mTmpReenterState != null) {
            int startingPosition = mTmpReenterState.getInt(JConstant.EXTRA_STARTING_ALBUM_POSITION);
            int currentPosition = mTmpReenterState.getInt(JConstant.EXTRA_CURRENT_ALBUM_POSITION);

//            if (startingPosition != currentPosition) {
            // If startingPosition != currentPosition the user must have swiped to activity_cloud_live_mesg_video_talk_item
            // different page in the DetailsActivity. We must update the shared element
            // so that the correct one falls into place.
            String newTransitionName = currentPosition + JConstant.KEY_SHARED_ELEMENT_TRANSITION_NAME_SUFFIX;
            SuperViewHolder holder = (SuperViewHolder) rVDevicesList.findViewHolderForAdapterPosition(currentPosition);
            View newSharedElement;
            if (holder != null) {
                newSharedElement = holder.getView(R.id.iv_wonderful_item_content);
            } else {
                newSharedElement = mWonderfulGuideContainer.findViewById(R.id.iv_wonderful_item_content);
            }
            ShadowFrameLayout parent = (ShadowFrameLayout) newSharedElement.getParent();
            if (mParent != parent) {
                mParent.adjustSize(false);
                parent.adjustSize(true);
            }
            AppLogger.d("transition newTransitionName: " + newTransitionName);
            AppLogger.d("transition newSharedElement: " + newSharedElement);
            if (newSharedElement != null) {
                names.clear();
                names.add(newTransitionName);
                sharedElements.clear();
                sharedElements.put(newTransitionName, newSharedElement);
            }
            mTmpReenterState = null;
        } else {
            // If mTmpReenterState is null, then the activity is exiting.
//            View navigationBar = findViewById(android.R.id.navigationBarBackground);
//            View statusBar = findViewById(android.R.id.statusBarBackground);
//            if (navigationBar != null) {
//                names.add(navigationBar.getTransitionName());
//                sharedElements.put(navigationBar.getTransitionName(), navigationBar);
//            }
//            if (statusBar != null) {
//                names.add(statusBar.getTransitionName());
//                sharedElements.put(statusBar.getTransitionName(), statusBar);
//            }
        }
    }

    @Override
    public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
        View view = sharedElements.get(0);
        if (view != null) {
            final ShadowFrameLayout parent = (ShadowFrameLayout) view.getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    parent.adjustSize(false);
                }
            });
        }
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        AppLogger.d("transition onActivityReenter");
        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(JConstant.EXTRA_STARTING_ALBUM_POSITION);
        int currentPosition = mTmpReenterState.getInt(JConstant.EXTRA_CURRENT_ALBUM_POSITION);
        if (startingPosition != currentPosition) {
//            rVDevicesList.scrollToPosition(currentPosition);
            ((LinearLayoutManager) rVDevicesList.getLayoutManager()).scrollToPositionWithOffset(currentPosition, 0);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().postponeEnterTransition();
        }
        rVDevicesList.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rVDevicesList.getViewTreeObserver().removeOnPreDrawListener(this);
                rVDevicesList.requestLayout();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().startPostponedEnterTransition();
                }
                return true;
            }
        });
    }

    private Bundle mTmpReenterState;

    @Override
    public void onTimeLineChanged(long newTime) {
        List<MediaBean> list = homeWonderAdapter.getList();
        for (MediaBean bean : list) {
            if (bean.time == newTime / 1000) {
                int index = list.indexOf(bean);
                int position = mLinearLayoutManager.findFirstVisibleItemPosition();
                rVDevicesList.smoothScrollToPosition(index > position ? index + 1 : index);
                break;
            }
        }
    }
}
