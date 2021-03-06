/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.journalkeeper.core.api.transaction;

import io.journalkeeper.core.api.RaftJournal;
import io.journalkeeper.core.api.UpdateRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author LiYue
 * Date: 2019/10/22
 *
 * 日志事务确保一个事务内的所有日志，要么都写入成功，要么都写入失败。
 * 当事务成功提交后，这些日志将提交给状态机执行，如果事务未提交或者回滚，所有日志都不会被状态机执行。
 */
public interface TransactionalJournalStore {

    /**
     * 开启一个新事务，并返回事务ID。
     * @return 事务ID
     */
    default CompletableFuture<TransactionContext> createTransaction() {
        return createTransaction(Collections.emptyMap());
    }

    /**
     * 开启一个新事务，并返回事务ID。
     * @param context 事务上下文
     * @return 事务ID
     */
    CompletableFuture<TransactionContext> createTransaction(Map<String, String> context);

    /**
     * 结束事务，可能是提交或者回滚事务。
     * @param transactionId 事务ID
     * @param commitOrAbort true：提交事务，false：回滚事务。
     * @return 执行成功返回null，失败抛出异常。
     */
    CompletableFuture<Void> completeTransaction(TransactionId transactionId, boolean commitOrAbort);

    /**
     * 查询进行中的事务。
     * @return 进行中的事务ID列表。
     */
    CompletableFuture<Collection<TransactionContext>> getOpeningTransactions();


    /**
     * 写入事务日志，分区为默认分区（0），批量大小为1，entry中不包含Header
     * @param transactionId 事务ID
     * @param entry 操作日志
     * @return 执行成功返回null，失败抛出异常。
     */
    default CompletableFuture<Void> append(TransactionId transactionId, byte[] entry) {
        return append(transactionId, entry, RaftJournal.DEFAULT_PARTITION, 1, false);
    }

    /**
     * 写入事务日志，entry中不包含Header
     * @param transactionId 事务ID
     * @param entry 操作日志
     * @param partition 分区
     * @param batchSize 批量大小
     * @return 执行成功返回null，失败抛出异常。
     */
    default CompletableFuture<Void> append(TransactionId transactionId, byte[] entry, int partition, int batchSize) {
        return append(transactionId, entry, partition, batchSize, false);
    }

    /**
     * 写入事务日志。
     * @param transactionId 事务ID
     * @param entry 操作日志数组
     * @param partition 分区
     * @param batchSize 批量大小
     * @param includeHeader entry中是否包含Header
     * @return 执行成功返回null，失败抛出异常。
     */
    default CompletableFuture<Void> append(TransactionId transactionId, byte[] entry, int partition, int batchSize, boolean includeHeader) {
        return append(transactionId, new UpdateRequest(entry, partition, batchSize), includeHeader);
    }


    /**
     * 写入事务操作日志变更状态。集群保证按照提供的顺序写入，保证原子性，服务是线性的，任一时间只能有一个update操作被执行。
     * 日志在集群中复制到大多数节点，并在状态机执行后返回。
     * @param transactionId 事务ID
     * @param updateRequest See {@link UpdateRequest}
     * @param includeHeader entry中是否包含Header
     * @return 执行成功返回null，失败抛出异常。
     */
    CompletableFuture<Void> append(TransactionId transactionId, UpdateRequest updateRequest, boolean includeHeader);

    /**
     * 写入事务操作日志变更状态。集群保证按照提供的顺序写入，保证原子性，服务是线性的，任一时间只能有一个update操作被执行。
     * 日志在集群中复制到大多数节点，并在状态机执行后返回。
     * 此方法等效于：append(transactionId, updateRequest, false, responseConfig);
     * @param transactionId 事务ID
     * @param updateRequest See {@link UpdateRequest}
     * @return 执行成功返回null，失败抛出异常。
     */
    default CompletableFuture<Void> append(TransactionId transactionId, UpdateRequest updateRequest) {
        return append(transactionId, updateRequest, false);
    }

    /**
     * 写入事务操作日志变更状态。集群保证按照提供的顺序写入，保证原子性，服务是线性的，任一时间只能有一个update操作被执行。
     * 日志在集群中复制到大多数节点，并在状态机执行后返回。
     * 此方法等效于：append(transactionId, updateRequests, false, responseConfig);
     * @param transactionId 事务ID
     * @param updateRequests See {@link UpdateRequest}
     * @return 执行成功返回null，失败抛出异常。
     */
    default CompletableFuture<Void> append(TransactionId transactionId, List<UpdateRequest> updateRequests) {
        return append(transactionId, updateRequests, false);
    }

    /**
     * 写入事务操作日志变更状态。集群保证按照提供的顺序写入，保证原子性，服务是线性的，任一时间只能有一个update操作被执行。
     * 日志在集群中复制到大多数节点，并在状态机执行后返回。
     * @param transactionId 事务ID
     * @param updateRequests See {@link UpdateRequest}
     * @param includeHeader entry中是否包含Header
     * @return 执行成功返回null，失败抛出异常。
     */
    CompletableFuture<Void> append(TransactionId transactionId, List<UpdateRequest> updateRequests, boolean includeHeader);

}
