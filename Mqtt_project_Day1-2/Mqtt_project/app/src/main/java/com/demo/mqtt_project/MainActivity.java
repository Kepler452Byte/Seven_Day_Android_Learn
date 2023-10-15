package com.demo.mqtt_project;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/*
第二节：
控件（常用的）：   控件（扩展）拖动条 进度条 浏览器框 地图 单选框 复选框
按钮 图片按钮
文本框
编辑框
图片框
选择开关
 wrap_content   自适应
 match_parent    充满父控件
 android:id="@+id/bt_1"   用来和JAVA文件通讯或者说是 绑定事件的
LinearLayout  线性布局
android:orientation="vertical"  设置布局方向  vertical垂直  horizontal水平
android:layout_margin="10dp"    边距
 */


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //这里是界面打开后 最先运行的地方
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 对应界面UI
        //一般先用来进行界面初始化 控件初始化  初始化一些参数和变量。。。。。
        //不恰当比方    类似于 单片机的   main函数
        //  第二节开始  目前Java0基础

    }
}
