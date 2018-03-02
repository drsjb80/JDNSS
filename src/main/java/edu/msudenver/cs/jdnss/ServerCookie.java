package edu.msudenver.cs.jdnss;

import lombok.Getter;

public class ServerCookie {

  private String clientIP;
  private String clientCookie;
  private String serverSecret;
  @Getter private byte[] rawServerCookie;

  ServerCookie(byte[] clientCookie){
    FNV1a64 fnv = new FNV1a64();
    fnv.init(new String(clientCookie));
    rawServerCookie = Utils.getBytes(fnv.getHash());
  }

}
