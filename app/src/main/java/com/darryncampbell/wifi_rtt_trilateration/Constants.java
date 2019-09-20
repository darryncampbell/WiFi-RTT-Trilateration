package com.darryncampbell.wifi_rtt_trilateration;

public class Constants {

    public interface ACTION {
        String START_LOCATION_RANGING_SERVICE = "com.darryncampbell.wifi_rtt_trilateration.action.startrangingservice";
        String STOP_LOCATION_RANGING_SERVICE = "com.darryncampbell.wifi_rtt_trilateration.action.stoprangingservice";
        String START_LOCATION_RANGING = "com.darryncampbell.wifi_rtt_trilateration.action.startranging";
        String STOP_LOCATION_RANGING = "com.darryncampbell.wifi_rtt_trilateration.action.stopranging";
    }

    public interface NOTIFICATION_ID {
        int LOCATION_RANGING_SERVICE = 1;
        String LOCATION_UPDATE_CHANNEL_ID = "com.darryncampbell.wifi_rtt_trilateration.channel.general";
        String LOCATION_UPDATE_CHANNEL = "location channel";
    }

    public interface SERVICE_COMMS {
        String LOCATION_COORDS = "com.darryncampbell.wifi_rtt_trilateration.service.location_coords";
        String MESSAGE = "com.darryncampbell.wifi_rtt_trilateration.service.message";
        String FINISH = "com.darryncampbell.wifi_rtt_trilateration.service.finish";
    }
}
