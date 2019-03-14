/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.rest.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.cloud.dataflow.rest.resource.RootResource;
import org.springframework.hateoas.Link;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gunnar Hillert
 * @author Glenn Renfro
 */
public class DataflowTemplateTests {

	@Before
	public void setup() {
		System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(100));
	}

	@After
	public void shutdown() {
		System.clearProperty("sun.net.client.defaultConnectTimeout");
	}

	@Test
	public void testDataFlowTemplateContructorWithNullUri() throws URISyntaxException {

		try {
			new DataFlowTemplate(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("The provided baseURI must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test(expected = ResourceAccessException.class)
	public void testDataFlowTemplateContructorWithNonExistingUri() throws URISyntaxException {
		new DataFlowTemplate(new URI("http://doesnotexist:1234"));
	}

	@Test
	public void testThatObjectMapperGetsPrepared() {
		final ObjectMapper objectMapper = new ObjectMapper();
		DataFlowTemplate.prepareObjectMapper(objectMapper);
		assertCorrectMixins(objectMapper);
	}

	@Test
	public void testPrepareObjectMapperWithNullObjectMapper() {
		try {
			DataFlowTemplate.prepareObjectMapper(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The objectMapper must not be null.", e.getMessage());
			return;
		}
	}

	@Test
	public void testThatDefaultDataflowRestTemplateContainsMixins() {
		final RestTemplate restTemplate = DataFlowTemplate.getDefaultDataflowRestTemplate();

		assertNotNull(restTemplate);
		assertTrue(restTemplate.getErrorHandler() instanceof VndErrorResponseErrorHandler);

		assertCorrectMixins(restTemplate);

	}

	private void assertCorrectMixins(RestTemplate restTemplate) {
		boolean containsMappingJackson2HttpMessageConverter = false;

		for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				containsMappingJackson2HttpMessageConverter = true;

				final MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				assertCorrectMixins(jacksonConverter.getObjectMapper());
			}
		}

		if (!containsMappingJackson2HttpMessageConverter) {
			fail("Expected that the restTemplate's list of Message Converters contained a "
					+ "MappingJackson2HttpMessageConverter");
		}
	}

	private void assertCorrectMixins(ObjectMapper objectMapper) {
		assertNotNull(objectMapper.findMixInClassFor(JobExecution.class));
		assertNotNull(objectMapper.findMixInClassFor(JobParameters.class));
		assertNotNull(objectMapper.findMixInClassFor(JobParameter.class));
		assertNotNull(objectMapper.findMixInClassFor(JobInstance.class));
		assertNotNull(objectMapper.findMixInClassFor(ExitStatus.class));
		assertNotNull(objectMapper.findMixInClassFor(StepExecution.class));
		assertNotNull(objectMapper.findMixInClassFor(ExecutionContext.class));
		assertNotNull(objectMapper.findMixInClassFor(StepExecutionHistory.class));
	}


	@Test
	public void testThatPrepareRestTemplateWithNullContructorValueContainsMixins() {
		final RestTemplate restTemplate = DataFlowTemplate.prepareRestTemplate(null);

		assertNotNull(restTemplate);
		assertTrue(restTemplate.getErrorHandler() instanceof VndErrorResponseErrorHandler);

		assertCorrectMixins(restTemplate);

	}

	@Test
	public void testThatPrepareRestTemplateWithProvidedRestTemplateContainsMixins() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		final RestTemplate restTemplate = DataFlowTemplate.prepareRestTemplate(providedRestTemplate);

		assertNotNull(restTemplate);
		assertTrue(providedRestTemplate == restTemplate);
		assertTrue(restTemplate.getErrorHandler() instanceof VndErrorResponseErrorHandler);

		assertCorrectMixins(restTemplate);
	}

	@Test
	public void testPrepareRestTemplateWithRestTemplateThatHasNoMessageConverters() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		providedRestTemplate.getMessageConverters().clear();

		try {
			DataFlowTemplate.prepareRestTemplate(providedRestTemplate);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'messageConverters' must not be empty", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testPrepareRestTemplateWithRestTemplateThatMissesJacksonConverter() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		final Iterator<HttpMessageConverter<?>> iterator = providedRestTemplate.getMessageConverters().iterator();

		while (iterator.hasNext()) {
			if (iterator.next() instanceof MappingJackson2HttpMessageConverter) {
				iterator.remove();
			}
		}

		try {
			DataFlowTemplate.prepareRestTemplate(providedRestTemplate);
		}
		catch (IllegalArgumentException e) {
			assertEquals("The RestTemplate does not contain a required MappingJackson2HttpMessageConverter.",
					e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testAllActive() throws Exception{
		DataFlowTemplate template = getMockedDataFlowTemplate(true);

		assertNotNull(template.taskOperations());
		assertNotNull(template.streamOperations());
		assertNotNull(template.runtimeOperations());
		assertNotNull(template.jobOperations());
		assertNotNull(template.schedulerOperations());

		testAlwaysActiveOperations(template);
	}

	@Test
	public void testAllDeActive() throws Exception{
		DataFlowTemplate template = getMockedDataFlowTemplate(false);

		assertNull(template.taskOperations());
		assertNull(template.streamOperations());
		assertNull(template.runtimeOperations());
		assertNull(template.jobOperations());
		assertNull(template.schedulerOperations());

		testAlwaysActiveOperations(template);
	}

	private void testAlwaysActiveOperations(DataFlowTemplate template) {
		//these operations are always active
		assertNotNull(template.aboutOperation());
		assertNotNull(template.appRegistryOperations());
		assertNotNull(template.completionOperations());
	}

	private DataFlowTemplate getMockedDataFlowTemplate(boolean isLinksActive) throws Exception{
		RestTemplate restTemplate = mock(RestTemplate.class);
		RootResource rootResource = mock(RootResource.class);
		Link link = mock(Link.class);
		when(link.getHref()).thenReturn("http://whereever");
		when(rootResource.getApiRevision()).thenReturn(Version.REVISION);
		when(rootResource.hasLink(any())).thenReturn(isLinksActive);
		when(rootResource.getLink(any())).thenReturn(link);
		when(restTemplate.getForObject(any(),any())).thenReturn(rootResource);
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		when(restTemplate.getMessageConverters()).thenReturn(converters);
		URI uri = new URI("foo");
		return new DataFlowTemplate(uri, restTemplate);
	}
}
