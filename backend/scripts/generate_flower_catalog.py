"""Generate append-only flower catalog SQL. Does not create customers, orders or reviews."""
import json
from pathlib import Path

GOODS = [
    ('轻语 · 9枝粉玫瑰',4,138,9101,9,'粉玫瑰、尤加利叶','粉色','日常','朋友,自己','把温柔放进日常'),
    ('朝夕 · 香槟玫瑰',3,298,9101,19,'香槟玫瑰、洋桔梗、尤加利叶','香槟色','纪念日','爱人','朝夕相伴，温柔长久'),
    ('热恋 · 33枝红玫瑰',7,528,9101,33,'红玫瑰、尤加利叶','红色','纪念日','爱人','热烈的心意，认真交给你'),
    ('心动 · 11枝粉玫瑰',4,198,9101,11,'粉玫瑰、满天星、配叶','粉色','生日','爱人,朋友','愿你每一天都有小小心动'),
    ('白昼 · 白色百合',9,188,9102,6,'白百合、白玫瑰、尤加利叶','白绿色','日常','家人,长辈','一束清香，一份陪伴'),
    ('晴雪 · 百合花束',9,258,9102,9,'白百合、洋桔梗、配叶','白色','生日','家人,长辈','把纯净的祝福送给你'),
    ('春信 · 郁金香',6,168,9103,10,'郁金香、配叶','奶油色','日常','朋友,自己','春日来信，温柔可期'),
    ('柔光 · 郁金香',6,298,9103,20,'郁金香、洋甘菊、配叶','奶油色','生日','爱人,朋友','愿你的生活常有柔光'),
    ('追光 · 向日葵',2,128,9104,5,'向日葵、尤加利叶','黄色','日常','朋友,同事','一路向阳，追光而行'),
    ('灿烂 · 向日葵',2,228,9104,12,'向日葵、香槟配花、尤加利叶','黄色','祝贺','朋友,同事','愿前路明亮，笑容灿烂'),
    ('微风 · 白绿洋桔梗',1,158,9105,12,'白色洋桔梗、绿色配叶','白绿色','日常','朋友,自己','微风轻轻，心意满满'),
    ('星露 · 洋桔梗',1,228,9105,20,'白色洋桔梗、白玫瑰、配叶','白绿色','生日','朋友,家人','星光与晨露，都是祝福'),
    ('秋日信笺 · 暖色花束',10,238,9106,18,'橙玫瑰、太阳花、配叶','橙色','日常','朋友,自己','把暖意写成一束花'),
    ('小确幸 · 缤纷花束',5,198,9106,15,'玫瑰、乒乓菊、洋桔梗、配叶','缤纷','生日','朋友,家人','日常的小欢喜，都值得庆祝'),
    ('晚霞 · 暖色花束',10,338,9106,28,'橙玫瑰、太阳花、洋桔梗、配叶','橙色','纪念日','爱人','晚霞有约，心意如初'),
    ('花田 · 生日混搭',5,368,9106,32,'玫瑰、乒乓菊、洋桔梗、配叶','缤纷','生日','朋友,家人','愿你的新一岁繁花相伴'),
    ('紫序 · 蝴蝶兰花礼',8,428,9107,20,'紫色蝴蝶兰、紫玫瑰、配花','紫色','纪念日','爱人,长辈','把浪漫与祝福珍藏'),
    ('珍藏 · 粉玫瑰礼盒',4,328,9107,19,'粉玫瑰、粉色洋桔梗、满天星','粉色','生日','爱人,朋友','把心意装进一盒温柔'),
    ('温柔 · 香槟玫瑰礼盒',3,368,9107,21,'香槟玫瑰、洋桔梗、尤加利叶','香槟色','纪念日','爱人,家人','愿长久的陪伴始终温柔'),
    ('远山 · 蓝白花篮',11,398,9108,24,'蓝色绣球、白玫瑰、洋桔梗','蓝白色','祝贺','朋友,同事','心怀远山，前程明朗'),
    ('澄空 · 开业花篮',11,568,9108,36,'蓝色绣球、白玫瑰、洋桔梗、配叶','蓝白色','祝贺','客户,同事','新程启幕，好事发生'),
    ('红火 · 开业桌花',12,488,9109,28,'红掌、粉玫瑰、百合、配叶','红粉色','祝贺','客户,同事','红火开场，盛放新程'),
    ('一隅 · 暖橙桌花',10,228,9109,16,'橙玫瑰、太阳花、配叶','橙色','日常','朋友,自己','给生活留一隅暖色'),
    ('午后 · 洋桔梗瓶插花',1,128,9109,10,'白色洋桔梗、绿色配叶','白绿色','日常','自己,家人','午后有花，平凡也美好'),
]

def quote(value):
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace('\\', '\\\\').replace("'", "''") + "'"

def js(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))

statements = [
    '-- 花卉商品扩充：24 款商品、48 个 SKU、6 套规格模板。',
    '-- 仅新增缺失记录；不覆盖已有商品和库存，不生成评价、顾客、订单或销售历史。',
    '-- 通过 deliverables/LocalFlowerData.java 的 apply 模式事务执行。图片复用已有同系列素材。',
]

def insert(table, data, condition):
    columns = ','.join(f'`{k}`' for k in data)
    values = ','.join(quote(v) for v in data.values())
    statements.append(f'INSERT INTO `{table}` ({columns}) SELECT {values} WHERE NOT EXISTS (SELECT 1 FROM `{table}` WHERE {condition});')

templates = [
    ('花束大小','花束规格',['标准款','加量款']),
    ('玫瑰枝数','主花枝数',['9枝','11枝','19枝','33枝']),
    ('礼盒规格','礼盒大小',['标准礼盒','加大礼盒']),
    ('花篮规格','花篮大小',['标准花篮','大型花篮']),
    ('桌花规格','花器大小',['小型','中型','大型']),
    ('包装风格','包装风格',['简约雾面纸','韩式双层纸','礼盒包装']),
]
for i,(name,attr,details) in enumerate(templates,930001):
    insert('eb_store_product_rule',dict(id=i,rule_name=name,rule_value=js([dict(value=attr,detail=details)])),f'id={i}')

care = '收到后斜剪根部并去除水下叶片，置于阴凉通风处，每日清洁花瓶并换水。花泥花器缓慢少量补水。'
for n,(name,photo,price,category,stems,materials,color,occasion,recipient,language) in enumerate(GOODS,13):
    pid = 900000+n
    image = f'crmebimage/public/flower/product-{photo:02}.jpg'
    stocks = [22+(n%4)*4,12+(n%3)*3]
    unit = '盒' if category==9107 else '篮' if category==9108 else '盆' if category==9109 else '束'
    packaging = '鲜花礼盒' if category==9107 else '手提花篮' if category==9108 else '桌面花器' if category==9109 else '雾面纸手工包扎'
    product = dict(id=pid,mer_id=0,image=image,slider_image=js([image]),store_name=name,
        store_info=f'{materials}，{packaging}，可选标准款和加量款',keyword=f'{materials},{occasion},{color}',
        bar_code=f'FD{n:06}',cate_id=str(category),price=price,vip_price=price,ot_price=price+50,postage=0,
        unit_name=unit,sort=60-n,sales=0,stock=sum(stocks),is_show=1,is_hot=0,is_benefit=0,is_best=1,is_new=1,
        add_time=0,is_postage=1,is_del=0,cost=round(price*.52,2),is_good=1,ficti=0,browse=0,temp_id=1,
        spec_type=1,activity='',flat_pattern='',is_recycle=0,version=0,guarantee_ids='0')
    insert('eb_store_product',product,f'id={pid}')
    # Database time rather than a stale fixed generation date.
    keys=list(product)
    values=[quote(v) for v in product.values()]
    values[keys.index('add_time')]='UNIX_TIMESTAMP()'
    statements[-1]=f"INSERT INTO `eb_store_product` ({','.join(f'`{k}`' for k in keys)}) SELECT {','.join(values)} WHERE NOT EXISTS (SELECT 1 FROM `eb_store_product` WHERE id={pid});"
    insert('eb_store_product_attr',dict(id=950000+n,product_id=pid,attr_name='花束规格',attr_values='标准款,加量款',type=0,is_del=0,is_show_image=0),f'id={950000+n}')
    sku_metadata=[]
    for variant,label in enumerate(['标准款','加量款'],1):
        sid=960000+n*2+variant-1
        sku_price=price+(variant-1)*80
        unique=f'F{n:04}S{variant:02}'
        insert('eb_store_product_attr_option',dict(id=sid,product_id=pid,attr_id=950000+n,option_name=label,image='',sort=variant,is_del=0),f'id={sid}')
        row=dict(id=sid,product_id=pid,suk=label,stock=stocks[variant-1],sales=0,price=sku_price,image=image,
            unique=unique,cost=round(sku_price*.52,2),bar_code=unique,ot_price=sku_price+50,weight=0,volume=0,
            brokerage=0,brokerage_two=0,type=0,quota=0,quota_show=0,attr_value=js({'花束规格':label}),
            is_del=0,version=0,is_default=int(variant==1),is_show=1)
        insert('eb_store_product_attr_value',row,f'id={sid}')
        camel={'product_id':'productId','bar_code':'barCode','ot_price':'otPrice','brokerage_two':'brokerageTwo','quota_show':'quotaShow','attr_value':'attrValue','is_del':'isDel','is_default':'isDefault','is_show':'isShow'}
        sku_metadata.append({camel.get(k,k):v for k,v in row.items()})
    insert('eb_flower_product_ext',dict(product_id=pid,flower_materials=materials,flower_language=language,
        occasion=occasion,recipient=recipient,color_series=color,stem_count=stems,packaging=packaging,care_tips=care,batch_managed=0),f'product_id={pid}')
    description=f'<h3>{name}</h3><p>花材：{materials}。标准款主花约{stems}枝；加量款在标准款基础上增加约三分之一花量。</p><p>包装：{packaging}。图片为同系列花材参考图；不同规格花量、包装及花器以选择的规格说明为准。自然花材的开放度与枝形会有差异。</p><p>花语：{language}</p><p>养护：{care}</p>'
    insert('eb_store_product_description',dict(id=pid,product_id=pid,description=description,type=0),f'product_id={pid} AND type=0')
    insert('eb_store_product_attr_result',dict(id=970000+n,product_id=pid,result=js(sku_metadata),change_time=0,type=0),f'product_id={pid} AND type=0')
    statements[-1]=statements[-1].replace(',0,0 WHERE',',UNIX_TIMESTAMP(),0 WHERE')

# Rebuild missing edit metadata from the existing SKU rows, retaining their current prices and quantities.
statements.append("""INSERT INTO `eb_store_product_attr_result` (`id`,`product_id`,`result`,`change_time`,`type`)
SELECT 970000+p.id-900000,p.id,CAST(JSON_ARRAYAGG(JSON_OBJECT('id',v.id,'productId',v.product_id,'suk',v.suk,'stock',v.stock,'price',v.price,'image',v.image,'unique',v.`unique`,'cost',v.cost,'barCode',v.bar_code,'otPrice',v.ot_price,'attrValue',v.attr_value)) AS CHAR),UNIX_TIMESTAMP(),0
FROM eb_store_product p JOIN eb_store_product_attr_value v ON v.product_id=p.id AND v.type=0 AND v.is_del=0
WHERE p.id BETWEEN 900001 AND 900012 AND NOT EXISTS(SELECT 1 FROM eb_store_product_attr_result r WHERE r.product_id=p.id AND r.type=0)
GROUP BY p.id;""")
output = Path(__file__).resolve().parents[1] / 'sql' / 'flower-catalog-expansion.sql'
output.write_text('\n\n'.join(statements)+'\n',encoding='utf-8')
print(f'Generated {len(statements)-3} insert statements: {output.name}')
