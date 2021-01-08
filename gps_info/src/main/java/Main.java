import com.dke.data.agrirouter.api.dto.onboard.OnboardingResponse;
import com.dke.data.agrirouter.api.dto.onboard.RouterDevice;
import com.dke.data.agrirouter.api.dto.onboard.inner.ConnectionCriteria;
import com.dke.data.agrirouter.api.env.Environment;
import com.dke.data.agrirouter.api.env.QA;
import com.dke.data.agrirouter.api.service.messaging.ListEndpointsService;
import com.dke.data.agrirouter.api.service.messaging.MessageConfirmationService;
import com.dke.data.agrirouter.api.service.messaging.SetCapabilityService;
import com.dke.data.agrirouter.api.service.messaging.SetSubscriptionService;
import com.dke.data.agrirouter.convenience.mqtt.client.MqttClientService;
import com.dke.data.agrirouter.convenience.mqtt.client.MqttOptionService;
import com.dke.data.agrirouter.impl.messaging.mqtt.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Main {
  //private static final String applicationID = "63d1cb74-4ac6-410c-a6bf-b008db87aba1";
  //private static final String certificationVersionID = "cc9b00a1-8009-409b-ab06-88cb08109322";
  private static final String applicationID = "63d1cb74-4ac6-410c-a6bf-b008db87aba1";
  private static final String certificationVersionID = "cc9b00a1-8009-409b-ab06-88cb08109322";
  private static Environment environment;
  private static List<AppInstance> appInstances;
  private static RouterDevice routerDevice;
  private static IMqttClient mqttClient;
  private static MqttClientService mqttClientService;
  private static MqttOptionService mqttOptionService;
  private static MQTTCallbackImpl mqttCallbackAdapter;

  private static SetSubscriptionService setSubscriptionService;
  private static SetCapabilityService setCapabilityService;
  private static ListEndpointsService listEndpointsService;
  private static SendMessageServiceImpl sendMessageService;
  private static DeleteMessageServiceImpl deleteMessageService;
  private static MessageConfirmationService messageConfirmationService;


  public static OnboardingResponse loadOnboardingResponse(String path) throws IOException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    String fullPath = classLoader.getResource(path).getPath();
    fullPath = new File(fullPath).toString();
    byte[] input = Files.readAllBytes(Paths.get(fullPath));
    String inputString = new String(input);
    Gson gson = new Gson();
    GsonBuilder gsonBuilder = new GsonBuilder();
    OnboardingResponse onboardingResponse = gson.fromJson(inputString, OnboardingResponse.class);

    return onboardingResponse;
  }

  public static RouterDevice loadRouterDevice(String path) throws IOException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    String fullPath = classLoader.getResource(path).getPath();
    fullPath = new File(fullPath).toString();
    byte[] input = Files.readAllBytes(Paths.get(fullPath));
    String inputString = new String(input);
    Gson gson = new Gson();
    GsonBuilder gsonBuilder = new GsonBuilder();
    RouterDevice routerDevice = gson.fromJson(inputString, RouterDevice.class);

    return routerDevice;
  }







  public static void connectMQTT() {
    IMqttToken iMqttToken = null;
    do {
      try {
        iMqttToken = mqttClient.connectWithResult(mqttOptionService.createMqttConnectOptions(routerDevice));
      } catch (MqttException e) {
        e.printStackTrace();
      }
    } while (iMqttToken == null);

    mqttClient.setCallback(mqttCallbackAdapter);
    for (AppInstance entry : appInstances) {
      try {
        OnboardingResponse onboardingResponse = entry.getOnboardingResponse();
        ConnectionCriteria connectionCriteria = onboardingResponse.getConnectionCriteria();
        mqttClient.subscribe(connectionCriteria.getCommands());
      } catch (MqttException e) {
        e.printStackTrace();
      }
    }

  }

  public static void loadAppInstances() {
    appInstances = new ArrayList<>();

    try {
      appInstances.add(new AppInstance("Sender",loadOnboardingResponse("./telemetry/onboard-fmis1.json"),applicationID,certificationVersionID));
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      appInstances.add(new AppInstance("Receiver",loadOnboardingResponse("./telemetry/onboard-fmis2.json"),applicationID,certificationVersionID));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static AppInstance getAppInstance(String name) throws Exception{
    for(AppInstance entry : appInstances){
      if (entry.getName().equals(name)){
        return  entry;
      }
    }

    throw new NoSuchFieldException();
  }


  public static void debugAppInstances() {
    int count = 0;
    for (AppInstance appInstance : appInstances) {
      count++;
      System.out.println("FMIS" + count + ": " + appInstance.getOnboardingResponse().getDeviceAlternateId());
    }
  }

  public static void main(String[] arguments) throws Exception {

    System.setProperty("javax.net.ssl.keyStore", Main.class.getClassLoader().getResource("keystore.jks").getPath());
    System.setProperty("javax.net.ssl.keystorePassword", "changeit");
    environment = new QA() {
    };


    routerDevice = loadRouterDevice("./routerdevice.json");

    mqttClientService = new MqttClientService(environment);
    mqttClient = mqttClientService.create(routerDevice);
    mqttOptionService = new MqttOptionService(environment);
    mqttCallbackAdapter = new MQTTCallbackImpl(mqttClient);


    deleteMessageService = new DeleteMessageServiceImpl(mqttClient);
    messageConfirmationService = new MessageConfirmationServiceImpl(mqttClient);

    setCapabilityService = new SetCapabilityServiceImpl(mqttClient);
    setSubscriptionService = new SetSubscriptionServiceImpl(mqttClient);
    listEndpointsService = new ListEndpointsServiceImpl(mqttClient);
    sendMessageService = new SendMessageServiceImpl(mqttClient);


    loadAppInstances();

    for (AppInstance appInstance : appInstances) {
      mqttCallbackAdapter.addEndpoint(appInstance);

      appInstance.setServices(
              setCapabilityService,
              setSubscriptionService,
              listEndpointsService,
              sendMessageService,
              messageConfirmationService
      );
    }

    connectMQTT();


    AppInstance sender = getAppInstance("Sender");
    AppInstance receiver = getAppInstance("Receiver");

    sender.sendCapabilities();
    sender.awaitAnswer();
    sender.sendSubscriptions();
    sender.awaitAnswer();

    receiver.sendCapabilities();
    receiver.awaitAnswer();
    receiver.sendSubscriptions();
    receiver.awaitAnswer();

    //IMPORTANT! At this point we need a breakpoint to be able to set the routings in the agrirouter UI

    sender.sendOwnGPSPosition(50.2,10.3);
    sender.awaitAnswer();
    sender.sendOwnUnknownGPSPosition();
    sender.awaitAnswer();
    sender.startGPSPositionList();
    sender.addForeignMember(2,"AgrirouterMember",receiver.getOnboardingResponse().getSensorAlternateId(), "");
    sender.addForeignMember(7, "Smartphone", "", "abc:123:463");

    sender.addForeignGPSPosition(2, 20.3, 10.7);
    sender.addForeignGPSPosition(2,20.4,10.8);
    sender.addForeignGPSPosition(7, 19.3, 19.2);
    sender.addForeignGPSPosition( 7, 19.4, 19.1);
    sender.addForeignUnknownGPSPosition( 7);
    sender.sendGPSPositionList();
    sender.awaitAnswer();


  }

}
