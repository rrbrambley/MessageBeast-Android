package com.alwaysallthetime.messagebeast;

import com.alwaysallthetime.adnlib.AppDotNetClient;
import com.alwaysallthetime.adnlib.data.Configuration;
import com.alwaysallthetime.adnlib.response.ConfigurationResponseHandler;

import java.util.Calendar;
import java.util.Date;

public class ConfigurationUtility {

    /**
     * Update the Configuration if due.
     *
     * http://developers.app.net/docs/resources/config/#how-to-use-the-configuration-object
     *
     * @param client the AppDotNetClient to use for the request
     * @return true if the configuration is being fetched, false otherwise.
     */
    public static boolean updateConfiguration(AppDotNetClient client) {
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
        return fetchNewConfig;
    }
}
