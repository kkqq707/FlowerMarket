import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Local development data audit and transactional, insert-only seed runner. */
public class LocalFlowerData {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Map<String,Object> config;
        try (java.io.InputStream input = Files.newInputStream(Paths.get("backend/crmeb-admin/src/main/resources/application.yml"))) {
            config = new Yaml().load(input);
        }
        Map<String,Object> spring = (Map<String,Object>) config.get("spring");
        Map<String,Object> datasource = (Map<String,Object>) spring.get("datasource");
        String url = String.valueOf(datasource.get("url"));
        if (!url.startsWith("jdbc:mysql://127.0.0.1:")) throw new IllegalStateException("Only the configured local development database is supported");
        Class.forName(String.valueOf(datasource.get("driver-class-name")));
        try (Connection connection = DriverManager.getConnection(url, String.valueOf(datasource.get("username")), String.valueOf(datasource.get("password")))) {
            connection.setAutoCommit(false);
            if (args.length == 0 || args[0].equals("audit")) {
                connection.setReadOnly(true);
                for (String table : Arrays.asList("eb_store_product", "eb_store_product_rule", "eb_store_product_attr", "eb_store_product_attr_value", "eb_store_product_attr_result", "eb_store_product_attr_option", "eb_store_product_reply", "eb_flower_material", "eb_flower_material_batch", "eb_flower_recipe", "eb_flower_delivery_slot")) {
                    query(connection, "SELECT '" + table + "' AS table_name,COUNT(*) AS rows_count FROM " + table);
                }
                query(connection, "SELECT c.id,c.name,COUNT(p.id) AS flower_count FROM eb_category c LEFT JOIN eb_store_product p ON FIND_IN_SET(c.id,p.cate_id) AND p.is_del=0 AND p.is_show=1 WHERE c.type=1 GROUP BY c.id,c.name ORDER BY c.id");
                for (String table : Arrays.asList("eb_store_product", "eb_store_product_rule", "eb_store_product_attr_value", "eb_store_product_attr_result", "eb_store_product_reply")) query(connection, "SHOW COLUMNS FROM " + table);
                query(connection, "SELECT id,rule_name,rule_value FROM eb_store_product_rule ORDER BY id");
                query(connection, "SELECT p.id,p.store_name,p.price,p.stock,p.spec_type,COUNT(v.id) AS sku_count, SUM(v.stock) AS sku_stock FROM eb_store_product p JOIN eb_flower_product_ext e ON e.product_id=p.id LEFT JOIN eb_store_product_attr_value v ON v.product_id=p.id AND v.type=0 AND v.is_del=0 WHERE p.is_del=0 GROUP BY p.id ORDER BY p.id");
            } else if (args[0].equals("apply")) {
                String source = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);
                int affected = 0;
                try {
                    List<String> originalProducts = snapshot(connection, "SELECT * FROM eb_store_product WHERE id BETWEEN 900001 AND 900012 ORDER BY id");
                    List<String> originalSkus = snapshot(connection, "SELECT * FROM eb_store_product_attr_value WHERE product_id BETWEEN 900001 AND 900012 ORDER BY id");
                    List<String> originalReviews = snapshot(connection, "SELECT * FROM eb_store_product_reply ORDER BY id");
                    for (String sql : statements(source)) {
                        if (!sql.trim().toUpperCase(Locale.ROOT).startsWith("INSERT INTO ")) throw new IllegalArgumentException("Only INSERT statements are allowed");
                        try (Statement statement = connection.createStatement()) { affected += statement.executeUpdate(sql); }
                    }
                    verify(connection);
                    if (!originalProducts.equals(snapshot(connection, "SELECT * FROM eb_store_product WHERE id BETWEEN 900001 AND 900012 ORDER BY id")) ||
                        !originalSkus.equals(snapshot(connection, "SELECT * FROM eb_store_product_attr_value WHERE product_id BETWEEN 900001 AND 900012 ORDER BY id")) ||
                        !originalReviews.equals(snapshot(connection, "SELECT * FROM eb_store_product_reply ORDER BY id"))) {
                        throw new IllegalStateException("Existing products, SKUs or customer reviews changed; rolling back");
                    }
                    connection.commit();
                    System.out.println("Seed transaction committed; affected rows: " + affected);
                } catch (Exception error) { connection.rollback(); throw error; }
            } else if (args[0].equals("verify")) {
                connection.setReadOnly(true);
                verify(connection);
            } else throw new IllegalArgumentException("Use audit, apply <sql>, or verify");
        }
    }
    static void verify(Connection connection) throws SQLException {
        query(connection, "SELECT COUNT(*) AS in_sale_flowers FROM eb_store_product p JOIN eb_flower_product_ext e ON e.product_id=p.id WHERE p.is_del=0 AND p.is_recycle=0 AND p.is_show=1");
        query(connection, "SELECT COUNT(*) AS flower_specification_templates FROM eb_store_product_rule WHERE id BETWEEN 930001 AND 930006");
        assertCount(connection, "SELECT COUNT(*) FROM eb_store_product WHERE id BETWEEN 900013 AND 900036", 24);
        assertCount(connection, "SELECT COUNT(*) FROM eb_store_product_rule WHERE id BETWEEN 930001 AND 930006", 6);
        assertCount(connection, "SELECT COUNT(*) FROM eb_store_product WHERE id BETWEEN 900013 AND 900036 AND bar_code LIKE 'FD%' AND is_del=0 AND is_show=1", 24);
        assertCount(connection, "SELECT COUNT(*) FROM eb_store_product p LEFT JOIN eb_store_product_attr_value v ON v.product_id=p.id AND v.type=0 AND v.is_del=0 WHERE p.id BETWEEN 900013 AND 900036 GROUP BY p.id HAVING COUNT(v.id)<>2 OR SUM(v.stock)<>MAX(p.stock) OR MIN(v.price)<>MAX(p.price)", 0, true);
        assertCount(connection, "SELECT COUNT(*) FROM eb_store_product p LEFT JOIN eb_flower_product_ext e ON e.product_id=p.id LEFT JOIN eb_store_product_description d ON d.product_id=p.id AND d.type=0 LEFT JOIN eb_store_product_attr_result r ON r.product_id=p.id AND r.type=0 WHERE p.id BETWEEN 900013 AND 900036 AND (e.id IS NULL OR d.id IS NULL OR r.id IS NULL)", 0);
        assertCount(connection, "SELECT COUNT(*) FROM eb_category c WHERE c.type=1 AND c.status=1 AND NOT EXISTS(SELECT 1 FROM eb_store_product p WHERE FIND_IN_SET(c.id,p.cate_id) AND p.is_del=0 AND p.is_show=1)", 0);
        System.out.println("Product/SKU totals, detail associations and category coverage verified.");
    }
    static List<String> snapshot(Connection connection, String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            int count = result.getMetaData().getColumnCount();
            while (result.next()) {
                StringJoiner row = new StringJoiner("\u0000");
                for (int i=1; i<=count; i++) row.add(String.valueOf(result.getString(i)));
                rows.add(row.toString());
            }
        }
        return rows;
    }
    static void assertCount(Connection connection, String sql, int expected) throws SQLException { assertCount(connection,sql,expected,false); }
    static void assertCount(Connection connection, String sql, int expected, boolean rowCount) throws SQLException {
        try (Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(sql)) {
            int count=0;
            if(rowCount) {while(result.next()) count++;} else {result.next();count=result.getInt(1);}
            if(count!=expected) throw new IllegalStateException("Data consistency check failed: expected="+expected+",actual="+count);
        }
    }
    static void query(Connection connection,String sql) throws SQLException {
        try(Statement statement=connection.createStatement();ResultSet result=statement.executeQuery(sql)) {
            ResultSetMetaData columns=result.getMetaData();
            while(result.next()) {StringJoiner line=new StringJoiner(" | ");for(int i=1;i<=columns.getColumnCount();i++) line.add(columns.getColumnLabel(i)+"="+result.getString(i));System.out.println(line);}
        }
    }
    static List<String> statements(String source) {
        List<String> statements=new ArrayList<>();StringBuilder current=new StringBuilder();char quote=0;boolean comment=false;
        for(int i=0;i<source.length();i++) {char c=source.charAt(i);
            if(comment){if(c=='\n'){comment=false;current.append('\n');}continue;}
            if(quote==0 && c=='-' && i+1<source.length() && source.charAt(i+1)=='-'){comment=true;i++;continue;}
            if(quote!=0 && c=='\\' && i+1<source.length()){current.append(c).append(source.charAt(++i));continue;}
            if(quote!=0 && c==quote)quote=0;else if(quote==0 && (c=='\''||c=='"'||c=='`'))quote=c;
            if(c==';' && quote==0){if(!current.toString().trim().isEmpty()) statements.add(current.toString().trim());current.setLength(0);}else current.append(c);
        }
        if(!current.toString().trim().isEmpty())statements.add(current.toString().trim());return statements;
    }
}
