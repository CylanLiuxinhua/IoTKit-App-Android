package com.cylan.jiafeigou.n.view.login;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.utils.ActivityUtils;
import com.cylan.jiafeigou.utils.AnimatorUtils;
import com.cylan.jiafeigou.utils.ColorPhrase;
import com.superlog.SLog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

/**
 * Created by lxh on 16-6-8.
 */

public class RegisterByPhoneFragment extends LoginBaseFragment {


    @BindView(R.id.et_register_username)
    EditText etRegisterUsername;
    @BindView(R.id.iv_register_clear_username)
    ImageView ivRegisterClearUsername;
    @BindView(R.id.et_register_code)
    EditText etRegisterCode;
    @BindView(R.id.tv_register_reciprocal_time)
    TextView tvRegisterReciprocalTime;
    @BindView(R.id.lLayout_input_code)
    LinearLayout lLayoutInputCode;
    @BindView(R.id.tv_forget_pwd_submit)
    TextView tvCommit;
    @BindView(R.id.tv_register_user_agreement)
    TextView tvRegisterUserAgreement;
    @BindView(R.id.lLayout_register_input)
    LinearLayout lLayoutRegisterInput;
    @BindView(R.id.tv_register_switch)
    TextView tvRegisterSwitch;


    private boolean isVerifyTime = false; //验证码有效时间内

    public static RegisterByPhoneFragment newInstance(Bundle bundle) {
        RegisterByPhoneFragment fragment = new RegisterByPhoneFragment();
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onResume() {
        super.onResume();
        AnimatorUtils.viewTranslationX(tvRegisterSwitch, true, 0, 800, 0, 500);
        AnimatorUtils.viewTranslationX(lLayoutRegisterInput, true, 0, 800, 0, 500);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_by_phone_layout, container, false);
        ButterKnife.bind(this, view);
        initView();
        addOnTouchListener(view);
        return view;
    }


    @OnClick(R.id.tv_forget_pwd_submit)
    public void regCommit(View view) {
        //如果是手机号，要显示验证码
        if (!isVerifyTime) {
            isVerifyTime = true;
            lLayoutInputCode.setVisibility(View.VISIBLE);
            tvRegisterUserAgreement.setVisibility(View.GONE);
            getCode();
            tvCommit.setText("继续");
            setViewEnableStyle(tvCommit, false);
        } else {
            SetPwdFragment fragment = SetPwdFragment.newInstance(null);
            ActivityUtils.addFragmentToActivity(getChildFragmentManager(), fragment, R.id.rLayout_register);
        }
    }


    private void initView() {
        String str = tvRegisterUserAgreement.getText().toString();
        CharSequence chars = ColorPhrase.from(str).withSeparator("{}").innerColor(getResources().getColor(R.color.color_4b9fd5))
                .outerColor(getResources().getColor(R.color.color_999999)).format();
        tvRegisterReciprocalTime.setText(chars);
    }


    private void getCode() {
        tvRegisterReciprocalTime.setTextColor(getResources().getColor(R.color.color_d8d8d8)); //改为灰色
        tvRegisterReciprocalTime.setEnabled(false);
        timer.start();
    }


    /***
     * 账号变化
     *
     * @param s
     * @param start
     * @param before
     * @param count
     */

    @OnTextChanged(R.id.et_register_username)
    public void onUserNameChange(CharSequence s, int start, int before, int count) {
        boolean flag = TextUtils.isEmpty(s);
        setViewEnableStyle(tvCommit, !flag);
        ivRegisterClearUsername.setVisibility(flag ? View.GONE : View.VISIBLE);
    }

    @OnTextChanged(R.id.et_register_code)
    public void onCodeChange(CharSequence s, int start, int before, int count) {
        boolean flag = TextUtils.isEmpty(s);
        setViewEnableStyle(tvCommit, !flag);
    }

    @OnClick(R.id.iv_register_clear_username)
    public void clearUserName(View view) {
        etRegisterUsername.getText().clear();
    }


    /**
     * 切换注册方式
     *
     * @param view
     */
    @OnClick(R.id.tv_register_switch)
    public void switchRegisterType(View view) {
        RegisterByMailFragment fragment = RegisterByMailFragment.newInstance(null);
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fLayout_login_container, fragment, "register").commit();
    }

    /**
     * 重新获取验证码
     *
     * @param view
     */
    @OnClick(R.id.tv_register_reciprocal_time)
    public void reGetPhoneVerifyCode(View view) {
        if (tvRegisterReciprocalTime.isEnabled()) {
            getCode();
        }
    }


    @Override
    public void onAttach(Context context) {
        initParentFragmentView();
        super.onAttach(context);
    }

    private void initParentFragmentView() {
        LoginModelFragment fragment = (LoginModelFragment) getActivity()
                .getSupportFragmentManager().getFragments().get(0);
        fragment.tvTopCenter.setText("注册");
        fragment.tvTopRight.setText("登录");
        fragment.tvTopRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginFragment();
            }
        });
    }


    private void showLoginFragment() {
        LoginFragment fragment = (LoginFragment) getFragmentManager()
                .findFragmentByTag("login");
        if (fragment == null) {
            fragment = LoginFragment.newInstance(null);
        }
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fLayout_login_container, fragment, "login").commit();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        timer.cancel();
    }


    CountDownTimer timer = new CountDownTimer(90 * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            tvRegisterReciprocalTime.setText(millisUntilFinished / 1000 + "S");
            SLog.i("millisUntilFinished:" + millisUntilFinished / 1000);
        }

        @Override
        public void onFinish() {
            tvRegisterReciprocalTime.setTextColor(getResources().getColor(R.color.color_4b9fd5)); //改为蓝色
            tvRegisterReciprocalTime.setText("重新获取");
            tvRegisterReciprocalTime.setEnabled(true);
        }
    };
}
