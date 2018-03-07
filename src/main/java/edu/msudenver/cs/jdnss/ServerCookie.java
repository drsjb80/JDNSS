package edu.msudenver.cs.jdnss;


import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerCookie {

  private String serverSecret; //TODO should come from /etc/named.conf
  private long hash;

  //contruct a new Server Cookie from a valid incoming client cookie.
  ServerCookie(byte[] clientCookie) {
    serverSecret = getServerSecret();
    FNV1a64 fnv = new FNV1a64();
    fnv.init(new String(clientCookie));
    hash = fnv.getHash();
  }

  //I think this is how it should really look?
  ServerCookie(byte[] clientCookie, String clientIP){
    serverSecret = getServerSecret();
    FNV1a64 fnv = new FNV1a64();
    fnv.init(new String(clientCookie) + clientIP + serverSecret);
    hash = fnv.getHash();
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
      String filename = "/etc/named.conf";
      String line = null;

      try {
          File file = new File(filename);

          FileInputStream fis = new FileInputStream(file);
          byte[] data = new byte[(int) file.length()];
          fis.read(data);
          fis.close();

          String confFile = new String(data, "UTF-8");
          Pattern p = Pattern.compile("cookie-secret\\s+\"(.*)\"");

          Matcher m = p.matcher(confFile);

          // Here we will need to decide what to do if no server secret is found
          if (m.find()) {
              return m.group(0);
          }
          else {
              System.out.println("Couldnt find Server Secret");
          }


      }
      catch (FileNotFoundException e) {
          System.out.println("Something wrong with Server Secret: " + e);
      }
      catch (IOException e){
          System.out.println("Something wrong with Server Secret: " + e);

      }
  }
  protected byte[] getBytes(){
    return Utils.getBytes(this.hash);
  }

}
