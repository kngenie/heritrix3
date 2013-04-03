<#if toePool??>
Toe threads report - ${archiveUtils.get12DigitDate()}
 Job being crawled: ${metadata.jobName}
 Number of toe threads in pool: ${toePool.toeCount} (${toePool.activeToeCount} active)
<#-- ToePool.getToes() should return stable copy of array -->
<#list toePool.toeThreads as toe>
[${toe.name}
<#if toe.currentURI??>
 ${toe.uriClassName?split(".")?last} ${toe.currentURI} ${toe.pathFromSeed} ${toe.via}<#rt>
    ${toe.fetchAttempts} attempts
    in processor: ${toe.currentProcessor}
<#else>
 -no CrawlURI- 
</#if>
    ${toe.status} for ${archiveUtils.formatMillisecondsToConventional(toe.currentStatusElapsedMilliseconds)}
    step: ${toe.step} for ${archiveUtils.formatMillisecondsToConventional(toe.stepElapsedMilliseconds)}
<#assign info=toe.threadInfo>
Java Thread State: ${info.threadState}
<#if (info.lockOwnerId >= 0)>
Blocked/Waiting On: ${info.lockName} which is owned by ${info.lockOwnerName}(${info.lockOwnerId})
<#else>
Blocked/Waiting On: NONE
</#if>
<#list toe.stackTrace as ste>
    ${ste}
</#list>
]
</#list>
<#else>
no ToeThreads
</#if>
