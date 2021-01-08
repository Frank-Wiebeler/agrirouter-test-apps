import agrirouter.response.Response;
import agrirouter.response.payload.account.Endpoints;
import com.dke.data.agrirouter.api.dto.onboard.OnboardingResponse;
import com.dke.data.agrirouter.convenience.mqtt.client.MqttClientService;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;

public interface IAppInstanceEvents {
  OnboardingResponse getOnboardingResponse();

  default void mqttEventCallback(Response.ResponseEnvelope envelope, Response.ResponsePayloadWrapper payloadWrapper){
    switch (envelope.getType()) {
      case ACK:
        System.out.println("Everything worked");
        break;
      case ACK_WITH_MESSAGES:
        System.out.println("Everything worked, but");
        System.out.println(payloadWrapper.toString());
        break;
      case ACK_WITH_FAILURE:
        System.out.println("D'Oh, something went wrong: ");
        System.out.println("          " + payloadWrapper.toString());
        break;
      case ENDPOINTS_LISTING:
        Endpoints.ListEndpointsResponse listEndpointsResponse = null;
        try {
          listEndpointsResponse = Endpoints.ListEndpointsResponse.parseFrom(payloadWrapper.getDetails().getValue().toByteArray());
          System.out.printf("Number of available Endpoints: "+ listEndpointsResponse.getEndpointsCount());
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
        break;
      case PUSH_NOTIFICATION:
        System.out.println("Received Push notification");
        break;
      default:
        System.out.println("Unexpected message received: "+envelope.getType().name());
    }
  }

  void onConnect(MqttClientService mqttClientService);
}
