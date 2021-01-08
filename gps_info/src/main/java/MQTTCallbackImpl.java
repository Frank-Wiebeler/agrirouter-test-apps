import agrirouter.feed.push.notification.PushNotificationOuterClass;
import agrirouter.response.Response;
import agrirouter.response.payload.account.Endpoints;
import agrirouter.technicalmessagetype.Gps;
import agrirouter.technicalmessagetype.Gps.GPSList;
import com.dke.data.agrirouter.api.dto.encoding.DecodeMessageResponse;
import com.dke.data.agrirouter.api.dto.messaging.FetchMessageResponse;
import com.dke.data.agrirouter.api.dto.onboard.OnboardingResponse;
import com.dke.data.agrirouter.api.enums.TechnicalMessageType;
import com.dke.data.agrirouter.api.service.messaging.MessageConfirmationService;
import com.dke.data.agrirouter.api.service.messaging.encoding.DecodeMessageService;
import com.dke.data.agrirouter.api.service.parameters.MessageConfirmationParameters;
import com.dke.data.agrirouter.impl.messaging.encoding.DecodeMessageServiceImpl;
import com.dke.data.agrirouter.impl.messaging.mqtt.MessageConfirmationServiceImpl;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MQTTCallbackImpl implements MqttCallback {
  private DecodeMessageService decodeMessageService;

  private IMqttClient mqttClient;
  private List<AppInstance> endpointList;

  public MQTTCallbackImpl(IMqttClient mqttClient){
    super();
    decodeMessageService = new DecodeMessageServiceImpl();
    this.mqttClient = mqttClient;
    this.endpointList = new ArrayList<>();
  }

  public void addEndpoint(AppInstance appInstance){
    this.endpointList.add(appInstance);
  }

  public  AppInstance findEndpoint(String topic){
    for(AppInstance entry: this.endpointList){
      if(topic.equals(entry.getOnboardingResponse().getConnectionCriteria().getCommands())){
        return entry;
      }
    }
    return null;
  }

  public List<String> saveReceivedFile(OnboardingResponse onboardingResponse, byte[] data ) throws IOException {
    List<String> messageIds = new ArrayList<>();
    PushNotificationOuterClass.PushNotification pushNotification = PushNotificationOuterClass.PushNotification.parseFrom(data);
    int count = 0;
    for( PushNotificationOuterClass.PushNotification.FeedMessage messageEntry: pushNotification.getMessagesList()){
      System.out.println("Message sent by: "+ messageEntry.getHeader().getSenderId());
      System.out.println("        Type: " + messageEntry.getHeader().getTechnicalMessageType());
      messageIds.add(messageEntry.getHeader().getMessageId());
      byte[] rawBase64Data = messageEntry.getContent().getValue().toByteArray();
      String rawBase64String = new String(rawBase64Data,"utf-8");
      byte[] rawData = Base64.decodeBase64(rawBase64String);
      Files.write(Paths.get("C:\\arapp\\tutorial\\in\\" + onboardingResponse.deviceAlternateId +"_" + count + ".bmp"),rawData);
      count++;
    }

    return messageIds;
  }




  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    try {
      AppInstance appInstance = findEndpoint(topic);
      if(appInstance == null) {
        System.out.println("ERROR: No corresponding Endpoint for topic" + topic);
      }

      Gson gson = new Gson();


      String incomingMessage = new String(message.getPayload());
      FetchMessageResponse fetchMessageResponse = gson.fromJson(incomingMessage, FetchMessageResponse.class);
      DecodeMessageResponse decodeMessageResponse = decodeMessageService.decode(fetchMessageResponse.getCommand().getMessage());

      Response.ResponseEnvelope envelope = decodeMessageResponse.getResponseEnvelope();
      Response.ResponsePayloadWrapper payloadWrapper = decodeMessageResponse.getResponsePayloadWrapper();

      appInstance.mqttEventCallback(envelope,payloadWrapper);

    } catch ( Exception e) {
      System.out.println("Error found: "+e.getMessage());
    }

  }



  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    //System.out.println("deliveryComplete");
  }

  public synchronized void reconnectMqtt(){
    Main.connectMQTT();
  }

  @Override
  public void connectionLost(Throwable cause) {
    System.out.println("connection Lost :(");
    reconnectMqtt();
  }


}
