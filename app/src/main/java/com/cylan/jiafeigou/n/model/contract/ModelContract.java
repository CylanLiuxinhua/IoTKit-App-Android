package com.cylan.jiafeigou.n.model.contract;

import com.cylan.jiafeigou.n.model.BeanInfoLogin;

/**
 * Created by chen on 5/25/16.
 */
public interface ModelContract {
     interface SplashModelOps {
         void splashTimeda();

         void finishAppDalayda();
     }

    interface LoginModelOps {
        String executeLoginda(BeanInfoLogin infoLogin);
    }
}