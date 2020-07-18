package com.microsoft.jenkins.kubernetes.util;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public interface K8sObjectLabels {

  //----------------
  String LABEL_STATEFUL_PROJECT = "appmgr-statefulset-project";

  String LABEL_STATEFUL_TENANT = "appmgr-statefulset-tenant";

  //----------------deployment labels
  String LABEL_DEPLOYMENT_PROJECT = "appmgr-deployment-project";
  String LABEL_DEPLOYMENT_TENANT = "appmgr-deployment-tenant";

  //----------------cronjob labels
  String LABEL_CRONJOB_PROJECT = "appmgr-cronjob-project";

  String LABEL_CRONJOB_TENANT = "appmgr-cronjob-tenant";

  //----------------job labels

  String LABEL_JOB_PROJECT = "appmgr-job-project";

  String LABEL_JOB_TENANT = "appmgr-job-tenant";


  /**
   * 标记资源属于 哪个应用 .
   */
  String LABEL_APP = "appmgr-app";

  /**
   * 标记资源属于哪个release(通过helm模版部署时才有此值) .
   */
  String LABEL_RELEASE_NAME = "appmgr-release-name";

  /**
   * 标记资源使用的哪个chart .
   */
  String LABEL_CHART = "appmgr-chart";

  /**
   * 标记chart所属的仓库 .
   */
  String LABEL_REGISTRY = "appmgr-registry";

  /**
   * 标记资源是哪种负载类型 .
   */
  String LABEL_LOADTYPE = "appmgr-loadType";

  //----------------pod labels
  /**
   * 标记pod属于哪个应用 .
   */
  String LABEL_POD_APP = "appmgr-pod-app";

  /**
   * 标记pod属于哪个job.
   */
  String LABEL_POD_JOB = "appmgr-pod-job";

  /**
   * 标记pod属于哪个deployment.
   */
  String LABEL_POD_DEPLOYMENT = "appmgr-pod-deployment";

  /**
   * 标记pod属于哪个statefulset.
   */
  String LABEL_POD_STATEFULSET = "appmgr-pod-statefulset";

  /**
   * 标记pod属于哪种负载类型,包括有状态、无状态、任务、定时任务.
   */
  String LABEL_POD_LOADTYPE = "appmgr-pod-loadType";

  /**
   * 标记pod属于哪个项目.
   */
  String LABEL_POD_POJECCT = "appmgr-pod-poject";

  /**
   * 标记pod属于哪个租户.
   */
  String LABEL_POD_TENANT = "appmgr-pod-tenant";


  //----------------------service labels

  /**
   * 标记service属于哪个pod.
   */
  String LABEL_SERVICE_POD = "appmgr-service-pod";

  /**
   * 标记service属于哪个应用.
   */
  String LABEL_SERVICE_APP = "appmgr-service-app";

  /**
   * 标记service属于哪个项目.
   */
  String LABEL_SERVICE_PROJECT = "appmgr-service-project";


  /**
   * 标记service属于哪个租户.
   */
  String LABEL_SERVICE_TENANT = "appmgr-service-tenant";

//----------------------ingress labels

  /**
   * 标记service属于哪个项目.
   */
  String LABEL_INGRESS_PROJECT = "appmgr-ingress-project";


  /**
   * 标记service属于哪个租户.
   */
  String LABEL_INGRESS_TENANT = "appmgr-ingress-tenant";

  //----------------------autoscaler labels
  /**
   * 标记autoscaler属于哪个应用.
   */
  String LABEL_AUTOSCALER_APP = "appmgr-autoscaler-app";
  /**
   * 标记autoscaler所属应用的负载类型，如Deployment,StatefulSet.
   */
  String LABEL_AUTOSCALER_LOADTYPE = "appmgr-autoscaler-loadType";
  /**
   * 标记autoscaler属于哪个项目.
   */
  String LABEL_AUTOSCALER_PROJECT = "appmgr-autoscaler-project";
  /**
   * 标记autoscaler属于哪个租户.
   */
  String LABEL_AUTOSCALER_TENANT = "appmgr-autoscaler-tenant";

  //----------------------secret labels
  /**
   * 标记secret属于哪个应用.
   */
  String LABEL_SECRET_TYPE = "appmgr-secret-type";

  /**
   * 标记secret属于哪个项目.
   */
  String LABEL_SECRET_PROJECT = "appmgr-secret-project";
  /**
   * 标记secret属于哪个租户.
   */
  String LABEL_SECRET_TENANT = "appmgr-secret-tenant";


  /**
   * 命名空间属于哪个租户 .
   */
  String LABEL_NAMESPACE_TENANT = "appmgr-ns-tenant";

  /**
   * 负载类型:有状态、无状态、任务、定时任务.
   */
  enum PodLoadType {

    StatefulSet("StatefulSet", "有状态"),
    Deployment("Deployment", "无状态"),
    Job("Job", "任务"),
    CronJob("CronJob", "定时任务");
    private final String code;
    private final String desc;

    PodLoadType(String code, String desc) {
      this.code = code;
      this.desc = desc;
    }

    public String getCode() {
      return code;
    }

    public String getDesc() {
      return desc;
    }

    public static Map<String, String> getTypeOptions() {
      return Arrays.stream(PodLoadType.values())
          .collect(Collectors.toMap(PodLoadType::getCode, PodLoadType::getDesc));
    }
  }

}
