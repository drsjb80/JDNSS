package edu.msudenver.cs.jdnss;


import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerCookie {


      final static Logger logger = JDNSS.logger;
      private String serverSecret = JDNSS.jargs.getServerSecret();
      private long hash;

    ServerCookie(byte[] clientCookie, String clientIP){
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie) + clientIP + serverSecret);
        logger.trace(serverSecret);
        hash = fnv.getHash();
        logger.trace(hash);
    }

    //check server cookie based on a client cookie and IP Address
    protected boolean isValid(byte[] clientCookie, String clientIP){
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie) + clientIP + serverSecret);
        return this.hash == fnv.getHash();
    }

    protected byte[] getBytes(){
      return Utils.getBytes(this.hash);
    }


}
