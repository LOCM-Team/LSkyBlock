package me.locm.lskyblock.provider;

import me.locm.lskyblock.LSkyblock;
import me.locm.lskyblock.skyblock.Island;
import me.locm.lskyblock.utils.Caculator;
import me.locm.lskyblock.utils.Utils;
import org.sqlite.util.StringUtils;
import ru.nukkit.dblib.DbLib;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLiteProvider {

    public Island getIsland(String player) throws SQLException {
        Map<String, String> data = selectAllFormPlayer(player);
        Island island =  new Island(player);
        List<String> members = getMembers(island);
        assert data != null;
        int id = Integer.parseInt(data.get("id"));
        island.setId(id);
        island.setMembers(members);
        island.setSpawn(Caculator.getDefaultSpawn(id));
        return island;
    }

    public boolean hasIsland(String player){
        boolean has = false;
        try{
            Map<String, String> data = selectAllFormPlayer(player);
            assert data != null;
            has = !(data.get("id") == null);
        }catch (SQLException ignored){}
        return has;
    }

    public List<Island> getIslands(){
        List<Island> islands = new ArrayList<>();
        try{
            islands = selectAll();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return islands;
    }

    public List<String> getMembers(Island island) throws SQLException {
        String memberstr = get(island.getOwner(), "members");
        assert memberstr != null;
        return Utils.stringToList(memberstr);
    }

    public void createIsland(String player, int id) throws SQLException {
        String query = "insert into lskyblock(id, player, members, pvp)" +
                        " values ('"+id+"', '"+player.toLowerCase()+"', '', '0')";
        executeUpdate(query);
    }

    public static Connection connectToSQLite(){
        return connectToSQLite("databases.db");
    }

    public void updateIsland(Island island){
        int id = island.getId();
        String owner = island.getOwner();
        List<String> members = island.getMembers();
        System.out.println(members);
        int pvp = island.getPvp() ? 1 : 0;
        String query = "insert or replace into lskyblock(id, player, members, pvp)" +
                        " values ('"+id+"', '"+owner.toLowerCase()+"', '" + StringUtils.join(members, ",") + "', '"+ pvp +"')";
        try{
            executeUpdate(query);
        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    public static List<Island> selectAll() throws SQLException{
        String query = "select * from lskyblock";
        List<Island> islands = new ArrayList<>();
        Connection connection = connectToSQLite();
        if (connection == null) return islands;
        ResultSet resultSet = connection.createStatement().executeQuery(query);
        if (resultSet == null) return null;
        while (resultSet.next()) {
            Island island = new Island(resultSet.getString("player"));
            island.setId(Integer.parseInt(resultSet.getString("id")));
            island.setMembers(Utils.stringToList(resultSet.getString("members")));
            island.setPvp(resultSet.getString("pvp").equals("1"));
            islands.add(island);
        }
        resultSet.close();
        connection.close();
        return islands;
    }

    public static Connection connectToSQLite(String filename){
        File file = new File(LSkyblock.getInstance().getDataFolder() + File.separator + filename);
        return DbLib.getSQLiteConnection(file);
    }

    public static Map<String, String> selectAllFormPlayer(String player) throws SQLException {
        String query = "select * from lskyblock where player='" + player.toLowerCase() + "'";
        Map<String, String> list = new HashMap<>();
        Connection connection = connectToSQLite();
        if (connection == null) return list;
        ResultSet resultSet = connection.createStatement().executeQuery(query);
        if (resultSet == null) return null;
        while (resultSet.next()) {
            list.put("id", resultSet.getString("id"));
            list.put("player", player);
            list.put("members", resultSet.getString("members"));
            list.put("pvp", resultSet.getString("pvp"));
        }
        resultSet.close();
        connection.close();
        return list;
    }

    public static String get(String player, String component) throws SQLException {
        String query = "select * from lskyblock where player='" + player.toLowerCase() + "'";
        Connection connection = connectToSQLite();
        if (connection == null) return "";
        ResultSet resultSet = connection.createStatement().executeQuery(query);
        if (resultSet == null) return null;
        connection.close();
        return resultSet.getString(component);
    }

    public static void executeUpdate(String query) throws SQLException {
        Connection connection = connectToSQLite();
        if (connection == null) return;
        connection.createStatement().executeUpdate(query);
        try {
            connection.close();
        } catch (SQLException ignored) {}
    }

    public static void create() throws SQLException {
        String query = "create table if not exists lskyblock (id varchar(20), player varchar(20), members text, pvp varchar(1))";
        executeUpdate(query);
    }

}
