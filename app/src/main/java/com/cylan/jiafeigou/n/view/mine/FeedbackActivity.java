package com.cylan.jiafeigou.n.view.mine;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cylan.entity.jniCall.JFGAccount;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.cache.db.module.FeedBackBean;
import com.cylan.jiafeigou.misc.AlertDialogManager;
import com.cylan.jiafeigou.n.BaseFullScreenFragmentActivity;
import com.cylan.jiafeigou.n.base.BaseApplication;
import com.cylan.jiafeigou.n.mvp.contract.home.FeedBackContract;
import com.cylan.jiafeigou.n.mvp.impl.home.FeedbackImpl;
import com.cylan.jiafeigou.n.view.adapter.FeedbackAdapter;
import com.cylan.jiafeigou.support.badge.Badge;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.support.softkeyboard.util.KPSwitchConflictUtil;
import com.cylan.jiafeigou.support.softkeyboard.util.KeyboardUtil;
import com.cylan.jiafeigou.support.softkeyboard.widget.KPSwitchFSPanelLinearLayout;
import com.cylan.jiafeigou.support.superadapter.internal.SuperViewHolder;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.IMEUtils;
import com.cylan.jiafeigou.utils.ListUtils;
import com.cylan.jiafeigou.utils.NetUtils;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.widget.CustomToolbar;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;

@Badge(parentTag = "HomeMineHelpFragment")
//@RuntimePermissions
public class FeedbackActivity extends BaseFullScreenFragmentActivity<FeedBackContract.Presenter>
        implements FeedBackContract.View {

    @BindView(R.id.custom_toolbar)
    CustomToolbar customToolbar;
    @BindView(R.id.rv_home_mine_suggestion)
    RecyclerView mRvMineSuggestion;
    @BindView(R.id.iv_loading_rotate)
    ImageView ivLoadingRotate;
    @BindView(R.id.fl_loading_container)
    FrameLayout flLoadingContainer;
    @BindView(R.id.et_home_mine_suggestion)
    EditText mEtSuggestion;
    @BindView(R.id.tv_home_mine_suggestion)
    TextView tvHomeMineSuggestion;
    @BindView(R.id.rl_home_mine_suggestion_bottom)
    RelativeLayout rlHomeMineSuggestionBottom;
    @BindView(R.id.panel_root)
    KPSwitchFSPanelLinearLayout panelRoot;

    private FeedbackAdapter suggestionAdapter;
    private FeedBackContract.Presenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_mine_help_suggestion);
        ButterKnife.bind(this);
        basePresenter = new FeedbackImpl(this);
        initKeyBoard();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRvMineSuggestion.setLayoutManager(layoutManager);
        suggestionAdapter = new FeedbackAdapter(getContext(), null, null);
        mRvMineSuggestion.setAdapter(suggestionAdapter);

    }

    @Override
    protected void onStop() {
        super.onStop();
        BaseApplication.getAppComponent().getTreeHelper().markNodeRead(HomeMineHelpFragment.class.getSimpleName());
    }

    @Override
    public void onBackPressed() {
        finishExt();
    }

    @OnClick({R.id.tv_toolbar_icon, R.id.tv_toolbar_right, R.id.tv_home_mine_suggestion})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_toolbar_icon:
                IMEUtils.hide(this);
                onBackPressed();
                break;
            case R.id.tv_toolbar_right:
                //弹出对话框
                if (suggestionAdapter.getItemCount() == 0) {
                    return;
                }
                showDialog();
                break;
            case R.id.tv_home_mine_suggestion:
                if (mEtSuggestion.getText().toString().length() < 10) {
                    ToastUtil.showNegativeToast(getString(R.string.Tap3_Feedback_TextFail));
                    return;
                }
                if (NetUtils.getJfgNetType() == 0) {
                    ToastUtil.showToast(getString(R.string.NoNetworkTips));
                    return;
                }
                addInputItem();
                mEtSuggestion.setText("");
                break;
        }
    }

    /**
     * 弹出对话框
     */
    private void showDialog() {
        AlertDialogManager.getInstance().showDialog(this, getString(R.string.Tap3_Feedback_ClearTips), getString(R.string.Tap3_Feedback_ClearTips),
                getString(R.string.Tap3_Feedback_Clear), (DialogInterface dialog, int which) -> {
                    presenter.onClearAllTalk();
                    suggestionAdapter.clear();
                    suggestionAdapter.notifyDataSetHasChanged();
                }, getString(R.string.CANCEL), null);
    }

    /**
     * 用户进行反馈时添加一个自动回复的条目
     */
    public void addAutoReply() {
        FeedBackBean autoReplyBean = new FeedBackBean();
        autoReplyBean.setViewType(0);
        autoReplyBean.setContent(getString(R.string.Tap3_Feedback_AutoReply));
        autoReplyBean.setMsgTime(System.currentTimeMillis());
        autoReplyBean.setAccount(getAccount());
        suggestionAdapter.add(autoReplyBean);
        mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1);
        presenter.saveIntoDb(autoReplyBean);
    }

    private String getAccount() {
        JFGAccount account = BaseApplication.getAppComponent().getSourceManager().getJFGAccount();
        return account != null && !TextUtils.isEmpty(account.getAccount()) ? account.getAccount() : "default";
    }

    /**
     * recycleview显示用户输入的条目
     */
    public void addInputItem() {
        final FeedBackBean suggestionBean = new FeedBackBean();
        suggestionBean.setViewType(1);
        suggestionBean.setContent(mEtSuggestion.getText().toString());
        suggestionBean.setMsgTime(System.currentTimeMillis());
        suggestionBean.setAccount(getAccount());
        presenter.sendFeedBack(suggestionBean)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ret -> {
                    suggestionAdapter.add(suggestionBean);
                    mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1);
                    addAutoReply();
                }, AppLogger::e);
        presenter.saveIntoDb(suggestionBean);
    }

    public FeedBackBean addSystemAutoReply() {
        FeedBackBean autoReplyBean = new FeedBackBean();
        autoReplyBean.setViewType(0);
        autoReplyBean.setContent(getString(R.string.Tap3_Feedback_AutoTips));
        autoReplyBean.setMsgTime(System.currentTimeMillis());
        autoReplyBean.setAccount(getAccount());
        presenter.saveIntoDb(autoReplyBean);
        return autoReplyBean;
    }


    /**
     * 初始化显示列表
     *
     * @param list
     */
    @Override
    public void initList(List<FeedBackBean> list) {
        if (ListUtils.isEmpty(list)) {
            //列表为空插入一条系统提示
            list = new ArrayList<>();
            list.add(addSystemAutoReply());
        }

        suggestionAdapter.clear();
        suggestionAdapter.addAll(list);
        //从最后一行显示
        mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1);

        suggestionAdapter.setOnResendFeedBack((holder, item, position) -> {
            if (NetUtils.getNetType(ContextUtils.getContext()) == -1) {
                ToastUtil.showToast(getString(R.string.NO_NETWORK_4));
                return;
            }
            showResendFeedBackDialog(holder, item, position);
        });
    }

    @Override
    public void updateItem(FeedBackBean bean) {
        final int count = suggestionAdapter == null ? 0 : suggestionAdapter.getCount();
        final int index = suggestionAdapter == null || suggestionAdapter.getCount() == 0 ? -1
                : suggestionAdapter.getList().indexOf(bean);
        if (index < 0 || index > count) return;
        suggestionAdapter.notifyItemChanged(index);
    }

    @Override
    public void appendList(List<FeedBackBean> list) {
        if (suggestionAdapter == null) return;
        suggestionAdapter.addAll(list);
        mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1);
    }

    private void showResendFeedBackDialog(SuperViewHolder holder, FeedBackBean item, int position) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.ANEW_SEND));
        b.setNegativeButton(getString(R.string.Button_No), (DialogInterface dialog, int which) -> dialog.dismiss());
        b.setPositiveButton(getString(R.string.Button_Yes), (DialogInterface dialog, int which) -> {
            presenter.sendFeedBack(item)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(ret -> {
                        final int index = suggestionAdapter.getList().indexOf(item);
                        if (index < 0) return;
                        suggestionAdapter.notifyItemChanged(index);
                    }, AppLogger::e);
            dialog.dismiss();
        });
        AlertDialogManager.getInstance().showDialog("showResendFeedBackDialog", this, b);
    }

    @Override
    public void setPresenter(FeedBackContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public String getUuid() {
        return null;
    }

    private void initKeyBoard() {
        KeyboardUtil.attach(this, panelRoot, isShowing -> {
            if (suggestionAdapter == null) return;
            mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1);
        });
        KPSwitchConflictUtil.attach(panelRoot, mEtSuggestion,
                switchToPanel -> {
                    AppLogger.w("KPSwitchConflictUtil:" + switchToPanel);
                    if (switchToPanel) {
                        mEtSuggestion.clearFocus();
                    } else {
                        mEtSuggestion.requestFocus();
                    }
                });

        mRvMineSuggestion.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                KPSwitchConflictUtil.hidePanelAndKeyboard(panelRoot);
            }
            return false;
        });
    }

}
