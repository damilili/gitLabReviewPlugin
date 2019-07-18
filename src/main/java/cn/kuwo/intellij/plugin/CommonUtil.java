package cn.kuwo.intellij.plugin;

import org.apache.commons.lang.StringUtils;

import java.awt.*;
import java.net.URI;

public class CommonUtil {
    public static void openWebPage(String uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(uri));
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean urlMatch(String remoteUrl, String apiUrl) {
        String formattedRemoteUrl = remoteUrl.trim();
        String formattedApiUrl = apiUrl.trim();
        formattedRemoteUrl = formattedRemoteUrl.replace("https://", "");
        formattedRemoteUrl = formattedRemoteUrl.replace("http://", "");
        return StringUtils.isNotBlank(formattedApiUrl) && StringUtils.isNotBlank(formattedRemoteUrl) && formattedApiUrl.toLowerCase().contains(formattedRemoteUrl.toLowerCase());
    }
}
