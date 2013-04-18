<#-- see org.archive.crawler.reporting.FrontierSummaryReport -->
<#-- originally implemented by WorkQueueFrontier.reportTo() -->
<#macro appendQueueReports label queues size max=frontier.maxQueuesPerReportCategory>
<#list queues as wq>
<#if (max > 0 && wq_index >= max)>
...and ${size - wq_index} more ${label}.
<#break>
</#if>
${label}#${wq_index}:
<#-- WorkQueue.reportTo() -->
Queue ${wq.queueName} (p${wq.precedence})
  ${wq.itemCount} items
<#if wq.wakeTime??>
  wakes in: ${archiveUtils.formatMillisecondsToConventional(wq.wakeTime - currentTimeMillis())}
</#if>
    last enqueued: ${wq.lastQueued!'-'}
      last peeked: ${wq.lastPeeked!'-'}
   total expended: ${wq.totalExpenditure} (total budget: ${wq.totalBudget})
   active balance: ${wq.sessionBalance}
   last(avg) cost: ${wq.lastCost} (${archiveUtils.doubleToString(wq.averageCost, 1)})
   <#-- substats.shortReportLegend() -->
   totalScheduled fetchSuccesses fetchFailures fetchDisregards fetchResponses robotsDenials successBytes totalBytes<#rt>
   fetchNonResponses lastSuccessTime<#lt>
   <#-- substats.shortReportLine() -->
   <#assign ss=wq.substats>
   ${ss.totalScheduled} ${ss.fetchSuccesses} ${ss.fetchFailures} ${ss.fetchDisregards} ${ss.fetchResponses} <#rt>
   ${ss.robotsDenials} ${ss.successBytes} ${ss.totalBytes} ${ss.fetchNonResponses} <#t>
   ${archiveUtils.getLog17Date(ss.lastSuccessTime)}<#lt>
   <#assign pp=wq.precedenceProvider>
   ${pp.className}
   ${pp.precedence}

</#list>
</#macro>
<#if !frontier.running>
frontier unstarted
<#else>
Frontier report - ${archiveUtils.get12DigitDate()}
 Job being crawled: ${metadata.jobName}

 -----===== STATS =====-----
 Discovered:    ${frontier.discoveredUriCount!'-'}
 Queued:        ${frontier.queuedUriCount!'-'}
 Finished:      ${frontier.finishedUriCount!'-'}
  Successfully: ${succeededFetchCount!'-'}
  Failed:       ${failedFetchCount!'-'}
  Disregarded:  ${disregardedCount!'-'}

 -----===== QUEUES =====-----
 Already included size:     ${frontier.uriUniqFilter.count()}
               pending:     ${frontier.uriUniqFilter.pending()}

 All class queues map size: ${frontier.totalQueues}
             Active queues: ${frontier.activeQueues}
                In-process: ${frontier.inProcessQueues}
                     Ready: ${frontier.readyQueues}
                   Snoozed: ${frontier.snoozedQueues}
           Inactive queues: ${frontier.inactiveQueues} (<#rt>
<#-- dump inactive queues by precedence -->
<#assign inactives=frontier.inactiveQueuesDataByPrecedence>
<#list inactives?keys as k>
p${k}: ${inactives[k].size()}${k_has_next?string(";","")}<#rt>
</#list>
)
            Retired queues: ${frontier.retiredQueues}
          Exhausted queues: ${frontier.exhaustedQueues}

             Last state: ${frontier.lastReachedState}

 -----===== MANAGER THREAD =====-----
<#-- ToeThread.reportThread(managerThread, writer); -->
<#assign th=frontier.managerThreadData>
Java Thread State: ${th.threadState}
Blocked/Waiting On: <#rt>
<#if (th.lockOwnerId >= 0)>${th.lockName} which is owner by ${th.lockOwnerName} (${th.lockOnwerId})<#else>NONE</#if>
<#list th.stackTrace as ste>
    ${ste}
</#list>
 -----===== ${frontier.largestQueues} LONGEST QUEUES =====-----
<#-- appendQueueReports(writer, "LONGEST", largestQueues.getEntriesDescending().iterator(), largestQueues.size(), largestQueues.size()); -->
<@appendQueueReports label="LONGEST" queues=frontier.largestQueuesData size=frontier.largestQueues max=0 />
 -----===== IN-PROCESS QUEUES =====-----
<#--
        Collection<WorkQueue> inProcess = inProcessQueues;
        ArrayList<WorkQueue> copy = extractSome(inProcess, maxQueuesPerReportCategory);
        appendQueueReports(writer, "IN-PROCESS", copy.iterator(), copy.size(), maxQueuesPerReportCategory);
-->
<@appendQueueReports label="IN-PROCESS" queues=frontier.inProcessQueuesData size=frontier.inProcessQueues />
 -----===== READY QUEUES =====-----
<#--         appendQueueReports(writer, "READY", this.readyClassQueues.iterator(),
            this.readyClassQueues.size(), maxQueuesPerReportCategory);
-->
<@appendQueueReports label="READY" queues=frontier.readyQueuesData size=frontier.readyQueues />
 -----===== SNOOZED QUEUES =====-----
<#--
        Object[] objs = snoozedClassQueues.toArray();
        DelayedWorkQueue[] qs = Arrays.copyOf(objs,objs.length,DelayedWorkQueue[].class);
        Arrays.sort(qs);
        appendQueueReports(writer, "SNOOZED", new ObjectArrayIterator(qs), getSnoozedCount(), maxQueuesPerReportCategory);
-->
<#-- TODO: sorting -->
<@appendQueueReports label="SNOOZED" queues=frontier.snoozedQueuesData size=frontier.snoozedQueues />
 -----===== INACTIVE QUEUES =====-----
<#--
        SortedMap<Integer,Queue<String>> sortedInactives = getInactiveQueuesByPrecedence();
        for(Integer prec : sortedInactives.keySet()) {
            Queue<String> inactiveQueues = sortedInactives.get(prec);
            appendQueueReports(writer, "INACTIVE-p"+prec, inactiveQueues.iterator(),
                    inactiveQueues.size(), maxQueuesPerReportCategory);
        }
-->
<#list inactives?keys as prec>
<@appendQueueReports label="INACTIVE-p${prec}" queues=inactives[prec].iterator() size=inactives[prec].size() />
</#list>
 -----===== RETIRED QUEUES =====-----
<#--         appendQueueReports(writer, "RETIRED", getRetiredQueues().iterator(),
            getRetiredQueues().size(), maxQueuesPerReportCategory);
-->
<@appendQueueReports label="RETIRED" queues=frontier.retiredQueuesData size=frontier.retiredQueues />
</#if>