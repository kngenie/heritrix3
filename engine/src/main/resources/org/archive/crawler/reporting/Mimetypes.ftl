[#urls] [#bytes] [mime-types]
<#assign fd=stats.sortByCount(stats.fileDistribution)>
<#list fd as entry>
${entry.key?abs?c} ${stats.getBytesPerFileType(entry.value)?c} ${entry.value}
</#list>