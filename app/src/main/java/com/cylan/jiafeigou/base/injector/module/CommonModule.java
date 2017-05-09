package com.cylan.jiafeigou.base.injector.module;

import com.cylan.jfgapp.interfases.AppCmd;
import com.cylan.jfgapp.jni.JfgAppCmd;
import com.cylan.jiafeigou.base.module.BaseAppCallBackHolder;
import com.cylan.jiafeigou.base.module.BasePresenterInjector;
import com.cylan.jiafeigou.base.module.BasePropertyParser;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.base.module.IHttpApi;
import com.cylan.jiafeigou.base.module.ImageFileConverterFactory;
import com.cylan.jiafeigou.base.view.IPropertyParser;
import com.cylan.jiafeigou.base.view.JFGSourceManager;
import com.cylan.jiafeigou.cache.db.impl.BaseDBHelper;
import com.cylan.jiafeigou.cache.db.impl.BaseDPTaskDispatcher;
import com.cylan.jiafeigou.cache.db.impl.BaseDPTaskFactory;
import com.cylan.jiafeigou.cache.db.view.IDBHelper;
import com.cylan.jiafeigou.cache.db.view.IDPTaskDispatcher;
import com.cylan.jiafeigou.cache.db.view.IDPTaskFactory;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.support.OptionsImpl;
import com.cylan.jiafeigou.support.Security;
import com.cylan.jiafeigou.utils.PathGetter;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by yanzhendong on 2017/4/12.
 */
@Module
public class CommonModule {

    @Provides
    @Singleton
    public static JFGSourceManager provideSourceManager() {
        return new DataSourceManager();
    }

    @Provides
    @Singleton
    public static IDBHelper provideDBHelper() {
        return new BaseDBHelper();
    }

    @Provides
    @Singleton
    public static IDPTaskDispatcher provideTaskDispatcher() {
        return new BaseDPTaskDispatcher();
    }

    @Provides
    @Singleton
    public static IDPTaskFactory provideTaskFactory() {
        return new BaseDPTaskFactory();
    }

    @Provides
    @Singleton
    public static IPropertyParser providePropertyParser() {
        return new BasePropertyParser();
    }

    @Provides
    @Singleton
    public static BasePresenterInjector provideBasePresenterInject(JFGSourceManager manager, IDPTaskDispatcher dispatcher, AppCmd appCmd) {
        return new BasePresenterInjector(manager, dispatcher, appCmd);
    }

    @Provides
    @Singleton
    public static BaseAppCallBackHolder provideAppCallBackHolder() {
        return new BaseAppCallBackHolder();
    }

    @Provides
    @Singleton
    public static AppCmd provideAppCmd() {
        return JfgAppCmd.getInstance();
    }

    @Provides
    @Singleton
    @Named("VKey")
    public static String provideVKey() {
        return Security.getVKey();
    }

    @Provides
    @Singleton
    @Named("Vid")
    public static String provideVid() {
        return Security.getVId();
    }

    @Provides
    @Singleton
    @Named("ServerAddress")
    public static String provideServerAddress() {
        return OptionsImpl.getServer();
    }

    @Provides
    @Singleton
    @Named("LogPath")
    public static String provideLogPath() {
        return JConstant.WORKER_PATH;
    }

    @Provides
    @Singleton
    @Named("CrashPath")
    public static String provideCrashPath() {
        return PathGetter.createPath(JConstant.CRASH_PATH);
    }

    @Provides
    @Singleton
    public static IHttpApi provideHttpApi(@Named("IHttpApi") Retrofit retrofit) {
        return retrofit.create(IHttpApi.class);
    }

    @Provides
    @Singleton
    public static OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    @Provides
    @Singleton
    @Named("IHttpApi")
    public static Retrofit provideRetrofit(OkHttpClient okHttpClient, @Named("IHttpApiBaseUrl") String baseUrl) {
        return new Retrofit.Builder().client(okHttpClient)
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ImageFileConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    @Named("IHttpApiBaseUrl")
    public static String provideHttpApiBaseUrl() {
        return "http://192.168.103.222/";
    }

}
