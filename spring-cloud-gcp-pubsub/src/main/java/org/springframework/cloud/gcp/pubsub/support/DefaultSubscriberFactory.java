/*
 *  Copyright 2017-2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.pubsub.support;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.SubscriptionName;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.Assert;

/**
 *
 * The default {@link SubscriberFactory} implementation.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class DefaultSubscriberFactory implements SubscriberFactory {

	private String projectId;

	private final ExecutorProvider executorProvider;

	private final TransportChannelProvider channelProvider;

	private final CredentialsProvider credentialsProvider;

	public DefaultSubscriberFactory(GcpProjectIdProvider projectIdProvider,
			ExecutorProvider executorProvider,
			TransportChannelProvider channelProvider,
			CredentialsProvider credentialsProvider) {
		Assert.notNull(projectIdProvider, "The project ID provider can't be null.");
		Assert.notNull(executorProvider, "The executor provider can't be null.");
		Assert.notNull(channelProvider, "The channel provider can't be null.");
		Assert.notNull(credentialsProvider, "The credentials provider can't be null.");

		this.projectId = projectIdProvider.getProjectId();
		Assert.hasText(this.projectId, "The project ID can't be null or empty.");
		this.executorProvider = executorProvider;
		this.channelProvider = channelProvider;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public Subscriber createSubscriber(String subscriptionName, MessageReceiver receiver) {
		return Subscriber.newBuilder(
				SubscriptionName.of(this.projectId, subscriptionName), receiver)
				.setChannelProvider(this.channelProvider)
				.setExecutorProvider(this.executorProvider)
				.setCredentialsProvider(this.credentialsProvider)
				.build();
	}

	@Override
	public PullRequest createPullRequest(String subscriptionName, Integer maxMessages,
			Boolean returnImmediately) {
		Assert.hasLength(subscriptionName, "The subscription name must be provided.");

		PullRequest.Builder pullRequestBuilder =
				PullRequest.newBuilder().setSubscriptionWithSubscriptionName(
						SubscriptionName.of(this.projectId, subscriptionName));

		if (maxMessages != null) {
			pullRequestBuilder.setMaxMessages(maxMessages);
		}

		if (returnImmediately != null) {
			pullRequestBuilder.setReturnImmediately(returnImmediately);
		}

		return pullRequestBuilder.build();
	}

	@Override
	public SubscriberStub createSubscriberStub(RetrySettings retrySettings) {
		try {
			SubscriptionAdminSettings.Builder subscriptionAdminSettingsBuilder = SubscriptionAdminSettings.newBuilder()
					.setExecutorProvider(this.executorProvider)
					.setCredentialsProvider(this.credentialsProvider)
					.setTransportChannelProvider(this.channelProvider);

			if (retrySettings != null) {
				subscriptionAdminSettingsBuilder.pullSettings().setRetrySettings(retrySettings);
			}

			return GrpcSubscriberStub.create(subscriptionAdminSettingsBuilder.build());
		}
		catch (IOException e) {
			throw new RuntimeException("Error creating the SubscriberStub", e);
		}
	}
}
