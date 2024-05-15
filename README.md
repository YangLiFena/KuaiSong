###### 技术栈

- Springboot 
- SSM 
- Redis
- MySQL
- Nginx
- JWT

###### 系统功能

- web端

  - 菜品管理

  - 套餐管理
  - 订单管理

  - 店铺营业状态设置
  - 员工管理

  - 用户登录/退出

- 微信小程序端

  - 登录功能
  - 菜品展示
  - 购物车管理
  - 用户下单
  - 地址簿管理

- application-dev.yml----修改数据库/Redis账号密码
- 通过ThreadLocal技术，记录存取用户id（借助jwt令牌以及拦截器）
- 通过pagehelper插件辅助分页
- 通过扩展SpringMVC的消息转换器，统一对日期类型进行格式化处理（一劳永逸）（或者使用@JsonFormat注解的方法--数据量大时比较麻烦）
- 使用阿里云OSS存储菜品与套餐的图片信息
- 使用Redis存储店铺营业状态字段
  + Redis服务启动(cmd)：redis-server.exe redis.windows.conf
  + Redis客户端连接(cmd)：redis-cli.exe -h ip地址 -p 端口号 -a password
