package com.sdk.ad;


import com.github.megatronking.stringfog.xor.StringFogImpl;

import com.github.megatronking.stringfog.Base64;
/**
 * Generated code from StringFog gradle plugin. Do not modify!
 */
public final class StringFog {
  private static final StringFogImpl IMPL = new StringFogImpl();

  public static String decrypt(String value, String key) {
    return IMPL.decrypt(Base64.decode(value, Base64.DEFAULT), Base64.decode(key, Base64.DEFAULT));
  }

}
