public class Config {
    public static String serverUrl = "http://101.132.160.17:8081/Server-1.0-SNAPSHOT/";
    public static String webSocktUrl = "ws://101.132.160.17:8081/Server-1.0-SNAPSHOT/";

//    public static String serverUrl = "http://localhost:8080/Server_war_exploded/";
//    public static String webSocktUrl = "ws://localhost:8080/Server_war_exploded/";

    public static String ServerUrl_createRoom = serverUrl + "create_room";
    public static String ServerUrl_getRoom = serverUrl + "get_room";

    public static String webSocktUrl_relayMessage = webSocktUrl + "relay_message/";
//    public static String ServerUrl_putTime = serverUrl + "put_time";
}
