package edu.msudenver.cs.jdnss;

import lombok.Getter;

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
    hash = fnv.getHash();
  }

  private String getServerSecret(){

  }
  protected byte[] getBytes(){
    return Utils.getBytes(this.hash);
  }

}
