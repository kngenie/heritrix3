<#macro chainReport chain>
<#if chain.className??>
${chain.className} - Processors report - ${archiveUtils.get12DigitDate()}
  Number of Processors: ${chain.processorCount}

<#list chain.processors as p>
${p.report()}
</#list>

</#if>
</#macro>
<@chainReport chain=candidateChain!{} />
<@chainReport chain=fetchChain!{} />
<@chainReport chain=dispositionChain!{} />
