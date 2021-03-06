package com.shouldit.proxy.lib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import com.shouldit.proxy.lib.enums.CheckStatusValues;
import com.shouldit.proxy.lib.log.LogWrapper;
import com.shouldit.proxy.lib.reflection.ReflectionUtils;
import com.shouldit.proxy.lib.reflection.android.ProxySetting;
import com.shouldit.proxy.lib.utils.ProxyUtils;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.UUID;

public class ProxyConfiguration implements Comparable<ProxyConfiguration>, Serializable
{
    public static final String TAG = "ProxyConfiguration";

    public UUID id;
    public WifiNetworkId internalWifiNetworkId;
    public ProxyStatus status;
    public AccessPoint ap;

    public ProxySetting proxySetting;
    private String proxyHost;
    private Integer proxyPort;
    private String stringProxyExclusionList;
    private String[] parsedProxyExclusionList;

    public ProxyConfiguration(ProxySetting proxyEnabled, String host, Integer port, String exclusionList, WifiConfiguration wifiConf)
    {
        id = UUID.randomUUID();

        proxySetting = proxyEnabled;
        proxyHost = host;
        proxyPort = port;
        setProxyExclusionList(exclusionList);

        if (wifiConf != null)
        {
            ap = new AccessPoint(wifiConf);
            internalWifiNetworkId = new WifiNetworkId(ap.ssid, ap.security);
        }
        else
        {

        }

        status = new ProxyStatus();
    }

    public Proxy getProxy()
    {
        if (proxySetting == ProxySetting.STATIC && proxyHost != null && proxyPort != null)
            return new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost, proxyPort));
        else
            return Proxy.NO_PROXY;
    }

    public boolean isValidProxyConfiguration()
    {
        ProxyStatusItem hostStatus = ProxyUtils.isProxyValidHostname(this);
        ProxyStatusItem portStatus = ProxyUtils.isProxyValidPort(this);

        if (hostStatus.effective && hostStatus.status == CheckStatusValues.CHECKED && hostStatus.result
                && portStatus.effective && portStatus.status == CheckStatusValues.CHECKED && portStatus.result)
        {
            return true;
        }
        else
            return false;

    }

    public void setProxySetting(ProxySetting setting)
    {
        proxySetting = setting;
    }

    public void setProxyHost(String host)
    {
        proxyHost = host;
    }

    public void setProxyPort(Integer port)
    {
        proxyPort = port;
    }

    public void setProxyExclusionList(String exList)
    {
        stringProxyExclusionList = exList;
        parsedProxyExclusionList = ProxyUtils.parseExclusionList(exList);
    }

    public boolean isSameConfiguration(Object another)
    {
        if (!(another instanceof ProxyConfiguration))
        {
            LogWrapper.d(TAG, "Not a ProxyConfiguration object");
            return false;
        }

        ProxyConfiguration anotherConf = (ProxyConfiguration) another;

//        if (this.ap != null && anotherConf.ap != null)
//        {
//            // Both not null
//            if (!this.ap.ssid.equalsIgnoreCase(anotherConf.ap.ssid))
//            {
//                // Different SSID -> Different configurations!
//                return false;
//            }
//            else
//            {
//                if(this.ap.mInfo != anotherConf.ap.mInfo)
//                {
//                    // One AP is connected and one not
//                    return false;
//                }
//
//                if(this.ap.mRssi != anotherConf.ap.mRssi)
//                {
//                    return false;
//                }No need to update proxy
//            }
//        }
//        else if (this.ap != anotherConf.ap)
//        {
//            // At least one is null
//            return false;
//        }

        if (!this.proxySetting.equals(anotherConf.proxySetting))
        {
            LogWrapper.d(TAG, "Different proxy settings toggle status");
            return false;
        }

        if (this.proxyHost != null && anotherConf.proxyHost != null)
        {
            if (!this.proxyHost.equalsIgnoreCase(anotherConf.proxyHost))
            {
                LogWrapper.d(TAG, "Different proxy host value");
                return false;
            }
        }
        else if (this.proxyHost != anotherConf.proxyHost)
        {
            LogWrapper.d(TAG, "Different proxy host set");
            return false;
        }

        if (this.proxyPort != null && anotherConf.proxyPort != null)
        {
            if (!this.proxyPort.equals(anotherConf.proxyPort))
            {
                LogWrapper.d(TAG, "Different proxy port value");
                return false;
            }
        }
        else if (this.proxyPort != anotherConf.proxyPort)
        {
            LogWrapper.d(TAG, "Different proxy port set");
            return false;
        }

        if (this.stringProxyExclusionList != null && anotherConf.stringProxyExclusionList != null)
        {
            if (!this.stringProxyExclusionList.equalsIgnoreCase(anotherConf.stringProxyExclusionList))
            {
                LogWrapper.d(TAG, "Different proxy exclusion list value");
                return false;
            }
        }
        else if (this.stringProxyExclusionList != anotherConf.stringProxyExclusionList)
        {
            LogWrapper.d(TAG, "Different proxy exclusion list set");
            return false;
        }

//        LogWrapper.d(TAG,"Same proxy configuration: \n" +  this.toShortString() + "\n" +  anotherConf.toShortString());
        return true;
    }

    @Override
    public int compareTo(ProxyConfiguration another)
    {
        int result = 0;

        if (this.isCurrentNetwork())
            return -1;
        if (another.isCurrentNetwork())
            return +1;

        if (ap != null && another.ap != null)
        {
            result = ap.compareTo(another.ap);
        }

        return result;
    }

    public boolean updateConfiguration(ProxyConfiguration updated)
    {
        //TODO: Add all required fields for updating an old configuration with an updated version
        if (!this.isSameConfiguration(updated))
        {
            LogWrapper.d(TAG, "Updating proxy configuration: \n" + this.toShortString() + "\n" + updated.toShortString());

            proxySetting = updated.proxySetting;
            proxyHost = updated.proxyHost;
            proxyPort = updated.proxyPort;
            stringProxyExclusionList = updated.stringProxyExclusionList;
            parsedProxyExclusionList = ProxyUtils.parseExclusionList(stringProxyExclusionList);

            status.clear();

            LogWrapper.d(TAG, "Updated proxy configuration: \n" + this.toShortString() + "\n" + updated.toShortString());

            return true;
        }
        else
        {
//            LogWrapper.d(TAG,"No need to update proxy configuration: " + this.toShortString());
            return false;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ID: %s\n", id.toString()));

        if (ap != null)
            sb.append(String.format("Wi-Fi Configuration Info: %s\n", ap.ssid));

        sb.append(String.format("Proxy setting: %s\n", proxySetting.toString()));
        sb.append(String.format("Proxy: %s\n", toStatusString()));
        sb.append(String.format("Is current network: %B\n", isCurrentNetwork()));
        sb.append(String.format("Proxy status checker results: %s\n", status.toString()));

        if (APL.getConnectivityManager().getActiveNetworkInfo() != null)
        {
            sb.append(String.format("Network Info: %s\n", APL.getConnectivityManager().getActiveNetworkInfo()));
        }

        return sb.toString();
    }

    public String toShortString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(id.toString());

        if (ap != null)
            sb.append(" - " + ap.ssid);

        sb.append(" - " + toStatusString());
        sb.append(" " + getProxyExclusionList());

        if (status != null)
            sb.append(" - " + status.toShortString());

        return sb.toString();
    }

    public String toStatusString()
    {
        if (proxySetting == ProxySetting.NONE || proxySetting == ProxySetting.UNASSIGNED)
        {
            return APL.getContext().getResources().getString(R.string.direct_connection);
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(proxyHost) && proxyPort != null && proxyPort > 0)
                sb.append(String.format("%s:%s", proxyHost, proxyPort));
            else
            {
                sb.append(APL.getContext().getResources().getString(R.string.not_set));
            }

            return sb.toString();
        }
    }

    public Boolean isCurrentNetwork()
    {
        WifiInfo connectionInfo = APL.getWifiManager().getConnectionInfo();

        if (ap != null && connectionInfo != null && ap.networkId == connectionInfo.getNetworkId())
            return true;
        else
            return false;
    }


    public Proxy.Type getProxyType()
    {
        return getProxy().type();
    }

    public String getProxyHostString()
    {
        return proxyHost;
    }

    public String getProxyIPHost()
    {
        return proxyHost;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public Integer getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyExclusionList()
    {
        if (stringProxyExclusionList == null)
            return "";
        else
            return stringProxyExclusionList;
    }

    public CheckStatusValues getCheckingStatus()
    {
        return status.getCheckingStatus();
    }

    public String getAPDescription(Context ctx)
    {
//		StringBuilder sb = new StringBuilder();
//		sb.append(ap.ssid);
//		sb.append(" - ");
//		sb.append(ap.getSecurityString(ctx, false));
//		sb.append(" - ");
//		sb.append(toStatusString());
//
//		return sb.toString();

        return String.format("%s (%s)", ap.ssid, ProxyUtils.getSecurityString(ap.security, ap.pskType, ctx, false));
    }

    public String getSSID()
    {
        if (ap != null && ap.wifiConfig != null && ap.wifiConfig.SSID != null)
        {
            return ap.wifiConfig.SSID;
        }
        else
            return null;
    }

//    public String getSecurityString()
//    {
//        if (ap != null)
//        {
//            return ap.getSecurity();
//        }
//    }

    public String getBSSID()
    {
        if (ap != null && ap.bssid != null)
        {
            return ap.bssid;
        }
        else
            return null;
    }

    public boolean isValidConfiguration()
    {
        if (ap != null)
            return true;
        else
            return false;
    }

    @Deprecated
    @TargetApi(12)
    public void writeConfigurationToDevice() throws Exception
    {
        WifiManager wifiManager = (WifiManager) APL.getContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();

        if (configuredNetworks == null || configuredNetworks.size() == 0)
            throw new Exception("Cannot find any configured network during writing configuration to the device: " + this.toShortString());

        WifiConfiguration selectedConfiguration = null;
        for (WifiConfiguration conf : configuredNetworks)
        {
            if (conf.networkId == ap.wifiConfig.networkId)
            {
                selectedConfiguration = conf;
                break;
            }
        }

        if (selectedConfiguration != null)
        {
            Constructor wfconfconstr = WifiConfiguration.class.getConstructors()[1];
            WifiConfiguration newConf = (WifiConfiguration) wfconfconstr.newInstance((Object) selectedConfiguration);

            Field proxySettingsField = newConf.getClass().getField("proxySettings");
            proxySettingsField.set(newConf, (Object) proxySettingsField.getType().getEnumConstants()[proxySetting.ordinal()]);
            Object proxySettings = proxySettingsField.get(newConf);
            int ordinal = ((Enum) proxySettings).ordinal();
            if (ordinal != proxySetting.ordinal())
                throw new Exception("Cannot set proxySettings variable");

            Field linkPropertiesField = newConf.getClass().getField("linkProperties");
            Object linkProperties = linkPropertiesField.get(newConf);
            Field mHttpProxyField = ReflectionUtils.getField(linkProperties.getClass().getDeclaredFields(), "mHttpProxy");
            mHttpProxyField.setAccessible(true);

            if (proxySetting == ProxySetting.NONE || proxySetting == ProxySetting.UNASSIGNED)
            {
                mHttpProxyField.set(linkProperties, null);
            }
            else if (proxySetting == ProxySetting.STATIC)
            {
                Class ProxyPropertiesClass = mHttpProxyField.getType();
                Integer port = getProxyPort();

                if (port == null)
                {
                    Constructor constr = ProxyPropertiesClass.getConstructors()[0];
                    Object ProxyProperties = constr.newInstance((Object) null);
                    mHttpProxyField.set(linkProperties, ProxyProperties);
                }
                else
                {
                    Constructor constr;

                    // TODO: Test again on KitKat Device
                    // NOTE: Hardcoded sdk version number.
                    // Instead of comparing against Build.VERSION_CODES.KITKAT, we directly compare against the version
                    // number to allow devs to compile with an older version of the sdk.
                    if (Build.VERSION.SDK_INT < 19)
                    {
                        constr = ProxyPropertiesClass.getConstructors()[1];
                    }
                    else
                    {
                        constr = ProxyPropertiesClass.getConstructors()[3];
                    }

                    Object ProxyProperties = constr.newInstance(getProxyHostString(), port, getProxyExclusionList());
                    mHttpProxyField.set(linkProperties, ProxyProperties);
                }
            }

//            Object mHttpProxy = mHttpProxyField.get(linkProperties);
//            mHttpProxy = mHttpProxyField.get(linkProperties);

            ReflectionUtils.saveWifiConfiguration(wifiManager, newConf);

            this.status.clear();
            LogWrapper.d(TAG, "Succesfully updated configuration on device: " + this.toShortString());

            LogWrapper.i(TAG, "Sending broadcast intent: " + APLConstants.APL_UPDATED_PROXY_CONFIGURATION);
            Intent intent = new Intent(APLConstants.APL_UPDATED_PROXY_CONFIGURATION);
            APL.getContext().sendBroadcast(intent);
        }
        else
        {
            throw new Exception("Cannot find selected configuration among configured networks during writing to the device: " + this.toShortString());
        }
    }


    public String getAPConnectionStatus()
    {
        if (isCurrentNetwork())
        {
            return APL.getContext().getString(R.string.connected);
        }
        else if (ap.getLevel() > 0)
        {
            return APL.getContext().getString(R.string.available);
        }
        else
        {
            return APL.getContext().getString(R.string.not_available);
        }
    }

}
