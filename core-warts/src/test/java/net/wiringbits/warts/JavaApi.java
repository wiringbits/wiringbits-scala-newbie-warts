package net.wiringbits.warts;

// A Java-defined API outside the standard library, used to check that the wart skips Java callees.
public class JavaApi {
  public JavaApi(String from, String to) {}

  public static void transfer(String from, String to) {}
}
