package com.microsoft.jenkins.kubernetes.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;

/**
 * Created by qixl on 2020/6/9.
 */
public final class K8sLabelUtils {

  private K8sLabelUtils() {
  }

  public static Map<String, String> buildDepPodLabel(V1Deployment deployment, String tenantCode) {
    Map<String, String> labels = new HashMap<>();
    labels
        .put(K8sObjectLabels.LABEL_POD_LOADTYPE, K8sObjectLabels.PodLoadType.Deployment.getCode());
    labels.put(K8sObjectLabels.LABEL_POD_APP, deployment.getMetadata().getName());
    labels.put(K8sObjectLabels.LABEL_POD_TENANT, tenantCode);
    labels.put(K8sObjectLabels.LABEL_POD_DEPLOYMENT, deployment.getMetadata().getName());

    return labels;
  }

  public static Map<String, String> buildDepLabel(String appCode) {
    Map<String, String> labels = new HashMap<>();
    labels.put(K8sObjectLabels.LABEL_APP, appCode);
    labels.put(K8sObjectLabels.LABEL_LOADTYPE, K8sObjectLabels.PodLoadType.Deployment.getCode());
    return labels;
  }

  public static Map<String, String> buildStaPodLabel(V1StatefulSet body, String tenantCode) {
    Map<String, String> labels = new HashMap<>();
    labels
        .put(K8sObjectLabels.LABEL_POD_LOADTYPE, K8sObjectLabels.PodLoadType.StatefulSet.getCode());
    labels.put(K8sObjectLabels.LABEL_POD_APP, body.getMetadata().getName());
    labels.put(K8sObjectLabels.LABEL_POD_TENANT, tenantCode);
    labels.put(K8sObjectLabels.LABEL_POD_STATEFULSET, body.getMetadata().getName());

    return labels;
  }

  public static Map<String, String> buildStaLabel(String appCode) {
    Map<String, String> labels = new HashMap<>();
    labels.put(K8sObjectLabels.LABEL_APP, appCode);
    labels.put(K8sObjectLabels.LABEL_LOADTYPE, K8sObjectLabels.PodLoadType.StatefulSet.getCode());
    return labels;
  }

  public static Map<String, String> buildServiceLabel(V1Service body, String tenantCode,
                                                      String projectName) {
    Map<String, String> labels = new HashMap<>();
    String podApp = body.getSpec().getSelector().get(K8sObjectLabels.LABEL_POD_APP);
    labels.put(K8sObjectLabels.LABEL_POD_APP, podApp);
    labels.put(K8sObjectLabels.LABEL_SERVICE_APP, podApp); //标记服务所属组件
    K8sObjectLabels.PodLoadType loadType = K8sObjectLabels.PodLoadType
        .valueOf(body.getSpec().getSelector().get(K8sObjectLabels.LABEL_POD_LOADTYPE));
    labels.put(K8sObjectLabels.LABEL_POD_LOADTYPE, loadType.getCode());
    labels.put(K8sObjectLabels.LABEL_SERVICE_TENANT, tenantCode);
    labels.put(K8sObjectLabels.LABEL_SERVICE_PROJECT, projectName);
    return labels;
  }

  public static Map<String, String> buildIngresslabel(String tenantCode, String projectName) {
    Map<String, String> labels = new HashMap<>();
    labels.put(K8sObjectLabels.LABEL_INGRESS_TENANT, tenantCode);
    labels.put(K8sObjectLabels.LABEL_INGRESS_PROJECT, projectName);
    return labels;
  }

  public static String buildAutoScalerLabelSelector(String app, String loadType) {
    String labelSelector = null;
    if (!StringUtils.isEmpty(app)) {
      labelSelector = K8sObjectLabels.LABEL_AUTOSCALER_APP + "=" + app;
    }
    if (!StringUtils.isEmpty(loadType)) {
      if (!StringUtils.isEmpty(labelSelector)) {
        labelSelector =
            labelSelector + ",";
      }
      labelSelector =
          labelSelector + K8sObjectLabels.LABEL_AUTOSCALER_LOADTYPE + "=" + loadType;
    }
    return labelSelector;
  }

}
