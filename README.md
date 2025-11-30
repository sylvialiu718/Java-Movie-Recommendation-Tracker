# Movie Recommendation System

电影推荐系统 - CPT111 课程作业

## 项目结构

```
cw/
├── src/                        # 源代码目录
│   ├── MovieSystem.java        # CLI 命令行界面
│   ├── MovieSystemSwingGUI.java # GUI 图形界面 (Swing)
│   └── MovieSystemGUI.java     # GUI 图形界面 (JavaFX, 需要额外配置)
├── bin/                        # 编译后的 class 文件
├── data/                       # 数据文件
│   ├── movies.csv              # 电影数据
│   └── users.csv               # 用户数据
└── javafx-sdk-25.0.1/          # JavaFX SDK (可选)
```

## 编译和运行

### CLI 命令行版本
```bash
# 编译
javac -encoding UTF-8 -d bin src/MovieSystem.java

# 运行
java -cp bin MovieSystem
```

### GUI 图形界面版本 (Swing - 推荐)
```bash
# 编译
javac -encoding UTF-8 -d bin src/MovieSystemSwingGUI.java

# 运行
java -cp bin MovieSystemSwingGUI
```

### GUI 图形界面版本 (JavaFX)
```bash
# 需要配置 JavaFX SDK 路径
javac --module-path javafx-sdk-25.0.1/lib --add-modules javafx.controls -d bin src/MovieSystemGUI.java
java --module-path javafx-sdk-25.0.1/lib --add-modules javafx.controls -cp bin MovieSystemGUI
```

## 功能说明

### 用户功能
- **注册/登录** - 用户认证系统
- **浏览电影** - 查看所有电影、分页浏览、排序
- **搜索电影** - 按标题搜索
- **按类型浏览** - 选择电影类型筛选
- **个性化推荐** - 基于观看历史推荐
- **观看列表** - 管理想看的电影
- **观看历史** - 记录已看的电影

### 推荐系统
- 基于用户观看历史分析喜好类型
- 按评分推荐同类型电影
- 按年份推荐电影
- 显示 Top 评分电影

## 数据格式

### movies.csv
```
ID,Title,Genre,Year,Rating
M001,The Shawshank Redemption,Drama,1994,9.3
```

### users.csv
```
Username,Password,Watchlist,History
user1,encoded_password,M001;M002,M003;M004
```
