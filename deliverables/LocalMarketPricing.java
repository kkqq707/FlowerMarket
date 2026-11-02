import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/** Audits and applies the explicitly listed local demo catalog prices. */
public class LocalMarketPricing {
    // 2026-11-02 local demo retail price list. References/rationale: flower-market-pricing.md.
    // Each row: standard SKU, larger SKU (zero for single-SKU products).
    static final int[][] PRICES={
        {129,0},{128,0},{159,0},{169,0},{189,0},{179,0},
        {199,0},{239,0},{239,0},{159,0},{269,0},{299,0},
        {78,118},{159,209},{199,269},{99,139},{128,168},{169,219},
        {99,139},{189,249},{78,118},{148,198},{99,139},{139,189},
        {139,189},{118,158},{179,239},{199,259},{269,339},{179,229},
        {199,259},{229,289},{299,369},{259,329},{139,189},{88,128}
    };
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Map<String,Object> config;
        try (java.io.InputStream input = Files.newInputStream(Paths.get("backend/crmeb-admin/src/main/resources/application.yml"))) {
            config = new Yaml().load(input);
        }
        Map<String,Object> datasource=(Map<String,Object>)((Map<String,Object>)config.get("spring")).get("datasource");
        String url=String.valueOf(datasource.get("url"));
        if(!url.startsWith("jdbc:mysql://127.0.0.1:")) throw new IllegalStateException("Local development database required");
        Class.forName(String.valueOf(datasource.get("driver-class-name")));
        try(Connection connection=DriverManager.getConnection(url,String.valueOf(datasource.get("username")),String.valueOf(datasource.get("password")))) {
            connection.setAutoCommit(false);
            String mode=args.length==0 ? "audit" : args[0];
            if(mode.equals("audit")) {
                connection.setReadOnly(true);
                LocalFlowerData.query(connection,"SELECT p.id,p.store_name,p.price,p.cost,p.ot_price,e.stem_count,e.packaging FROM eb_store_product p JOIN eb_flower_product_ext e ON e.product_id=p.id WHERE p.id BETWEEN 900001 AND 900036 ORDER BY p.id");
            } else if(mode.equals("check") || mode.equals("apply")) {
                try {
                    LocalOperationsData.require(connection,"SELECT COUNT(*) FROM eb_store_product p JOIN eb_flower_product_ext e ON e.product_id=p.id WHERE p.id BETWEEN 900001 AND 900036 AND p.is_del=0",36);
                    List<String> orders=LocalFlowerData.snapshot(connection,"SELECT * FROM eb_store_order ORDER BY id");
                    List<String> items=LocalFlowerData.snapshot(connection,"SELECT * FROM eb_store_order_info ORDER BY id");
                    List<String> otherProducts=LocalFlowerData.snapshot(connection,"SELECT * FROM eb_store_product WHERE id NOT BETWEEN 900001 AND 900036 ORDER BY id");
                    List<String> quantities=LocalFlowerData.snapshot(connection,"SELECT id,stock,sales,cost FROM eb_store_product WHERE id BETWEEN 900001 AND 900036 ORDER BY id");
                    List<String> skuQuantities=LocalFlowerData.snapshot(connection,"SELECT id,stock,sales,cost FROM eb_store_product_attr_value WHERE product_id BETWEEN 900001 AND 900036 ORDER BY id");
                    int changed=updatePrices(connection);
                    verify(connection);
                    if(!orders.equals(LocalFlowerData.snapshot(connection,"SELECT * FROM eb_store_order ORDER BY id")) ||
                       !items.equals(LocalFlowerData.snapshot(connection,"SELECT * FROM eb_store_order_info ORDER BY id")) ||
                       !otherProducts.equals(LocalFlowerData.snapshot(connection,"SELECT * FROM eb_store_product WHERE id NOT BETWEEN 900001 AND 900036 ORDER BY id")) ||
                       !quantities.equals(LocalFlowerData.snapshot(connection,"SELECT id,stock,sales,cost FROM eb_store_product WHERE id BETWEEN 900001 AND 900036 ORDER BY id")) ||
                       !skuQuantities.equals(LocalFlowerData.snapshot(connection,"SELECT id,stock,sales,cost FROM eb_store_product_attr_value WHERE product_id BETWEEN 900001 AND 900036 ORDER BY id")))
                        throw new IllegalStateException("Protected orders, cost or inventory changed");
                    if(mode.equals("check")) connection.rollback(); else connection.commit();
                    System.out.println("Pricing "+mode+" verified; changed rows="+changed+"; original orders, cost and stock unchanged");
                } catch(Exception error) {connection.rollback();throw error;}
            } else if(mode.equals("verify")) {connection.setReadOnly(true);verify(connection);}
            else throw new IllegalArgumentException("Use audit, check, apply or verify");
        }
    }
    static int updatePrices(Connection connection) throws SQLException {
        int changed=0;
        try(PreparedStatement sku=connection.prepareStatement("UPDATE eb_store_product_attr_value SET price=?,ot_price=? WHERE id=? AND (price<>? OR ot_price<>?)");
            PreparedStatement product=connection.prepareStatement("UPDATE eb_store_product SET price=?,vip_price=?,ot_price=?,version=version+1 WHERE id=? AND (price<>? OR vip_price<>? OR ot_price<>?)");
            PreparedStatement snapshot=connection.prepareStatement("UPDATE eb_store_product_attr_result SET result=JSON_SET(result,?,CAST(? AS DECIMAL(10,2)),?,CAST(? AS DECIMAL(10,2))) WHERE product_id=? AND type=0 AND (JSON_EXTRACT(result,?)<>? OR JSON_EXTRACT(result,?)<>?)")) {
            for(int n=0;n<PRICES.length;n++) {
                int productId=900001+n;
                List<Integer> skuIds=new ArrayList<>();
                try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery("SELECT id FROM eb_store_product_attr_value WHERE product_id="+productId+" AND type=0 AND is_del=0 ORDER BY id")) {
                    while(rows.next())skuIds.add(rows.getInt(1));
                }
                if(skuIds.size()!=(n<12?1:2))throw new IllegalStateException("Unexpected SKU count for "+productId);
                for(int i=0;i<skuIds.size();i++) {
                    BigDecimal price=BigDecimal.valueOf(PRICES[n][i]).setScale(2);
                    sku.setBigDecimal(1,price);sku.setBigDecimal(2,price);sku.setInt(3,skuIds.get(i));sku.setBigDecimal(4,price);sku.setBigDecimal(5,price);
                    changed+=sku.executeUpdate();
                    // Resolve the JSON position by SKU id, preserving order and every other snapshot field.
                    int position;
                    try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery("SELECT j.position FROM eb_store_product_attr_result r JOIN JSON_TABLE(r.result,'$[*]' COLUMNS(position FOR ORDINALITY,sku_id INT PATH '$.id')) j WHERE r.product_id="+productId+" AND r.type=0 AND j.sku_id="+skuIds.get(i))) {
                        if(!rows.next())throw new IllegalStateException("Missing SKU snapshot");position=rows.getInt(1)-1;
                    }
                    String pricePath="$["+position+"].price",otPath="$["+position+"].otPrice";
                    snapshot.setString(1,pricePath);snapshot.setBigDecimal(2,price);snapshot.setString(3,otPath);snapshot.setBigDecimal(4,price);snapshot.setInt(5,productId);
                    snapshot.setString(6,pricePath);snapshot.setBigDecimal(7,price);snapshot.setString(8,otPath);snapshot.setBigDecimal(9,price);
                    changed+=snapshot.executeUpdate();
                }
                BigDecimal base=BigDecimal.valueOf(PRICES[n][0]).setScale(2);
                for(int i=1;i<=3;i++)product.setBigDecimal(i,base);product.setInt(4,productId);
                for(int i=5;i<=7;i++)product.setBigDecimal(i,base);
                changed+=product.executeUpdate();
            }
        }
        return changed;
    }
    static void verify(Connection connection) throws SQLException {
        LocalOperationsData.require(connection,"SELECT COUNT(*) FROM eb_store_product p WHERE p.id BETWEEN 900001 AND 900036 AND (p.price<>(SELECT MIN(v.price) FROM eb_store_product_attr_value v WHERE v.product_id=p.id AND v.type=0 AND v.is_del=0) OR p.vip_price<>p.price OR p.ot_price<>p.price)",0);
        LocalOperationsData.require(connection,"SELECT COUNT(*) FROM eb_store_product_attr_result r JOIN JSON_TABLE(r.result,'$[*]' COLUMNS(sku_id INT PATH '$.id',price DECIMAL(10,2) PATH '$.price',ot_price DECIMAL(10,2) PATH '$.otPrice')) j JOIN eb_store_product_attr_value v ON v.id=j.sku_id WHERE r.product_id BETWEEN 900001 AND 900036 AND r.type=0 AND (j.price<>v.price OR j.ot_price<>v.ot_price)",0);
        for(int n=0;n<PRICES.length;n++) LocalOperationsData.require(connection,"SELECT COUNT(*) FROM eb_store_product WHERE id="+(900001+n)+" AND price="+PRICES[n][0],1);
        LocalFlowerData.query(connection,"SELECT COUNT(*) AS products,MIN(price) AS min_standard_price,MAX(price) AS max_standard_price FROM eb_store_product WHERE id BETWEEN 900001 AND 900036");
        System.out.println("All 36 product prices match their SKU and JSON snapshots.");
    }
}
