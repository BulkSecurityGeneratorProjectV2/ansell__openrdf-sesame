/*******************************************************************************
 * Copyright (c) 2015, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.openrdf.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.io.IOUtil;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.util.Models;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryReadOnlyException;
import org.openrdf.repository.config.ConfigTemplate;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.config.RepositoryConfigUtil;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

/**
 * @author Dale Visser
 */
public class Create implements Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(Create.class);

	private static final String TEMPLATES_DIR = "templates";

	private final ConsoleIO consoleIO;

	private final ConsoleState state;

	private final LockRemover lockRemover;

	Create(ConsoleIO consoleIO, ConsoleState state, LockRemover lockRemover) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.lockRemover = lockRemover;
	}

	public void execute(String... tokens)
		throws IOException
	{
		if (tokens.length < 2) {
			consoleIO.writeln(PrintHelp.CREATE);
		}
		else {
			createRepository(tokens[1]);
		}
	}

	private void createRepository(final String templateName)
		throws IOException
	{
		try {
			// FIXME: remove assumption of .ttl extension
			final String templateFileName = templateName + ".ttl";
			final File templatesDir = new File(state.getDataDirectory(), TEMPLATES_DIR);
			final File templateFile = new File(templatesDir, templateFileName);
			InputStream templateStream = createTemplateStream(templateName, templateFileName, templatesDir,
					templateFile);
			if (templateStream != null) {
				String template;
				try {
					template = IOUtil.readString(new InputStreamReader(templateStream, "UTF-8"));
				}
				finally {
					templateStream.close();
				}
				final ConfigTemplate configTemplate = new ConfigTemplate(template);
				final Map<String, String> valueMap = new HashMap<String, String>();
				final Map<String, List<String>> variableMap = configTemplate.getVariableMap();
				boolean eof = inputParameters(valueMap, variableMap, configTemplate.getMultilineMap());
				if (!eof) {
					final String configString = configTemplate.render(valueMap);
					final Repository systemRepo = this.state.getManager().getSystemRepository();
					final Model graph = new LinkedHashModel();
					final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, systemRepo.getValueFactory());
					rdfParser.setRDFHandler(new StatementCollector(graph));
					rdfParser.parse(new StringReader(configString), RepositoryConfigSchema.NAMESPACE);
					final Resource repositoryNode = Models.subject(
							graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElseThrow(
									() -> new RepositoryConfigException("missing repository node"));
					final RepositoryConfig repConfig = RepositoryConfig.create(graph, repositoryNode);
					repConfig.validate();
					boolean proceed = RepositoryConfigUtil.hasRepositoryConfig(systemRepo, repConfig.getID())
							? consoleIO.askProceed(
									"WARNING: you are about to overwrite the configuration of an existing repository!",
									false)
							: true;
					if (proceed) {
						try {
							RepositoryConfigUtil.updateRepositoryConfigs(systemRepo, repConfig);
							consoleIO.writeln("Repository created");
						}
						catch (RepositoryReadOnlyException e) {
							if (lockRemover.tryToRemoveLock(systemRepo)) {
								RepositoryConfigUtil.updateRepositoryConfigs(systemRepo, repConfig);
								consoleIO.writeln("Repository created");
							}
							else {
								consoleIO.writeError("Failed to create repository");
								LOGGER.error("Failed to create repository", e);
							}
						}
					}
					else {
						consoleIO.writeln("Create aborted");
					}
				}
			}
		}
		catch (Exception e) {
			consoleIO.writeError(e.getClass().getName() + ": " + e.getMessage());
			LOGGER.error("Failed to create repository", e);
		}
	}

	private boolean inputParameters(final Map<String, String> valueMap,
			final Map<String, List<String>> variableMap, Map<String, String> multilineInput)
				throws IOException
	{
		if (!variableMap.isEmpty()) {
			consoleIO.writeln("Please specify values for the following variables:");
		}
		boolean eof = false;
		for (Map.Entry<String, List<String>> entry : variableMap.entrySet()) {
			final String var = entry.getKey();
			final List<String> values = entry.getValue();
			consoleIO.write(var);
			if (values.size() > 1) {
				consoleIO.write(" (");
				for (int i = 0; i < values.size(); i++) {
					if (i > 0) {
						consoleIO.write("|");
					}
					consoleIO.write(values.get(i));
				}
				consoleIO.write(")");
			}
			if (!values.isEmpty()) {
				consoleIO.write(" [" + values.get(0) + "]");
			}
			consoleIO.write(": ");
			String value = multilineInput.containsKey(var) ? consoleIO.readMultiLineInput() : consoleIO.readln();
			eof = (value == null);
			if (eof) {
				break; // for loop
			}
			value = value.trim();
			if (value.length() == 0) {
				value = null; // NOPMD
			}
			valueMap.put(var, value);
		}
		return eof;
	}

	private InputStream createTemplateStream(final String templateName, final String templateFileName,
			final File templatesDir, final File templateFile)
				throws FileNotFoundException
	{
		InputStream templateStream = null;
		if (templateFile.exists()) {
			if (templateFile.canRead()) {
				templateStream = new FileInputStream(templateFile);
			}
			else {
				consoleIO.writeError("Not allowed to read template file: " + templateFile);
			}
		}
		else {
			// Try class path for built-ins
			templateStream = RepositoryConfig.class.getResourceAsStream(templateFileName);
			if (templateStream == null) {
				consoleIO.writeError("No template called " + templateName + " found in " + templatesDir);
			}
		}
		return templateStream;
	}
}
