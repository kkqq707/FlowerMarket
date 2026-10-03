package com.zbkj.service.service.impl;

import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.request.flower.FlowerProductSearchRequest;
import com.zbkj.common.response.flower.FlowerProductCardResponse;
import com.zbkj.service.service.FlowerProductService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

public class FlowerProductExportTest {
    @Test
    public void workbookUsesTheSameFiltersAndDisplayedProductValues() throws Exception {
        FlowerProductSearchRequest request = new FlowerProductSearchRequest();
        request.setType(2);
        request.setCid(9101);
        request.setRecipient("爱人");
        request.setColorSeries("奶油色");
        request.setPage(3);
        request.setLimit(20);
        FlowerProductCardResponse product = new FlowerProductCardResponse();
        product.setId(12);
        product.setStoreName("测试花礼");
        product.setPrice(new BigDecimal("188.00"));
        product.setStock(99);
        product.setSales(100);
        product.setFlowerMaterials("玫瑰");
        product.setOccasion("生日");
        product.setRecipient("爱人");
        product.setColorSeries("奶油色");
        FlowerProductService source = (FlowerProductService) Proxy.newProxyInstance(FlowerProductService.class.getClassLoader(),
                new Class[]{FlowerProductService.class}, (instance, method, args) -> {
                    Assert.assertEquals("searchAdmin", method.getName());
                    FlowerProductSearchRequest criteria = (FlowerProductSearchRequest) args[0];
                    Assert.assertEquals(request.getType(), criteria.getType());
                    Assert.assertEquals(request.getCid(), criteria.getCid());
                    Assert.assertEquals(request.getRecipient(), criteria.getRecipient());
                    Assert.assertEquals(request.getColorSeries(), criteria.getColorSeries());
                    Assert.assertEquals(1, criteria.getPage());
                    return Collections.singletonList(product);
                });
        ExcelServiceImpl service = new ExcelServiceImpl();
        Field sourceField = ExcelServiceImpl.class.getDeclaredField("flowerProductService");
        sourceField.setAccessible(true);
        sourceField.set(service, source);
        Path root = Files.createTempDirectory("flower-export-test-");
        CrmebConfig config = new CrmebConfig();
        config.setImagePath(root.toString() + "/");
        Field configField = ExcelServiceImpl.class.getDeclaredField("crmebConfig");
        configField.setAccessible(true);
        configField.set(service, config);
        String output = service.exportFlowerProduct(request);
        Assert.assertTrue(output.endsWith(".xlsx"));
        Assert.assertEquals(3, request.getPage());
        Assert.assertEquals(20, request.getLimit());
        Path workbookPath;
        try (Stream<Path> paths = Files.walk(root)) {
            workbookPath = paths.filter(path -> path.toString().endsWith(".xlsx")).findFirst().get();
        }
        try (Workbook workbook = WorkbookFactory.create(workbookPath.toFile())) {
            Assert.assertEquals("商品名称", workbook.getSheetAt(0).getRow(2).getCell(1).getStringCellValue());
            Assert.assertEquals("测试花礼", workbook.getSheetAt(0).getRow(3).getCell(1).getStringCellValue());
            Assert.assertEquals(188, workbook.getSheetAt(0).getRow(3).getCell(2).getNumericCellValue(), 0.001);
            Assert.assertEquals(99, workbook.getSheetAt(0).getRow(3).getCell(3).getNumericCellValue(), 0.001);
            Assert.assertEquals(100, workbook.getSheetAt(0).getRow(3).getCell(4).getNumericCellValue(), 0.001);
        }
    }
}
