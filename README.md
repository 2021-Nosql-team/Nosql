# Nosql
### 小组成员
毛恒明 李昊阳 刘志成 任洲弘 石涵舟 熊师意
### 用户画像电影画像的构建
1. 储存用户画像电影画像部分的代码在service文件夹里，其中电影画像的代码是MovieProfileMaker，用户画像是getData和savemessage文件。用户画像和电影画像均储存在redis里。
2. 我们还从imdb网站上爬取了更多有关电影的信息，存储在mongodb里。

### 召回
召回部分我们使用了四种方式召回
1. icf
2. ucf
3. 矩阵分解
4. 基于tag的召回（在service文件夹里，tagsToTags文件）

### 打分排序模型
代码见model文件夹

### 推荐系统
系统的服务器和客户端代码分别是helloworld文件夹中的helloworldClient和helloworldServer文件。
