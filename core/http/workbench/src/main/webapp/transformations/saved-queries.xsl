<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="url-encode.xsl" />

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title" select="$saved-queries.title" />

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql/sparql:results">
		<script src="../../scripts/saved-queries.js" type="text/javascript"></script>
		<xsl:for-each select="sparql:result">
			<xsl:variable name="queryLn"
				select="normalize-space(sparql:binding[@name='queryLn'])" />
			<xsl:variable name="rowsPerPage" select="normalize-space(sparql:binding[@name='rowsPerPage'])" />
			<table class="data">
				<tr>
					<th>User</th>
					<td>
						<xsl:value-of select="sparql:binding[@name='user']" />
					</td>
					<td rowspan="6">
						<pre>
							<xsl:value-of select="sparql:binding[@name='queryText']" />
						</pre>
					</td>
				</tr>
				<tr>
					<th>Query Name</th>
					<td>
						<a>
							<xsl:attribute name="href">query?action=exec&amp;queryLn=<xsl:value-of
								select="$queryLn" />&amp;query=<xsl:call-template
								name="url-encode">
									<xsl:with-param name="str"
								select="normalize-space(sparql:binding[@name='queryText'])" />
								</xsl:call-template>&amp;limit=<xsl:value-of
								select="$rowsPerPage" /></xsl:attribute>
							<xsl:value-of select="sparql:binding[@name='queryName']" />
						</a>
					</td>
				</tr>
				<tr>
					<th>Query Language</th>
					<td>
						<xsl:value-of select="$queryLn" />
					</td>
				</tr>
				<tr>
					<th>Rows Per Page</th>
					<td>
						<xsl:value-of select="$rowsPerPage" />
					</td>
				</tr>
				<tr>
					<th>Shared</th>
					<td>
						<xsl:value-of select="sparql:binding[@name='shared']" />
					</td>
				</tr>
				<tr>
					<td colspan="2">
						ExecuteButton EditButton
					</td>
				</tr>
			</table>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
