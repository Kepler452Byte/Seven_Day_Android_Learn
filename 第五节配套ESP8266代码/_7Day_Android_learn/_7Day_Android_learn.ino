
/*
 * 阿正整理设计
 * 此代码为 7天Java0基础速成安卓开发配套硬件代码
 * esp环境需要自己搭建（不同版本函数参数略有不同，自己优化）
 * 为达到硬件也是0基础（设计如下内容一个开发板一根数据线就可以完成）
 * 采用NodeMCU（或者任意ESP8266开发板）
 * 温度上报使用ADC模拟随机数
 * 数据接收不采用json解析而是直接字符串处理
 * 
 * 博客：http://wenzheng.club/
 * B站：https://space.bilibili.com/265908761
 */

#include <ESP8266WiFi.h>
#include <PubSubClient.h>
WiFiClient espClient;
PubSubClient client(espClient);
const char* wifissid = "CMCC-wenzheng"; //改成自己家wifi
const char* password = "wenzhengspace666"; //改成自己家wifi
const char* mqtt_server = "106.13.150.28";
const char* mqtt_id = "565402462_ESP";   //改成自己的QQ号+_ESP
const char* Mqtt_sub_topic = "565402462_ESP";   //改成自己的QQ号+_ESP
const char* Mqtt_pub_topic = "565402462";  //  上报消息给  手机APP的TOPIC  //改成自己的QQ号
long lastMsg = 0; //定时用的
void setup() {
  pinMode(2, OUTPUT);     
  Serial.begin(115200);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}
void setup_wifi() {
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(wifissid);
  WiFi.begin(wifissid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}
void callback(char* topic, byte* payload, unsigned int length) {
  String msg="";
  String LED_set = "";
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    msg+= (char)payload[i];
  }
  Serial.println(msg);
  if(msg.indexOf("led"))  //判断是否是要设置LED灯
  {
    //取出LED_set数据 并执行
    LED_set = msg.substring(msg.indexOf("led\":")+5,msg.indexOf("}")); 
    digitalWrite(2,!LED_set.toInt()); 
  }
}
void reconnect() {
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    if (client.connect(mqtt_id)) {
      Serial.println("connected");
      //连接成功以后就开始订阅
      client.subscribe(Mqtt_sub_topic,1);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}
void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
  long now = millis();
  if (now - lastMsg > 2000) {
    lastMsg = now;
    String json = "{\"temperature\":"+String(analogRead(A0))+"}";
    client.publish(Mqtt_pub_topic,json.c_str());
  }
}
