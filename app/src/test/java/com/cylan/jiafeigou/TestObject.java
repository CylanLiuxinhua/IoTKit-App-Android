package com.cylan.jiafeigou;

import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.utils.RandomUtils;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

/**
 * Created by hds on 17-10-23.
 */

public class TestObject {


    @Test
    public void mockVisitors() {
        ArrayList<DpMsgDefine.Visitor> mockList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DpMsgDefine.Visitor visitor = new DpMsgDefine.Visitor();
            visitor.lastTime = System.currentTimeMillis() - RandomUtils.getRandom(24) * 3600;
            visitor.personId = i + "";
            visitor.personName = i + "," + i;
            visitor.faceIdList = getProvinces();
            mockList.add(visitor);
        }
        System.out.println(mockList);

        ArrayList<DpMsgDefine.StrangerVisitor> strangerVisitors = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            DpMsgDefine.StrangerVisitor visitor = new DpMsgDefine.StrangerVisitor();
            visitor.faceId = 20 + "" + i;
            visitor.lastTime = System.currentTimeMillis() - RandomUtils.getRandom(24) * 3600;
            strangerVisitors.add(visitor);
        }
        System.out.println(strangerVisitors);
    }

    private ArrayList<String> getProvinces() {
        try {

            JSONObject object = new JSONObject(arrays);
            System.out.println("object:" + object);
            String[] array = arrays.split(",");
            int tCnt = RandomUtils.getRandom(array.length);
            tCnt = Math.max(1, tCnt);
            ArrayList<String> list = new ArrayList<>(tCnt);
            for (int i = 0; i < array.length; i++) {
                if (list.size() < tCnt) {
                    list.add(array[i]);
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final String arrays = "北京市,天津市,河北省,山西省,内蒙古自治区,辽宁省,吉林省,黑龙江省,上海市,江苏省," +
            "浙江省,安徽省,福建省,江西省,山东省,河南省,湖北省,湖南省" +
            ",广东省,广西壮族自治区,海南省,重庆市,四川省,贵州省,云南省" +
            ",西藏自治区,陕西省,甘肃省,青海省,宁夏回族自治区,新疆维吾尔自治区,台湾省,香港特别行政区,澳门特别行政区";
}
