## 使用了线程安全的并发工具，并不代表解决了所有线程安全问题
> @see package concurrenthashmapmisuse 

ConcurrentHashMap 只能保证提供的原子性读写操作是线程安全的。需要注意 ConcurrentHashMap 对外提供的方法或能力的限制：
- 使用了 ConcurrentHashMap，不代表对它的多个操作之间的状态是一致的，是没有其他线程在操作它的，如果需要确保需要手动加锁。
- 诸如 size、isEmpty 和 containsValue 等聚合方法，在并发情况下可能会反映 ConcurrentHashMap 的中间状态。因此在并发情况下，这些方法的返回值只能用作参考，而不能用于流程控制。显然，利用 size 方法计算差异值，是一个流程控制。
- 诸如 putAll 这样的聚合方法也不能确保原子性，在 putAll 的过程中去获取数据可能会获取到部分数据。

## 没有充分了解并发工具的特性，从而无法发挥其威力
> @see package concurrenthashmapperformance