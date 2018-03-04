package edu.msudenver.cs.jdnss;

import lombok.Getter;

public class ServerCookie {

  private String clientIP;
  private String clientCookie;
  private String serverSecret;
  private long hash;

  //contruct a new Server Cookie from a valid incoming client cookie.
  // TODO include clientIP and serverSecret
  ServerCookie(byte[] clientCookie){
    FNV1a64 fnv = new FNV1a64();
    fnv.init(new String(clientCookie));
    hash = fnv.getHash();
  }

  protected byte[] getBytes(){
    return Utils.getBytes(this.hash);
  }

}
