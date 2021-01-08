import agrirouter.commons.MessageOuterClass;
import agrirouter.feed.push.notification.PushNotificationOuterClass;
import agrirouter.request.Request;
import agrirouter.request.payload.endpoint.Capabilities;
import agrirouter.response.Response;
import agrirouter.response.payload.account.Endpoints;
import agrirouter.technicalmessagetype.Gps;
import com.dke.data.agrirouter.api.dto.onboard.OnboardingResponse;
import com.dke.data.agrirouter.api.enums.TechnicalMessageType;
import com.dke.data.agrirouter.api.service.messaging.*;
import com.dke.data.agrirouter.api.service.messaging.encoding.EncodeMessageService;
import com.dke.data.agrirouter.api.service.parameters.*;
import com.dke.data.agrirouter.convenience.mqtt.client.MqttClientService;
import com.dke.data.agrirouter.impl.common.MessageIdService;
import com.dke.data.agrirouter.impl.messaging.encoding.EncodeMessageServiceImpl;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AppInstance implements  IAppInstanceEvents{
  private static int TIMEOUT_WAITER = 500;
  private boolean pendingAnswer;

  private enum Status{
    SetCapabilities,
    AwaitCapabilities,
    SetSubscriptions,
    AwaitSubscriptions,
    MainLoop
  }

  private OnboardingResponse onboardingResponse;
  private String applicationId;
  private String certificationVersionId;
  private Status status;
  private int lastCallTime;
  private SetCapabilityService setCapabilityService;
  private SetSubscriptionService setSubscriptionService;
  private ListEndpointsService listEndpointsService;
  private SendMessageService sendMessageService;
  private MessageConfirmationService messageConfirmationService;
  private String name;
  private String teamsetContextId;
  private Gps.GPSList.Builder groupGPSBuilder;


  AppInstance(String name, OnboardingResponse onboardingResponse,
              String applicationId,
              String certificationVersionId){
    this.name = name;
    this.onboardingResponse = onboardingResponse;
    this.applicationId = applicationId;
    this.certificationVersionId = certificationVersionId;
    this.status = Status.SetCapabilities;
    this.lastCallTime = 0;
    this.groupGPSBuilder = Gps.GPSList.newBuilder();

    this.teamsetContextId = UUID.randomUUID().toString();
    this.pendingAnswer = false;
  }

  public String generateTeamSetContextID(){
    this.teamsetContextId = UUID.randomUUID().toString();
    return this.teamsetContextId;
  }

  public String getTeamsetContextId(){
    return this.teamsetContextId;
  }


  public String getName(){
    return this.name;
  }


  public void onConnect(MqttClientService mqttClientService){

  }

  private SetCapabilitiesParameters buildCapabilitiesMessage() {
    List<SetCapabilitiesParameters.CapabilityParameters> parametersList = new ArrayList();

    SetCapabilitiesParameters.CapabilityParameters capabilityParameters;


    //GPS
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.GPS_INFO);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //TaskData
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.ISO_11783_TASKDATA_ZIP);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    /*
    //EFDI DeviceDescriptions
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.ISO_11783_DEVICE_DESCRIPTION);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //EFDI TimeLog
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.ISO_11783_TIME_LOG);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //BMP
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.IMG_BMP);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //PNG
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.IMG_PNG);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //JPEG
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.IMG_JPEG);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //AVI
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.VID_AVI);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //MP4
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.VID_MP4);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);


    //WMV
    capabilityParameters = new SetCapabilitiesParameters.CapabilityParameters();
    capabilityParameters.setTechnicalMessageType(TechnicalMessageType.VID_WMV);
    capabilityParameters.setDirection(Capabilities.CapabilitySpecification.Direction.SEND_RECEIVE);
    parametersList.add(capabilityParameters);
    */

    SetCapabilitiesParameters parameters = new SetCapabilitiesParameters();
    parameters.setOnboardingResponse(onboardingResponse);
    parameters.setApplicationId(applicationId);
    parameters.setCertificationVersionId(certificationVersionId);
    parameters.setCapabilitiesParameters(parametersList);
    parameters.setEnablePushNotifications(Capabilities.CapabilitySpecification.PushNotification.ENABLED);

    return parameters;
  }

  private SetSubscriptionParameters buildSubscriptionMessage() {
    SetSubscriptionParameters setSubscriptionParameters = new SetSubscriptionParameters();
    setSubscriptionParameters.setOnboardingResponse(onboardingResponse);
    List<SetSubscriptionParameters.Subscription> subscriptionList = new ArrayList<>();
    SetSubscriptionParameters.Subscription subscription;

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.GPS_INFO);
    subscriptionList.add(subscription);


    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.ISO_11783_TASKDATA_ZIP);
    subscriptionList.add(subscription);

    /*
    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.ISO_11783_TIME_LOG);
    subscriptionList.add(subscription);

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.ISO_11783_DEVICE_DESCRIPTION);
    subscriptionList.add(subscription);

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.IMG_BMP);
    subscriptionList.add(subscription);

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.IMG_PNG);
    subscriptionList.add(subscription);

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.IMG_JPEG);
    subscriptionList.add(subscription);

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.VID_WMV);
    subscriptionList.add(subscription);

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.VID_AVI);
    subscriptionList.add(subscription);

    subscription = new SetSubscriptionParameters.Subscription();
    subscription.setTechnicalMessageType(TechnicalMessageType.VID_MP4);
    subscriptionList.add(subscription);

     */
    setSubscriptionParameters.setSubscriptions(subscriptionList);

    return setSubscriptionParameters;
  }

  public void setServices(SetCapabilityService setCapabilityService,
                          SetSubscriptionService setSubscriptionService,
                          ListEndpointsService listEndpointsService,
                          SendMessageService sendMessageService,
                          MessageConfirmationService messageConfirmationService){

        this.setCapabilityService = setCapabilityService;
        this.setSubscriptionService = setSubscriptionService;
        this.listEndpointsService = listEndpointsService;
        this.sendMessageService = sendMessageService;
        this.messageConfirmationService = messageConfirmationService;

  }

  public void sendCapabilities(){
      this.setCapabilityService.send(this.buildCapabilitiesMessage());
  }

  public void sendSubscriptions(){
      this.setSubscriptionService.send(this.buildSubscriptionMessage());
  }

  private SendMessageParameters buildGPSMessage(Gps.GPSList.Builder gpsListBuilder){
    SendMessageParameters parameters = new SendMessageParameters();
    parameters.setOnboardingResponse(onboardingResponse);
    EncodeMessageService encodeMessageService = new EncodeMessageServiceImpl();
    MessageHeaderParameters encodeMessageHeaderParameters = new MessageHeaderParameters();
    encodeMessageHeaderParameters.setApplicationMessageSeqNo(1);
    String applicationMessageId = MessageIdService.generateMessageId();
    encodeMessageHeaderParameters.setApplicationMessageId(applicationMessageId);
    encodeMessageHeaderParameters.setTechnicalMessageType(TechnicalMessageType.GPS_INFO);
    encodeMessageHeaderParameters.setMode(Request.RequestEnvelope.Mode.PUBLISH);
    encodeMessageHeaderParameters.setTeamSetContextId(this.getTeamsetContextId());
    encodeMessageHeaderParameters.setMetadata(MessageOuterClass.Metadata.newBuilder().build());

    ByteString content = gpsListBuilder.build().toByteString();

    PayloadParameters encodePayloadParameters = new PayloadParameters();
    encodePayloadParameters.setTypeUrl(Gps.GPSList.getDescriptor().getFullName());
    encodePayloadParameters.setValue(content);
    String encodedMessage = encodeMessageService.encode(encodeMessageHeaderParameters, encodePayloadParameters);
    parameters.setEncodedMessages(Collections.singletonList(encodedMessage));

    return parameters;
  }


  public void sendOwnGPSPosition( double longitude, double latitude){
    Gps.GPSList.Builder gpsListBuilder = Gps.GPSList.newBuilder();
    Gps.GPSList.GPSEntry gpsEntry = Gps.GPSList.GPSEntry.newBuilder()
            .setPositionEast(latitude)
            .setPositionNorth(longitude)
            .setPositionStatus(Gps.GPSList.GPSEntry.PositionStatus.D_DGNSS)
            .build();
    gpsListBuilder.addGpsEntries(gpsEntry);

    this.sendMessageService.send(buildGPSMessage( gpsListBuilder));
  }

  public void sendOwnUnknownGPSPosition(){
    Gps.GPSList.Builder gpsListBuilder = Gps.GPSList.newBuilder();
    Gps.GPSList.GPSEntry gpsEntry = Gps.GPSList.GPSEntry.newBuilder()
            .setPositionEast(0.0)
            .setPositionNorth(0.0)
            .setPositionStatus(Gps.GPSList.GPSEntry.PositionStatus.D_NOT_AVAILABLE)
            .build();
    gpsListBuilder.addGpsEntries(gpsEntry);

    this.sendMessageService.send(buildGPSMessage( gpsListBuilder));

  }

  public void startGPSPositionList(){
    this.groupGPSBuilder.clear();
  }

  public void addForeignGPSPosition(int member,double longitude, double latitude){
    Gps.GPSList.GPSEntry entry = Gps.GPSList.GPSEntry.newBuilder()
            .setPositionEast(latitude)
            .setPositionNorth(longitude)
            .setPositionStatus(Gps.GPSList.GPSEntry.PositionStatus.D_DGNSS)
            .setSourceDeviceIndex(member).build();

    this.groupGPSBuilder.addGpsEntries(entry);
  }


  public void addForeignUnknownGPSPosition(int member){
    Gps.GPSList.GPSEntry entry = Gps.GPSList.GPSEntry.newBuilder()
            .setPositionEast(0.0)
            .setPositionNorth(0.0)
            .setPositionStatus(Gps.GPSList.GPSEntry.PositionStatus.D_NOT_AVAILABLE)
            .setSourceDeviceIndex(member).build();

    this.groupGPSBuilder.addGpsEntries(entry);
  }

  public void addForeignMember(int member, String name, String agrirouterID, String internalId){
    Gps.GPSList.SourceDevice sourceDevice = Gps.GPSList.SourceDevice.newBuilder()
            .setIndex(member)
            .setAgrirouterId(agrirouterID)
            .setDisplayName(name)
            .setInternalId(internalId).build();
    this.groupGPSBuilder.addSourceDevices(sourceDevice);
  }

  public void sendGPSPositionList(){
    this.sendMessageService.send(buildGPSMessage(this.groupGPSBuilder));
  }


  private List<String> debugGPSPosition(Response.ResponsePayloadWrapper payloadWrapper) {

    List<String> messageIds = new ArrayList<>();
    PushNotificationOuterClass.PushNotification pushNotification = null;
    try {
      pushNotification = PushNotificationOuterClass.PushNotification.parseFrom(payloadWrapper.getDetails().getValue());
      for( PushNotificationOuterClass.PushNotification.FeedMessage messageEntry: pushNotification.getMessagesList()) {
        System.out.println("=======================================================================");
        System.out.println("Message sent by: " + messageEntry.getHeader().getSenderId());
        if (messageEntry.getHeader().getTechnicalMessageType().equals(TechnicalMessageType.GPS_INFO.getKey())) {
          try {
            if(!messageEntry.getContent().getTypeUrl().equals("")) {
              System.out.println("Messagetype: GPS\n\n");
              byte[] data = messageEntry.getContent().getValue().toByteArray();
              Gps.GPSList gpsList = Gps.GPSList.parseFrom(data);
              if(gpsList.getSourceDevicesCount() == 0){
                System.out.println("Own GPS Positions received");
                for (Gps.GPSList.GPSEntry entry : gpsList.getGpsEntriesList()) {
                  if( entry.getPositionStatus() != Gps.GPSList.GPSEntry.PositionStatus.D_NOT_AVAILABLE){
                    System.out.println("North: " + entry.getPositionNorth() + " East: " + entry.getPositionEast());
                  } else {
                    System.out.println("Unknown");
                  }
                }
              } else {
                System.out.println("Foreign GPS Positions received");
                for (Gps.GPSList.SourceDevice sourceDevice: gpsList.getSourceDevicesList()){
                  if(("").equals(sourceDevice.getAgrirouterId())){
                    System.out.println("Device " + sourceDevice.getIndex() + ": \"" + sourceDevice.getDisplayName() +"\", Internal ID: " + sourceDevice.getInternalId());
                  } else {
                    System.out.println("Device: " + sourceDevice.getIndex() + ": \"" + sourceDevice.getDisplayName() +"\", AgrirouterID: " + sourceDevice.getAgrirouterId());
                  }
                  System.out.println("[");
                  for( Gps.GPSList.GPSEntry entry: gpsList.getGpsEntriesList()){
                    if( entry.getSourceDeviceIndex() == sourceDevice.getIndex()){
                      if( entry.getPositionStatus() != Gps.GPSList.GPSEntry.PositionStatus.D_NOT_AVAILABLE){
                        System.out.println("North: " + entry.getPositionNorth() + " East: " + entry.getPositionEast());
                      } else {
                        System.out.println("Unknown");
                      }
                    }
                  }
                  System.out.println("]");
                }
              }
            }
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }

          messageIds.add(messageEntry.getHeader().getMessageId());
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return messageIds;
  }

  public void confirmMessages(List<String> messages){
    MessageConfirmationParameters parameters  = new MessageConfirmationParameters();
    parameters.setMessageIds(messages);
    parameters.setOnboardingResponse(this.getOnboardingResponse());

    messageConfirmationService.send(parameters);
  }



  @Override
  public OnboardingResponse getOnboardingResponse() {
    return this.onboardingResponse;
  }

  @Override
  public void mqttEventCallback(Response.ResponseEnvelope envelope, Response.ResponsePayloadWrapper payloadWrapper) {
    switch (envelope.getType()) {
      case ACK:
        System.out.println("Everything worked: ACK");
        this.pendingAnswer = false;
        break;
      case ACK_WITH_MESSAGES:
        System.out.println("Everything worked, but: ACK_WITH_MESSAGES");
        System.out.println(payloadWrapper.toString());
        this.pendingAnswer = false;
        break;
      case ACK_WITH_FAILURE:
        System.out.println("D'Oh, something went wrong: ACK_WITH_FAILURE");
        System.out.println("          " + payloadWrapper.toString());
        this.pendingAnswer = false;
        break;
      case ENDPOINTS_LISTING:
        Endpoints.ListEndpointsResponse listEndpointsResponse = null;
        try {
          listEndpointsResponse = Endpoints.ListEndpointsResponse.parseFrom(payloadWrapper.getDetails().getValue().toByteArray());
          System.out.printf("Number of available Endpoints: "+ listEndpointsResponse.getEndpointsCount());
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
        this.pendingAnswer = false;
        break;
      case PUSH_NOTIFICATION:
        System.out.println("Received Push notification");
        this.debugGPSPosition(payloadWrapper);
        this.pendingAnswer = false;
        break;
      default:
        System.out.println("Unexpected message received: "+envelope.getType().name());
        this.pendingAnswer = false;
    }
  }

  public void awaitAnswer() throws InterruptedException {
    this.pendingAnswer = true;
    do{
      Thread.sleep(1000);
      System.out.println("Life message from " +this.getName());
    } while( this.pendingAnswer == true);
  }

}
