<#-- TODO: fallback support for non-WorkQueueFrontier implementations -->
<#macro queueSingleLines queues>
<#-- write queue.shortReportLegend() -->
queue precedence currentSize totalEnqueues sessionBalance lastCost (averageCost) <#rt>
lastDequeueTime wakeTime totalSpend/totalBudget errorCount lastPeekUri lastQueueUri
<#list queues as wq>
<#-- call queue.shortReportLineTo(writer) -->
<#-- classKey: queueName -->
${wq.queueName} ${wq.precedence} ${wq.itemCount} ${wq.enqueueCount} ${wq.sessionBalance} <#rt>
${wq.lastCost} (${wq.averageCost}) <#rt>
<#-- (${archiveUtils.doubleToString(wq.totalExpenditure/wq.costCount), 1)}) <#rt> -->
<#if wq.lastDequeueTime??>${archiveUtils.getLog17Date(wq.lastDequeueTime)}<#else>-</#if> <#rt>
<#if wq.wakeTime??>${archiveUtils.formatMillisecondsToConventional(wq.wakeTime - currentTimeMillis())}<#else>-</#if> <#rt>
${wq.totalExpenditure} ${wq.totalBudget} ${wq.errorCount} ${wq.lastPeeked!'-'} ${wq.lastQueued!'-'}
</#list>
</#macro>
<#if !frontier??>
<#elseif !frontier.running>
frontier unstarted
<#elseif frontier.empty>
frontier empty
<#else>

 -----===== IN-PROCESS QUEUES =====-----
<#-- queueSingleLinesTo(writer, inProcessQueuesCopy.iterator()) -->
<#-- ArrayList<WorkQueue> -->
<@queueSingleLines frontier.inProcessQueuesDataAll />

 -----===== READY QUEUES =====-----
<#-- queueSingleLinesTo(writer, this.readyClassQueues.iterator()) -->
<#-- BlockingQueue<String> -->
<@queueSingleLines frontier.readyQueuesData![] />

 -----===== SNOOZED QUEUES =====-----
<#-- queueSingleLinesTo(writer, this.snoozedClassQueues.iterator()) -->
<#-- DelayedWorkQueue -->
<@queueSingleLines frontier.snoozedQueuesData![] />
<#-- queueSingleLinesTo(writer, this.snoozedOverflow.values().iterator()) -->
<#-- DelayedWorkQueue -->
<@queueSingleLines frontier.snoozedOverflowData![] />

 -----===== INACTIVE QUEUES =====-----
<#-- queueSingleLinesTo(writer, inactiveQueues.iterator()) for inactiveQueues in getInactiveQueuesByPrecedence().values()) -->
<@queueSingleLines frontier.inactiveQueuesData![] />

 -----===== RETIRED QUEUES =====-----
<#-- queueSingleLinesTo(writer, getRetiredQueues().iterator()) : Queue<String> -->
<@queueSingleLines frontier.retiredQueuesData![] />
</#if>
