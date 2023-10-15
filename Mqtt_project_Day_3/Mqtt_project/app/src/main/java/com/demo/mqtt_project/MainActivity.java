package com.demo.mqtt_project;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/*
//  第二节开始  目前Java0基础
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

第三节开始   目前java 0基础
安卓开发  要多调试  多刷程序（因为你不知道你的APP程序会什么时候崩溃！！！！）
java里面的操作  大部分都类似于单片机的函数

控件的ID  是java文件与XML文件通讯的介质  类似于控件的 号码牌（唯一）
按钮：
单击事件有很多种实现方法  咱们讲最简单最常用的一种

按钮单机    用来发送命令控制硬件 例如：open door
文本框更新数据   用来接收硬件上报的传感器值  例如：温度 25.6


咱们 不凭空的学安卓开发  咱们是有目的的 直接面向项目和实战。
剩下的交给举一反三
 */


public class MainActivity extends AppCompatActivity {
    private Button btn_1;  //类似于单片机开发里面的   参数初始化
    private ImageView image_1;
    private TextView text_test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //这里是界面打开后 最先运行的地方
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 对应界面UI
        //一般先用来进行界面初始化 控件初始化  初始化一些参数和变量。。。。。
        //不恰当比方    类似于 单片机的   main函数

        btn_1 = findViewById(R.id.btn_1); // 寻找xml里面真正的id  与自己定义的id绑定
        btn_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 这里就是单机之后执行的地方
                // 玩单片机  最常用的就是调试  printf（“hello”）
                System.out.println("hello");
                //更直观的方法   用弹窗：toast
                //在当前activity 显示内容为“hello”的短时间弹窗
                Toast.makeText(MainActivity.this,"hello" ,Toast.LENGTH_SHORT).show();
            }
        });
        //到这里  你已经学会了基本的安卓开发
        // 按钮单机事件你会了   图片单机呢？？？
        image_1 =findViewById(R.id.image_1);
        image_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"我是第一个图片" ,Toast.LENGTH_SHORT).show();
                text_test.setText("我是新的内容");
            }
        });
        //  两个控件联动    按钮单机 更改 textview 的内容
        text_test =findViewById(R.id.text_test);




    }
}
