# 事务

## 日志事务

日志事务确保一个事务内的所有日志，要么都写入成功，要么都写入失败。当事务成功提交后，这些日志将提交给状态机执行，如果事务未提交或者回滚，所有日志都不会被状态机执行。

### 对使用者提供的API

日志事务提供的API如下：

#### 开启事务

开启一个新事务，并返回事务ID。

该请求无参数。

返回 | 描述
-- | --
transactionId | 事务ID

#### 写入事务日志

与写入普通日志相同，需要带上事务ID。

#### 结束事务

结束事务，可能是提交或者回滚事务。

参数 | 描述
-- | --
operation | 操作：提交或回滚。
transactionId | 事务ID

无返回值。

#### 查询进行中的事务

查询进行中的事务。

该请求无参数。

返回 | 描述
-- | --
transactionIds[] | 进行中的事务ID集合

### 实现设计

将分区号30000以上的分区都作为保留分区。使用30000-30127共128个保留分区作为事务分区，用于记录事务日志。事务的实现算法基于二阶段提交算法。

#### 如何生成事务ID？

使用Java的UUID。

#### 开启事务

1. 选择一个空闲的事务分区，如果所有事务分区都被占用了，先清理过期事务，然后再选择一个空闲的事务分区返回；如果清理后还没有空闲事务分区，则返回错误；
2. 获取一个事务ID；
3. 在分区中写入TRANSACTION_START日志，日志包含的信息：事务开启时间和事务ID。
4. 返回事务ID，事务开启成功；

#### 写入事务日志

1. 检查事务状态：日志写入前需要检查根据事务ID检查事务状态，只有事务处于进行中才允许写入；
2. 日志的写入的实现与普通日志相同，日志中的分区号为事务分区，日志内容中记录日志的原始分区；

#### 结束事务

1. 检查事务状态；
2. 写入一条事务日志：TRANSACTION_PRE_COMPLETE，返回。只要TRANSACTION_PRE_COMPLETE日志写入成功，该事务一定会结束。

执行状态机阶段，针对“TRANSACTION_PRE_COMPLETE”的日志，需要执行如下完成事务逻辑：

1. 如果操作是回滚，直接写入TRANSACTION_COMPLETE日志；
2. 如果操作是提交：
3. 这个事务中所有消息涉及到的每个分区：读出该分区的所有事务日志，按顺序写入对应的对应分区。
4. 上步骤中每条日志都写入成功后，写入TRANSACTION_COMPLETE日志，提交成功。

上述结束事务的操作需要异步执行，考虑到执行状态机阶段，有可能会执行失败（比如，集群当前正好不可用）。因此，增加一个重试和补偿的机制来解决这个问题。

在Leader启动阶段，需要检查所有事务分区，对于最后一条已提交的事务日志为“TRANSACTION_PRE_COMPLETE”的日志，执重试完成事务逻辑；

在执行完成事务逻辑过程中，如果发生异常执行失败，则加入到延时重试队列中，反复重试直到执行成功。

#### 查询进行中的事务

遍历所有事务分区，如果分区中有日志，并且最后一条日志不是TRANSACTION_COMPLETE日志，则为进行中的事务。返回所有进行中的事务。





### 事务日志类型

类型 | 类型值 | 内容| 说明
-- | -- | -- | --
TRANSACTION_START | 0 | 事务ID | 开启事务
TRANSACTION_ENTRY | 1 | 事务ID、分区号、批量大小、序列化后的日志 | 事务日志
TRANSACTION_PRE_COMPLETE | 2 | 事务ID、操作（提交或回滚） | 预提交
TRANSACTION_COMPLETE | 3 | 事务ID、操作（提交或回滚） | 事务结束

### 事务分区号

分区号30000（含）以上为保留分区。[30000-30128)为128个事务日志分区。

分区 | 分区号 | 说明
-- | -- | -- 
TRANSACTION_PARTITION_N | 30000 + N | 事务日志分区， 30000 <= N < 30128
