package com.alwaysallthetime.adnlibutils;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.Configuration;
import com.alwaysallthetime.adnlib.response.ConfigurationResponseHandler;

import java.util.Calendar;
import java.util.Date;

public class ConfigurationUtility {
    public static void updateConfiguration(AppDotNetClient client) {
        Date configurationSaveDate = ADNSharedPreferences.getConfigurationSaveDate();
        boolean fetchNewConfig = configurationSaveDate == null;
        if(!fetchNewConfig) {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(configurationSaveDate);
            cal2.setTime(new Date());
            boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
            fetchNewConfig = !sameDay;
        }
        if(fetchNewConfig) {
            client.retrieveConfiguration(new ConfigurationResponseHandler() {
                @Override
                public void onSuccess(Configuration responseData) {
                    ADNSharedPreferences.saveConfiguration(responseData);
                }
            });
        }
    }
}
