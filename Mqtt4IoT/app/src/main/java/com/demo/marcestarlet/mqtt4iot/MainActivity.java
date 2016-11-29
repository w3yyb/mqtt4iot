package com.demo.marcestarlet.mqtt4iot;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;

//import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
//import android.view.Menu;
import android.graphics.Color;
import android.graphics.BitmapFactory;

import android.view.View.OnClickListener;
import android.widget.Button;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity implements MqttCallback{
    Button button;


    // connection default values
    private static final String BROKER_URI = "tcp://www.p2hp.com:1883";
    private static final String TOPIC = "mqtt4iotdemo";
    private static final int QOS = 2;

    // user name for the chat
    private static final String USER_NAME = Build.DEVICE;

    // global types
    private MqttAndroidClient client;
    private EditText textMessage;
    private TextView textConversation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //button = (Button) findViewById(R.id.notify);

        // get text elements to re-use them
        textMessage = (EditText) findViewById(R.id.message);
        textConversation = (TextView) findViewById(R.id.conversation);

        // when the activity is created call to connect to the broker
        connect();
    }

    private void connect(){
        // create a new MqttAndroidClient using the current context
        client = new MqttAndroidClient(this, BROKER_URI, USER_NAME);
        client.setCallback(this); // set this as callback to listen for messages

        try{
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false); // clean session in order to don't get duplicate messages each time we connect
           // Toast.makeText(this,"cleansession"+ options.isCleanSession() , Toast.LENGTH_SHORT).show();

            client.connect(options, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Toast.makeText(MainActivity.this, "Ready for chat", Toast.LENGTH_SHORT).show();
                    // once connected call to subscribe to receive messages
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Toast.makeText(MainActivity.this, "Unavailable chat, cause: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                }

            });
        } catch (MqttException e){
            Toast.makeText(this, "ERROR, client not connected to broker in " + BROKER_URI, Toast.LENGTH_LONG).show();
        }
    }

    public void publish(View view) {
        // we are in the right view?
        if (view.getId() == R.id.publish) {
            // we only publish if connected
            if (null != client && client.isConnected()) {

                String message = textMessage.getText().toString();
                // we only publish if there is message to publish
                if (!message.isEmpty()) {

                    message = "<b>" + USER_NAME + "</b>: " + message + "<br/>";
                    textMessage.setText("");

                    MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                    mqttMessage.setQos(QOS);
                    try {
                        client.publish(TOPIC, mqttMessage, null, new IMqttActionListener() {

                            @Override
                            public void onSuccess(IMqttToken iMqttToken) {
                                //Toast.makeText(MainActivity.this, "message sent", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                                Toast.makeText(MainActivity.this, "Failed on publish, cause: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            }

                        });
                    } catch (MqttException e) {
                        Toast.makeText(this, " ERROR, an error occurs when publishing", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(this, "WARNING, client not connected", Toast.LENGTH_LONG).show();
            }

        }
    }

    private void subscribe() {
        try {
            client.subscribe(TOPIC, QOS, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    //Toast.makeText(MainActivity.this, "Subscribed to:" + TOPIC, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Toast.makeText(MainActivity.this, "Failed on subscribe, cause: " + throwable, Toast.LENGTH_LONG).show();
                }

            });

        } catch (MqttException e) {
            Toast.makeText(this, "ERROR, an error occurs when subscribing", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Toast.makeText(this, "Connection lost!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //Toast.makeText(this, "message arrived", Toast.LENGTH_SHORT).show();
        // format message in html
        String text = null != textConversation.getTag() ? (String) textConversation.getTag() : "";
        text += message.toString();
        textConversation.setTag(text);
        textConversation.setText(Html.fromHtml(text));

        //获得通知管理器
        Notification notifation= new Notification.Builder(MainActivity.this)
                .setContentTitle("新消息")
                .setContentText(Html.fromHtml(message.toString()))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .build();
        NotificationManager manger= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //自定义声音   声音文件放在ram目录下，没有此目录自己创建一个
        //notifation.sound=Uri.parse("android.resource://" + getPackageName() + "/" +R.raw.mm);

//使用系统默认声音用下面这条
        //notifation.defaults=Notification.DEFAULT_SOUND;

        //设置手机震动
 //第一个，0表示手机静止的时长，第二个，1000表示手机震动的时长
 //第三个，1000表示手机震动的时长，第四个，1000表示手机震动的时长
   //此处表示手机先震动1秒，然后静止1秒，然后再震动1秒
   //long[] vibrates = {0, 1000, 1000, 1000};
    //    notifation.vibrate = vibrates;


        //设置LED指示灯的闪烁 --not work!!!
  //ledARGB设置颜色
   //ledOnMS指定LED灯亮起的时间
   //ledOffMS指定LED灯暗去的时间
   //flags用于指定通知的行为
      //  notifation.ledARGB = Color.GREEN;
      //  notifation.ledOnMS = 1000;
      //  notifation.ledOffMS = 1000;
      //  notifation.flags = Notification.FLAG_SHOW_LIGHTS;


        //如果不想进行那么多繁杂的这只，可以直接使用通知的默认效果
        //默认设置了声音，震动和灯光
        notifation.defaults = Notification.DEFAULT_ALL;
        manger.notify(0, notifation);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //Toast.makeText(this, "Delivery complete!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    }
