crawl name: ${metadata.jobName}
crawl status: ${crawlExitStatus.description}
duration: ${archiveUtils.formatMillisecondsToConventional(stats.crawlElapsedTime)}

seeds crawled: ${stats.seedsCrawled}
seeds uncrawled: ${stats.seedsTotal - stats.seedsCrawled}

hosts visited: ${stats.serverCache.hostKeys()?size - 1}
<#assign snapshot=stats.lastSnapshot>
URIs processed: ${snapshot.finishedUriCount}
URI successes: ${snapshot.downloadedUriCount}
URI failures: ${snapshot.downloadFailures}
URI disregards: ${snapshot.downloadDisregards}

novel URIs: ${stats.crawledBytes.novelCount}
<#if stats.crawledBytes.dupByHashCount??>
duplicate-by-hash URIs: ${stats.crawledBytes.dupByHashCount}
</#if>
<#if stats.crawledBytes.notModifiedCount??>
not-modified URIs: ${stats.crawledBytes.notModifiedCount}
</#if>

<#-- total bytes 'crawled' (which includes the size of
     refetched-but-unwritten-duplicates and reconsidered-but-not-modified -->
total crawled bytes: ${snapshot.bytesProcessed} (${archiveUtils.formatBytesForDisplay(snapshot.bytesProcessed)}) 
novel crawled bytes: ${stats.crawledBytes.novel} (${archiveUtils.formatBytesForDisplay(stats.crawledBytes.novel)})
<#if stats.crawledBytes.dupByHash??>
duplicate-by-hash crawled bytes: ${stats.crawledBytes.dupByHash} (${archiveUtils.formatBytesForDisplay(stats.crawledBytes.dupByHash)}) 
</#if>
<#if stats.crawledBytes.notModified??>
not-modified crawled bytes: ${stats.crawledBytes.notModified} (${archiveUtils.formatBytesForDisplay(stats.crawledBytes.notModified)}) 
</#if>

URIs/sec: ${snapshot.docsPerSecond?string["0.##"]}
KB/sec: ${snapshot.totalKiBPerSec}
