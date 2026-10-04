import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** Uses the existing local database configuration without printing credentials. */
public class LocalStaffData {
    static final String[] ACCOUNTS = {"flower_manager", "flower_goods", "flower_florist", "flower_delivery", "flower_service"};
    static final String[] ROLES = {"门店店长", "商品运营", "花艺师", "配送员", "客服专员"};
    static final String[] NAMES = {"林晓（店长）", "陈悦（运营）", "苏禾（花艺）", "周晨（配送）", "许宁（客服）"};
    static final int[][] RULES = {
        {2,14,15,16,17,18,19,20,21,22,25,26,27,29,31,273,277,278,389,491,294,295,296,297,298,299,300,301,302,4,39,44,315,316,578,580,581,582,583,584,585,586,587,588,589,590,591,592,593,594,595,596,597,650,651,652,653,654,655,656,657},
        {2,14,15,16,17,18,19,20,21,22,25,26,27,29,31,273,277,278,389,491,294,295,296,297,298,299,300,301,302,578,582,583,650,651,652,653,654,655,656,657},
        {2,14,273,277,294,295,299,300,301,578,580,582,585,596,597,650,651,655,656},
        {578,580,581,586,587,589,592,596,597},
        {4,39,44,315,316,578,580,584,596,597,655,656}
    };
    static SortedSet<Integer> rules(int index) {
        SortedSet<Integer> ids = new TreeSet<>(Arrays.asList(280,281,282,283,488,489));
        for (int id : RULES[index]) ids.add(id);
        return ids;
    }
    static String quoted(String value) { return "'" + value.replace("'", "''") + "'"; }
    static String password(String account) throws Exception {
        byte[] key = new byte[8]; Arrays.fill(key, (byte) 1);
        byte[] source = account.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(source, 0, key, 0, Math.min(8, source.length));
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key,"DES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal("Flower@2026!".getBytes(StandardCharsets.UTF_8)));
    }
    static void generate(Path target) throws Exception {
        StringBuilder sql = new StringBuilder("-- 花间集本地示例员工与岗位权限；仅新增，重复执行不覆盖现有账号或角色。\n-- 使用项目现有密码编码；初始密码见配套说明。\n-- 前置：花卉、花材库存、养花指南升级已完成。\n");
        for (int i=0;i<ACCOUNTS.length;i++) {
            sql.append("\n-- ").append(ROLES[i]).append("\n");
            sql.append("INSERT INTO eb_system_role (role_name,rules,level,status,create_time,update_time) SELECT ").append(quoted(ROLES[i])).append(",'',1,1,NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM eb_system_role WHERE role_name=").append(quoted(ROLES[i])).append(");\n");
            for (int id : rules(i)) sql.append("INSERT INTO eb_system_role_menu (rid,menu_id) SELECT r.id,m.id FROM eb_system_role r JOIN eb_system_menu m ON m.id=").append(id).append(" AND m.is_delte=0 WHERE r.role_name=").append(quoted(ROLES[i])).append(" AND NOT EXISTS (SELECT 1 FROM eb_system_role_menu rm WHERE rm.rid=r.id AND rm.menu_id=m.id);\n");
            sql.append("INSERT INTO eb_system_admin (account,pwd,real_name,roles,level,status,is_del,phone,is_sms,last_ip,login_count,create_time,update_time) SELECT ").append(quoted(ACCOUNTS[i])).append(",").append(quoted(password(ACCOUNTS[i]))).append(",").append(quoted(NAMES[i])).append(",CAST(r.id AS CHAR),1,1,0,'1390000000").append(i+1).append("',0,'',0,NOW(),NOW() FROM eb_system_role r WHERE r.role_name=").append(quoted(ROLES[i])).append(" AND NOT EXISTS (SELECT 1 FROM eb_system_admin a WHERE a.account=").append(quoted(ACCOUNTS[i])).append(");\n");
        }
        Files.write(target,sql.toString().getBytes(StandardCharsets.UTF_8));
    }
    static void verify(Connection connection) throws Exception {
        for (int i=0;i<ACCOUNTS.length;i++) {
            try (PreparedStatement statement=connection.prepareStatement("SELECT a.id,a.level,a.status,a.is_del,r.id AS rid,r.status AS role_status FROM eb_system_admin a JOIN eb_system_role r ON a.roles=CAST(r.id AS CHAR) WHERE a.account=? AND r.role_name=?")) {
                statement.setString(1,ACCOUNTS[i]); statement.setString(2,ROLES[i]);
                try (ResultSet result=statement.executeQuery()) {
                    if (!result.next() || result.getInt("level") != 1 || result.getInt("status") != 1 || result.getInt("is_del") != 0 || result.getInt("role_status") != 1) throw new IllegalStateException("Invalid staff account: " + ACCOUNTS[i]);
                    int rid=result.getInt("rid");
                    SortedSet<Integer> actual=new TreeSet<>();
                    try (PreparedStatement permissions=connection.prepareStatement("SELECT rm.menu_id FROM eb_system_role_menu rm JOIN eb_system_menu m ON m.id=rm.menu_id AND m.is_delte=0 WHERE rm.rid=?")) {
                        permissions.setInt(1,rid);
                        try (ResultSet rows=permissions.executeQuery()) { while(rows.next()) actual.add(rows.getInt(1)); }
                    }
                    if (!rules(i).equals(actual)) throw new IllegalStateException("Permission mismatch: " + ACCOUNTS[i]);
                    if (result.next()) throw new IllegalStateException("Duplicate staff account");
                    System.out.println(ACCOUNTS[i]+" | "+ROLES[i]+" | enabled | permissions="+actual.size());
                }
            }
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("generate")) { generate(Paths.get(args[1])); return; }
        Map<String,Object> config;
        try (java.io.InputStream input = Files.newInputStream(Paths.get("backend/crmeb-admin/src/main/resources/application.yml"))) {
            config = new Yaml().load(input);
        }
        Map<String,Object> datasource = (Map<String,Object>) ((Map<String,Object>) config.get("spring")).get("datasource");
        String url = String.valueOf(datasource.get("url"));
        if (!url.startsWith("jdbc:mysql://127.0.0.1:")) throw new IllegalStateException("Local database required");
        Class.forName(String.valueOf(datasource.get("driver-class-name")));
        try (Connection connection = DriverManager.getConnection(url, String.valueOf(datasource.get("username")), String.valueOf(datasource.get("password")))) {
            if (args.length>0 && args[0].equals("apply")) {
                connection.setAutoCommit(false);
                List<String> admins=LocalFlowerData.snapshot(connection,"SELECT * FROM eb_system_admin ORDER BY id");
                List<String> roles=LocalFlowerData.snapshot(connection,"SELECT * FROM eb_system_role ORDER BY id");
                List<String> existingPermissions=LocalFlowerData.snapshot(connection,"SELECT * FROM eb_system_role_menu ORDER BY rid,menu_id");
                int affected=0;
                try {
                    for(String sql:LocalFlowerData.statements(new String(Files.readAllBytes(Paths.get(args[1])),StandardCharsets.UTF_8))) {
                        if(!sql.toUpperCase(Locale.ROOT).startsWith("INSERT INTO ")) throw new IllegalArgumentException("Insert-only seed required");
                        try(Statement statement=connection.createStatement()) {affected+=statement.executeUpdate(sql);}
                    }
                    verify(connection);
                    if(!LocalFlowerData.snapshot(connection,"SELECT * FROM eb_system_admin ORDER BY id").containsAll(admins) || !LocalFlowerData.snapshot(connection,"SELECT * FROM eb_system_role ORDER BY id").containsAll(roles) || !LocalFlowerData.snapshot(connection,"SELECT * FROM eb_system_role_menu ORDER BY rid,menu_id").containsAll(existingPermissions)) throw new IllegalStateException("Existing data changed");
                    connection.commit();
                    System.out.println("Committed insert-only seed; affected rows="+affected+"; existing records preserved.");
                } catch(Exception error) {connection.rollback();throw error;}
            } else if(args.length>0 && args[0].equals("verify")) {
                connection.setReadOnly(true); verify(connection);
            } else {
                connection.setReadOnly(true);
                for (String sql : args) LocalFlowerData.query(connection, sql);
            }
        }
    }
}
