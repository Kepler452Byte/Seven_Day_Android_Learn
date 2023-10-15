package com.demo.opencv_learn;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
public class General_controlActivity extends AppCompatActivity implements View.OnClickListener {
    public  static final  String BASE_URL = "http://106.13.150.28:1880";
    private String host = "tcp://106.13.150.28:1883";  //ckgjppc.mqtt.iot.bj.baidubce.com   47.105.157.158:1883"
    private String userName = "android";
    private String passWord = "android";
    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private MqttConnectOptions options;
    private TextView debug_window;
    private Handler handler;
    private String phone_imei;
    private String Apk_up_address;
    private long mExitTime;
    private int app_version;

    @SuppressLint("HandlerLeak")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_control);
        initUI();
        getRequest();
        Mqtt_init();
        startReconnect();
        Notification_init();
        handler = new Handler() {
            @SuppressLint("SetTextI18n")
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 1: //开机校验更新回传
                        //Toast.makeText(General_controlActivity.this,msg.obj.toString() ,Toast.LENGTH_SHORT).show();
                        String  msg_json = msg.obj.toString().substring(msg.obj.toString().indexOf("[")+1,msg.obj.toString().indexOf("]"));
                        try {
                            JSONObject jsonObject = new JSONObject(msg_json);
                            int apk_version = jsonObject.getInt("app_version");
                            Apk_up_address = jsonObject.getString("apk_address");
                            String up_msg = jsonObject.getString("update_message");
                            if(apk_version>app_version) //远程版本是否大于本地版本
                            {
                                showdialog(0,up_msg);
                            }
                            debug_window.setText(app_version+":"+up_msg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        handler.removeMessages(1); //销毁消息
                    break;
                    case 2:  // 反馈回传
                        Toast.makeText(General_controlActivity.this,msg.obj.toString() ,Toast.LENGTH_SHORT).show();
                        break;
                    case 3:  //MQTT 收到消息回传   UTF8Buffer msg=new UTF8Buffer(object.toString());
                        Send_Notification("远程通知",msg.obj.toString());
                        //Toast.makeText(General_controlActivity.this,msg.obj.toString() ,Toast.LENGTH_SHORT).show();
                        break;
                    case 30:  //连接失败
                        Toast.makeText(General_controlActivity.this, "连接失败，系统正在重连", Toast.LENGTH_SHORT).show();
                        System.out.println("连接失败，系统正在重连");
                        break;
                    case 31:   //连接成功
                        Toast.makeText(General_controlActivity.this,"连接成功" ,Toast.LENGTH_SHORT).show();
                        try {
                            //  订阅消息
                            client.subscribe("/App_Notic", 1);//订阅主题
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }

        };
    }

    //发送通知栏通知
    private void Send_Notification(String title, String msg) {
        Intent intent = new Intent(General_controlActivity.this, MqttActivity.class);
        PendingIntent pi =PendingIntent.getActivity(General_controlActivity.this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, "chat")
                .setContentTitle(title)
                .setContentText(msg)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.camera)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.camera))
                .setAutoCancel(true)  //设置自动消失
                .setContentIntent(pi) //设置点击跳转页面
                .build();
        manager.notify(1, notification);
    }

    private void Notification_init() {
        //初始化通知隧道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "chat";
            String channelName = "聊天消息";
            int importance = NotificationManager.IMPORTANCE_HIGH;  //
            createNotificationChannel(channelId, channelName, importance);

            channelId = "subscribe";
            channelName = "订阅消息";
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            createNotificationChannel(channelId, channelName, importance);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, int importance) {
        //  渠道ID、渠道名称以及重要等级
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    private void getRequest() {
        //有一个客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.MILLISECONDS)
                .build();
        //创建请求内容
        Request.Builder builder = new Request.Builder();
        Request request = builder
                .get()
                .url(BASE_URL + "/APP?imei="+phone_imei)
                .build();
        Toast.makeText(General_controlActivity.this,phone_imei,Toast.LENGTH_SHORT).show();
        if (phone_imei == "")
        {
            Toast.makeText(General_controlActivity.this,"无法获取imei，请授予相关权限",Toast.LENGTH_SHORT).show();
        }
        else{
        //创建请求任务
        Call task = okHttpClient.newCall(request);
        task.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.out.println("ConnectionLost----------" + e.toString());
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                int code = response.code();
                if (code == HttpURLConnection.HTTP_OK) {
                    System.out.println("Connection" + code);
                }
                ResponseBody body = response.body();
                if (body != null) {
                    //这里的body.string 只能调用一次，第二次会被进程销毁清空。如需要多次使用，可以用变量转存
                    //System.out.println("ConnectionSuccess----------"+body.string());
                    Message msg = new Message();
                    msg.what = 1;   //收到消息标志位
                    msg.obj = body.string();
                    handler.sendMessage(msg);    // hander 回传
                }
            }
        });
    }
    }
private  void app_feedback(String msg)
{
    //新建一个客户端对象
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10000, TimeUnit.MILLISECONDS)
            .build();
    //创建请求内容
    Request.Builder builder = new Request.Builder();
    Request request = builder
            .get()
            .url(BASE_URL + "/APP_feedback?msg="+msg)
            .build();
    Toast.makeText(General_controlActivity.this,phone_imei,Toast.LENGTH_SHORT).show();
        //创建请求任务
        Call task = okHttpClient.newCall(request);
        task.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.out.println("ConnectionLost----------" + e.toString());
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                int code = response.code();
                if (code == HttpURLConnection.HTTP_OK) {
                    System.out.println("Connection" + code);
                }
                ResponseBody body = response.body();
                if (body != null) {
                    //这里的body.string 只能调用一次，第二次会被进程销毁清空。如需要多次使用，可以用变量转存
                    //System.out.println("ConnectionSuccess----------"+body.string());
                    Message msg = new Message();
                    msg.what = 2;   //收到消息标志位
                    msg.obj = body.string();
                    handler.sendMessage(msg);    // hander 回传
                }
            }
        });
}
    private void initUI()
    {
        phone_imei = getIMEI(General_controlActivity.this,0);
        app_version = getLocalVersion(General_controlActivity.this);
        Button mqtt_btn = findViewById(R.id.mqtt_btn);
        mqtt_btn.setOnClickListener(this);
        Button opencv_btn = findViewById(R.id.opencv_btn);
        opencv_btn.setOnClickListener(this);
        debug_window = findViewById(R.id.debug_window);
        Button feedback = findViewById(R.id.feedback);
        feedback.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        //  Activity 直接实现监听接口
        switch (v.getId()) {
            case R.id.mqtt_btn:
                Intent MqttActivity = new Intent(General_controlActivity.this, MqttActivity.class);
                startActivity(MqttActivity);
                finish(); //销毁界面
                break;
            case R.id.opencv_btn:
                Intent MainActivity = new Intent(General_controlActivity.this, MainActivity.class);
                startActivity(MainActivity);
                finish(); //销毁界面
                break;
            case R.id.feedback:
                final EditText inputServer = new EditText(General_controlActivity.this);
                AlertDialog.Builder builder = new AlertDialog.Builder(General_controlActivity.this);
                builder.setTitle("反馈").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("反馈作者", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //debug_window.setText( inputServer.getText().toString());
                        app_feedback(inputServer.getText().toString());
                    }
                });
                builder.show();
                break;
            default:
                break;
        }
    }
    private void Mqtt_init()
    {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, phone_imei,
                    new MemoryPersistence());
            //MQTT的连接设置
            options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(false);
            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");
                    //startReconnect();
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------"
                            + token.isComplete());
                }
                @Override
                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {
                    //subscribe后得到的消息会执行到这里面
                    System.out.println("messageArrived----------");
                    Message msg = new Message();
                    msg.what = 3;   //收到消息标志位
                    msg.obj = topicName + "---" + message.toString();
                    handler.sendMessage(msg);    // hander 回传
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!(client.isConnected()) )  //如果还未连接
                    {
                        client.connect(options);
                        Message msg = new Message();
                        msg.what = 31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 30;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }
    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }
    private DialogInterface.OnClickListener click1=new DialogInterface.OnClickListener()
    {
        //使用该标记是为了增强程序在编译时候的检查，如果该方法并不是一个覆盖父类的方法，在编译时编译器就会报告错误。
        @Override
        public void onClick(DialogInterface arg0,int arg1)
        {
            Uri uri = Uri.parse(Apk_up_address);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    };
    private DialogInterface.OnClickListener click2=new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface arg0,int arg1)
        {
            //当按钮click2被按下时执行结束进程
            android.os.Process.killProcess(android.os.Process.myPid());
           // arg0.cancel();  取消
        }
    };
    public void showdialog( int flag,String msg)
    {
        //定义一个新的对话框对象
        AlertDialog.Builder alertdialogbuilder=new AlertDialog.Builder(this);
        //设置可点击对话框外 对话框是否关闭
        alertdialogbuilder.setCancelable(false);
        //设置对话框提示内容
        switch (flag)
        {
            case 0:
                alertdialogbuilder.setMessage(msg);
                alertdialogbuilder.setPositiveButton("下载",click1);
                alertdialogbuilder.setNegativeButton("退出",click2);
                break;
            case 1:
                alertdialogbuilder.setMessage("作者:阿正wenzheng.club\nB站:阿正啷个哩个啷\nQQ群:476840321");
                alertdialogbuilder.setNegativeButton("取消",click2);
                break;
            default:
                break;
        }
        //创建并显示对话框
        AlertDialog alertdialog1=alertdialogbuilder.create();
        alertdialog1.show();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((client != null) && (keyCode == KeyEvent.KEYCODE_BACK)) {
            if((System.currentTimeMillis()-mExitTime)>2000)
            {
                Toast.makeText(this, "再按一次退出APP", Toast.LENGTH_SHORT).show();
                //System.currentTimeMillis()系统当前时间
                mExitTime = System.currentTimeMillis();
            }else {
                try {
                    client.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.finish();
                System.exit(0);   // 退出整个应用

            }

        }
        return super.onKeyDown(keyCode, event);
    }
    public static String getIMEI(Context context, int slotId) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method method = manager.getClass().getMethod("getImei", int.class);
            String im = (String) method.invoke(manager, slotId);
            return im;
        } catch (Exception e) {
            return "";
        }
    }
    //安卓权限动态授权
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "权限拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    /* 获取本地软件版本号​
     */
    public static int getLocalVersion(Context ctx) {
        int localVersion = 0;
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionCode;
            Log.d("TAG", "当前版本号：" + localVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

}

/*
1、public：public表明该数据成员、成员函数是对所有用户开放的，所有用户都可以直接进行调用
2、private：private表示私有，私有的意思就是除了class自己之外，任何人都不可以直接使用，私有财产神圣不可侵犯嘛，即便是子女，朋友，都不可以使用。
3、protected：protected对于子女、朋友来说，就是public的，可以自由使用，没有任何限制，而对于其他的外部class，protected就变成private。

 */