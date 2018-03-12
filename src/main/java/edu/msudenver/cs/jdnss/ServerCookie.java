package edu.msudenver.cs.jdnss;


import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerCookie {


      final static Logger logger = JDNSS.logger;
      private String serverSecret = getServerSecret();
      private long hash;

    //I think this is how it should really look?
    ServerCookie(byte[] clientCookie, String clientIP){
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie) + clientIP + serverSecret);
        logger.trace(serverSecret);
        hash = fnv.getHash();
        logger.trace(hash);
    }

    //check incoming server cookie to see if it is valid
    //FIXME this is bad
    protected boolean isValid(byte[] clientCookie, String clientIP){
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie) + clientIP + serverSecret);
        return this.hash == fnv.getHash();
    }

    /*
    *
    * What happens if server secret is not there?
    *
    */
    private String getServerSecret(){
        String filename = JDNSS.jargs.ServerSecretLocation;

        if (filename == null) {
            filename = "/etc/jnamed.conf";
        }

        try {
            // Reads Server Secret from jargs.ServerSecretLocation
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String confFile = new String(data, "UTF-8");
            fis.close();

            // Find ServerSecret with Regex
            Pattern p = Pattern.compile("cookie-secret\\s+\"(.*)\"");
            Matcher m = p.matcher(confFile);

            // Here we will need to decide what to do if no server secret is found
            if (m.find()) {
                return m.group(1);
            }
            else {
               logger.warn("Couldnt find Server Secret");
                return "123456789";
            }


        }
        catch (FileNotFoundException e) {
            System.out.println("Something wrong with Server Secret: " + e);
            return "123456789";
        }
        catch (IOException e){
            System.out.println("Something wrong with Server Secret: " + e);
            return "123456789";

        }
    }

    protected byte[] getBytes(){
      return Utils.getBytes(this.hash);
    }


}
