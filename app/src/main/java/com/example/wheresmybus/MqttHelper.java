package com.example.wheresmybus;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Created by wildan on 3/19/2017.
 */

public class MqttHelper {
    public MqttAndroidClient mqttAndroidClient;
   public  boolean conectado= false;
    final String serverUri = "tcp://broker.hivemq.com:1883";

    String clientId = MqttClient.generateClientId();
    final String username = "waterami";
    final String password = "waterami";

    public MqttHelper(Context context){
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("mqtt", s);
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Mqtt", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        connect();

    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    public void connect(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
       // mqttConnectOptions.setUserName(username);
        //mqttConnectOptions.setPassword(password.toCharArray());

        try {

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    conectado=true;
                    Log.w("Mqtt", ": conectado com sucesso " );
                    //publish("teste98","funciona crl");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                    conectado=false;
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }
     public boolean getConectado(){
        return conectado;
    }

    public void subscribeToTopic(String topico,int qos) {
        try {
            mqttAndroidClient.subscribe(topico, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("Mqtt","Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }


    public void publish(String topic, String info,int qos) {

        byte[] encodedInfo = new byte[0];
        try {
            encodedInfo = info.getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(encodedInfo);
            message.setQos(qos);
            mqttAndroidClient.publish(topic, message);
            Log.e("Mqtt", "publish done");
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e("Mqtt", Objects.requireNonNull(e.getMessage()));
        } catch (Exception e) {
            Log.e("Mqtt", "general exception " + e.getMessage());
        }

    }
}
