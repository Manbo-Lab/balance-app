# AMD 余额

一个查询 AMD Radeon API 每日额度的原生 Android 小工具。

查询 [developer.amd.com.cn](https://developer.amd.com.cn) 的 `/api/v1/usage` 接口，
展示今日剩余 / 已用 / 日限额 / 请求数，并按模型统计费用。

- 余额卡片 + 环形图
- 按模型费用分布
- 设置页填写 API Key（仅存本机 SharedPreferences，不打包进源码）

## 技术栈

- Kotlin + Android Gradle Plugin 8.2.2
- 传统 View + XML 布局
- 网络用 `HttpURLConnection` + `org.json`，零第三方依赖

## 构建

```sh
# 需要 JDK 17 + Android SDK（compileSdk 34）
./gradlew assembleRelease
```

APK 在 `app/build/outputs/apk/release/`。

## 说明

- API Key 首次打开时在「设置」页填写，密钥只保存在本机，不会进入仓库。
- 接口数据字段见 `ApiClient.kt`。

## License

[MIT](LICENSE)
