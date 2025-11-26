package utils;

public class Logger {
   public String identifier;

   public Logger(String id) {
    identifier = id;
   }

   public void log(String message) {
    System.out.println("[" + identifier + "]: " + message);
   }
}
