# CEPAS 读卡器核心

此文件夹包含处理读取 CEPAS (非接触式电子钱包应用标准) 卡片过程的基本文件。这些文件协同工作，为基于 NFC 的 CEPAS 读卡和 CAN ID 提取提供完整的解决方案。

## 概述

CEPAS 卡是用于非接触式支付和交通系统的智能卡，尤其在新加坡广泛使用。此实现提供了通过 NFC 读取 CEPAS 卡并提取包括 CAN (卡账户号码) ID 在内的特定数据的核心功能。

## 文件概述

### 1. **`CEPASTagReader.java`** - 主读取器类
**主要角色**: 协调整个 CEPAS 读卡过程

**主要职责**:
- 扩展 `TagReader<IsoDep, RawCEPASCard, CardKeys>`
- 创建一个 `CEPASProtocol` 实例来处理底层通信
- 从 CEPAS 卡中读取所有 16 个钱包 (0-15)
- 为每个有效钱包读取交易历史
- 返回一个包含所有数据的 `RawCEPASCard` 对象

**主要方法**: `readTag()` - 读取整张卡并返回结构化数据

### 2. **`CEPASProtocol.java`** - 底层通信
**主要角色**: 处理与 CEPAS 卡的底层 NFC 通信

**主要职责**:
- 发送 CEPAS 应用选择命令 (`00 A4 00 00 02 40 00`)
- 发送 READ PURSE (读取钱包) 命令 (`32 [purse_id] 00 00 01 00`)
- 处理 APDU 命令的封装和响应解析
- 管理错误处理和状态码
- 实现 CEPAS 协议规范

**主要方法**:
- `getPurse(int purseId)` - 从卡中读取指定的钱包
- `sendRequest()` - 向卡发送 APDU 命令
- `sendSelectFile()` - 选择 CEPAS 应用

### 3. **`CANScannerActivity.java`** - 用户界面与流程控制
**主要角色**: 提供 Android 用户界面并协调扫描过程

**主要职责**:
- 当卡片靠近读取器时处理 NFC 意图检测
- 创建 `CEPASTagReader` 实例
- 调用 `tagReader.readTag()` 来读取卡片
- 将原始卡数据解析为 `CEPASCard` 对象
- 特别提取钱包 3 的数据和 CAN ID 信息
- 向用户显示带有详细计算的结果

**主要方法**:
- `scanCard(Tag tag)` - 主要的扫描流程控制
- `extractAndConvertCAN(byte[] canBytes)` - CAN ID 处理
- `displayPurseInfo(CEPASPurse purse)` - 信息显示

## CEPAS 读卡流程

### 1. NFC 检测
- `CANScannerActivity` 检测到 CEPAS 卡靠近 NFC 读取器
- Android 系统发送一个包含标签信息的 NFC 意图

### 2. 读取器创建
- 使用标签 ID 和 NFC 标签对象创建一个 `CEPASTagReader`
- 初始化 ISO-DEP 技术进行通信

### 3. 协议通信
`CEPASTagReader` 使用 `CEPASProtocol` 来:
- **选择 CEPAS 应用**: 发送 `00 A4 00 00 02 40 00` 来选择 CEPAS 应用
- **读取所有钱包**: 使用 READ PURSE 命令遍历所有 16 个钱包 (0-15)
- **读取历史记录**: 对于有效的钱包，读取交易历史记录

### 4. 数据解析
- 来自卡片的原始二进制数据被解析为结构化的 `CEPASCard` 对象
- 每个钱包包含特定的数据字段 (余额、CAN ID、CSN 等)

### 5. 特定提取
- 应用专注于 **钱包 3** (通常包含主要的交通数据)
- 从钱包数据的字节 8-15 中提取 **CAN ID**
- 将 8 字节的 CAN ID 转换为 64 位整数

### 6. 显示结果
- 显示钱包信息 (余额、状态、日期等)
- 以十六进制格式显示 CAN 字节
- 显示计算出的 64 位卡序列号
- 提供分步计算详情

## CEPAS 卡结构

### 钱包数据布局 (64 字节)
```
字节位置 | 字段名称           | 大小 | 描述
--------------|---------------------|------|-------------
0             | CEPAS 版本          | 1    | 协议版本
1             | 钱包状态            | 1    | 当前状态
2-4           | 钱包余额            | 3    | 当前余额 (有符号)
5-7           | 自动加载金额        | 3    | 自动充值金额 (有符号)
8-15          | CAN (卡账户)        | 8    | **CAN ID - 8 字节**
16-23         | CSN (卡序列号)      | 8    | 卡序列号
24-25         | 钱包到期日期        | 2    | 到期日期 (从 1995 年起的天数)
26-27         | 钱包创建日期        | 2    | 创建日期 (从 1995 年起的天数)
28-31         | 最后信用 TRP        | 4    | 最后一次信用交易
32-39         | 信用头              | 8    | 信用交易头
40            | 日志文件记录数      | 1    | 交易记录数
41            | 发行方数据长度      | 1    | 发行方数据长度
42-45         | 最后交易 TRP        | 4    | 最后一次交易参考
46-61         | 最后交易            | 16   | 最后一次交易记录
62+           | 发行方特定数据      | var  | 可变长度发行方数据
```

## CAN ID 处理

### 提取过程
1. **位置**: 每个钱包数据结构的字节 8-15
2. **格式**: 原始 8 字节二进制数据
3. **目的**: 用于交通系统的唯一卡标识符

### 转换算法
```java
public static long byteArrayToLong(byte[] b, int offset, int length) {
    long value = 0;
    for (int i = 0; i < length; i++) {
        int shift = (length - 1 - i) * 8;
        value += (long) (b[i + offset] & 0x000000FF) << shift;
    }
    return value;
}
```

### 计算示例
对于 CAN 字节 `[0x1C, 0x61, 0xE9, 0x59, 0x00, 0x00, 0x00, 0x00]`:
```
字节 0: 0x1C (28) << 56 = 2017612633061982208
字节 1: 0x61 (97) << 48 = 273818857344155648
字节 2: 0xE9 (233) << 40 = 25670517876088832
字节 3: 0x59 (89) << 32 = 382252544
字节 4: 0x00 (0) << 24 = 0
字节 5: 0x00 (0) << 16 = 0
字节 6: 0x00 (0) << 8 = 0
字节 7: 0x00 (0) << 0 = 0

总计: 2045172274264276992
```

## 使用的 NFC 命令

### CEPAS 应用选择
```
命令: 00 A4 00 00 02 40 00
- CLA: 00 (ISO 7816-4)
- INS: A4 (SELECT)
- P1: 00 (按名称选择)
- P2: 00 (第一次或仅有一次出现)
- Lc: 02 (AID 长度)
- 数据: 40 00 (CEPAS 应用 ID)
```

### 读取钱包命令
```
命令: 32 [purse_id] 00 00 01 00
- CLA: 90 (CEPAS 特定)
- INS: 32 (READ PURSE)
- P1: [purse_id] (0-15)
- P2: 00 (读取钱包数据)
- Lc: 01 (数据长度)
- 数据: 00 (从头开始读取)
```

## 错误处理

### 常见错误场景
1. **未找到钱包 3**: 卡上没有有效的钱包 3
2. **CAN 数据问题**: CAN 字节为 null 或不是 8 字节
3. **NFC 通信错误**: 未正确检测到卡片
4. **权限被拒绝**: 卡需要身份验证

### 错误响应代码
- `0x90 0x00`: 操作成功
- `0x90 0x9D`: 权限被拒绝
- `0x6B`: 无效的文件引用
- `0x67`: 无效的文件大小

## 依赖项

### Android NFC 要求
- `android.nfc.Tag` - NFC 标签接口
- `android.nfc.tech.IsoDep` - ISO-DEP 技术
- AndroidManifest.xml 中的 NFC 权限

### 内部依赖
- `TagReader` 基类
- `RawCEPASCard`, `RawCEPASPurse`, `RawCEPASHistory` 数据类
- `CEPASException` 用于错误处理
- `ByteUtils` 用于数据转换

## 使用示例

```java
// 创建标签读取器
CEPASTagReader tagReader = new CEPASTagReader(tagId, tag);

// 读取原始 CEPAS 卡
RawCEPASCard rawCard = tagReader.readTag();

// 解析以获取 CEPAS 卡
CEPASCard cepasCard = rawCard.parse();

// 获取钱包 3 (主要交通数据)
CEPASPurse purse = cepasCard.getPurse(3);

if (purse != null && purse.isValid()) {
    // 提取 CAN ID
    byte[] canBytes = purse.getCAN().bytes();
    long cardSerial = ByteUtils.byteArrayToLong(canBytes);
    
    // 使用 CAN ID 进行交通系统识别
    System.out.println("Card Serial: " + cardSerial);
}
```

## 技术说明

### 为什么是钱包 3?
- **交通数据**: 包含主要的交通/运输数据
- **标准位置**: 大多数 CEPAS 卡使用钱包 3 来存储交通数据
- **一致性**: 在不同类型的卡之间提供可靠的数据
- **CAN ID**: 通常包含与交通系统最相关的 CAN ID

### 性能考虑
- 读取所有 16 个钱包需要时间，但能确保数据的完整性
- 交易历史读取是可选的，可以跳过
- 错误处理可防止在无效卡上发生崩溃

### 安全说明
- 此实现仅读取 CEPAS 卡的公共数据
- 不需要身份验证或加密密钥
- 所有读取的数据都可以通过 NFC 公开访问

## 许可证

此代码是 FareBot 项目的一部分，并根据 GNU 通用公共许可证 v3.0 进行许可。

## 贡献

在修改这些文件时:
1. 保持与现有 CEPAS 卡的向后兼容性
2. 遵循已建立的错误处理模式
3. 使用真实的 CEPAS 卡进行测试以确保可靠性
4. 记录任何协议更改或添加
