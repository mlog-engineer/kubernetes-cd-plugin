package com.microsoft.jenkins.kubernetes.wrapper;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AutoscalingV2beta2Api;
import io.kubernetes.client.models.V2beta2HorizontalPodAutoscaler;

import static com.google.common.base.Preconditions.checkNotNull;

public class V2beta2ResourceManager extends ResourceManager {
    private final AutoscalingV2beta2Api autoscalingV2beta2Api;

    private V2beta2ResourceUpdateMonitor resourceUpdateMonitor = V2beta2ResourceUpdateMonitor.NOOP;

    public V2beta2ResourceManager(ApiClient client) {
        super(true);
        checkNotNull(client);

        autoscalingV2beta2Api = new AutoscalingV2beta2Api(client);

    }

    public V2beta2ResourceManager(ApiClient client, boolean pretty) {
        super(pretty);
        checkNotNull(client);

        autoscalingV2beta2Api = new AutoscalingV2beta2Api(client);
    }

    public V2beta2ResourceUpdateMonitor getResourceUpdateMonitor() {
        return resourceUpdateMonitor;
    }

    public V2beta2ResourceManager withResourceUpdateMonitor(V2beta2ResourceUpdateMonitor monitor) {
        checkNotNull(monitor);
        this.resourceUpdateMonitor = monitor;
        return this;
    }

    class HorizontalPodAutoscalerUpdater extends ResourceUpdater<V2beta2HorizontalPodAutoscaler> {
        HorizontalPodAutoscalerUpdater(V2beta2HorizontalPodAutoscaler namespace) {
            super(namespace);
        }

        @Override
        V2beta2HorizontalPodAutoscaler getCurrentResource() {
            V2beta2HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV2beta2Api.readNamespacedHorizontalPodAutoscaler(
                        getName(), getNamespace(), getPretty(), true, true);
            } catch (ApiException e) {
                handleApiExceptionExceptNotFound(e);
            }
            return result;
        }

        @Override
        V2beta2HorizontalPodAutoscaler applyResource(
                V2beta2HorizontalPodAutoscaler original, V2beta2HorizontalPodAutoscaler current) {
            V2beta2HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV2beta2Api.replaceNamespacedHorizontalPodAutoscaler(
                        getName(), getNamespace(), current, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        V2beta2HorizontalPodAutoscaler createResource(V2beta2HorizontalPodAutoscaler current) {
            V2beta2HorizontalPodAutoscaler result = null;
            try {
                result = autoscalingV2beta2Api.createNamespacedHorizontalPodAutoscaler(
                        getNamespace(), current, null, getPretty(), null);
            } catch (ApiException e) {
                handleApiException(e);
            }
            return result;
        }

        @Override
        void notifyUpdate(V2beta2HorizontalPodAutoscaler original, V2beta2HorizontalPodAutoscaler current) {
            resourceUpdateMonitor.onHorizontalPodAutoscalerUpdate(original, current);
        }
    }
}
