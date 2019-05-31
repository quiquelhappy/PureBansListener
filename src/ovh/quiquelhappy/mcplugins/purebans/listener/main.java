package ovh.quiquelhappy.mcplugins.purebans.listener;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.concurrent.TimeUnit;

public class main extends Plugin {

    Connection conn = null;
    ProxyServer proxy = ProxyServer.getInstance();


    @Override
    public void onEnable() {
        super.onEnable();

        System.out.println("  _____                ____                  ");
        System.out.println(" |  __ \\              |  _ \\                 ");
        System.out.println(" | |__) |   _ _ __ ___| |_) | __ _ _ __  ___ ");
        System.out.println(" |  ___/ | | | '__/ _ \\  _ < / _` | '_ \\/ __|");
        System.out.println(" | |   | |_| | | |  __/ |_) | (_| | | | \\__ \\");
        System.out.println(" |_|    \\__,_|_|  \\___|____/ \\__,_|_| |_|___/");

        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");


        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Configuration config = null;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            System.out.println("[PureBans] Couldn't load default config: "+e.getMessage());
        }

        String direction = config.getString("mysql.direction");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");
        Boolean ssl = config.getBoolean("mysql.SSL");

        try {
            conn = DriverManager.getConnection("jdbc:mysql://"+direction+":"+port+"/"+database+"?user="+username+"&password="+password+"&useSSL="+ssl);
            System.out.println("[PureBans] Connected to the database");
        } catch (SQLException e) {
            System.out.println("[PureBans] Couldn't connect to the database");
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }


        getProxy().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                checkUnchecked();
            }
        }, 1, 10, TimeUnit.SECONDS);


    }

    public void checkUnchecked(){

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT chat_points,gameplay_points,player,operator,contextid FROM pure_bans WHERE checked = 0");
            while (rs.next()) {

                Statement stmt2 = conn.createStatement();
                ResultSet rs2 = stmt2.executeQuery("SELECT username FROM luckperms_players WHERE uuid = '"+rs.getString("player")+"'");

                if(rs2.next()){
                    System.out.println("[PureBans] Executing punishment for "+rs2.getString("username")+", #"+rs.getString("contextid")+". Chat points: "+rs.getInt("chat_points")+", Gameplay points: "+rs.getInt("gameplay_points")+".");

                    String player = rs2.getString("username");
                    String id = rs.getString("contextid");
                    Integer chat = rs.getInt("chat_points");
                    Integer gameplay = rs.getInt("gameplay_points");

                    if(chat>0&&chat<=50){
                        // warning
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "warn "+player+" Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    } else if(chat>50&&chat<=150){
                        // temp mute 24h
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "tempmute "+player+" 24h Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    } else if(chat>150&&chat<=250){
                        // temp mute 1mo
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "tempmute "+player+" 30d Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    } else if(chat>250){
                        // perma mute
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "mute "+player+" Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    }

                    if(gameplay>0&&gameplay<100) {
                        // warning
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "warn "+player+" Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    } else if(gameplay>=100&&gameplay<150){
                        // temp ban (8h)
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "tempban "+player+" 8h Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    } else if(gameplay>=150&&gameplay<300){
                        // temp ban (48h)
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "tempban "+player+" 48h Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    } else if(gameplay>=300){
                        // perma ban
                        proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "ban "+player+" Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);
                    }

                    Statement stmt3 = conn.createStatement();
                    stmt3.executeUpdate("UPDATE `pure_bans` SET `checked`=true WHERE contextid='"+id+"'");
                    proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "kick "+player+" Please, improve your behavior. Appeal here: https://www.purevanilla.es/punishments/appeal?id="+id);

                } else {
                    System.out.println("[PureBans] Player for "+rs.getString("contextid")+" was not found");
                }


            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
