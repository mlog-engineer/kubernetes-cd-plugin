/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import com.microsoft.jenkins.azurecommons.core.EnvironmentInjector;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.kubernetes.BaseResponse;
import com.microsoft.jenkins.kubernetes.KubernetesCDPlugin;
import com.microsoft.jenkins.kubernetes.Messages;
import com.microsoft.jenkins.kubernetes.credentials.ClientWrapperFactory;
import com.microsoft.jenkins.kubernetes.credentials.ResolvedDockerRegistryEndpoint;
import com.microsoft.jenkins.kubernetes.util.Constants;
import com.microsoft.jenkins.kubernetes.wrapper.KubernetesClientWrapper;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.remoting.ProxyException;
import hudson.security.ACL;
import hudson.util.VariableResolver;
import io.kubernetes.client.openapi.ApiClient;
import jenkins.security.MasterToSlaveCallable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * Command to deploy Kubernetes configurations.
 * <p>
 * Mark it as serializable so that the inner Callable can be serialized correctly.
 */
public class DeploymentCommand
    implements ICommand<DeploymentCommand.IDeploymentCommand>, Serializable {

  private static final int SC_OK = 200;
  private PrintStream logger = null;


  @Override
  public void execute(IDeploymentCommand context) {
    JobContext jobContext = context.getJobContext();
    logger = jobContext.getTaskListener().getLogger();

    FilePath workspace = jobContext.getWorkspace();
    EnvVars envVars = context.getEnvVars();
    Item owner = context.getJobContext().getRun().getParent();
    String appManagerCredentialId = context.getAppManagerCredentialId();
    String appManagerUrl = context.getAppManagerUrl();

    TaskResult taskResult = null;
    try {
      DeploymentTask task = new DeploymentTask();
      task.setWorkspace(workspace);
      task.setTaskListener(jobContext.getTaskListener());
      task.setClientFactory(context.clientFactory(context.getJobContext().getRun().getParent()));
      task.setEnvVars(envVars);
      task.setConfigPaths(context.getConfigs());
      task.setSecretNamespace(context.getSecretNamespace());
      task.setSecretNameCfg(context.getSecretName());
      task.setDefaultSecretNameSeed(jobContext.getRun().getDisplayName());
      task.setEnableSubstitution(context.isEnableConfigSubstitution());
      task.setDockerRegistryEndpoints(context.resolveEndpoints(jobContext.getRun().getParent()));
      task.setDeleteResource(context.isDeleteResource());

      //mlog add start
      if (StringUtils.isNotBlank(appManagerCredentialId) && StringUtils.isNotBlank(appManagerUrl)) {
        task.setAllowNamespaces(getAllowNamespaces(appManagerUrl, appManagerCredentialId, owner));
      }

      task.setAppManagerUrl(context.getAppManagerUrl());
      task.setProjectName(context.getProjectName());
      task.setTenantCode(context.getTenantCode());
      task.setAppCode(context.getAppCode());
      //mlog add end

      taskResult = workspace.act(task);

      for (Map.Entry<String, String> entry : taskResult.extraEnvVars.entrySet()) {
        EnvironmentInjector.inject(jobContext.getRun(), envVars, entry.getKey(), entry.getValue());
      }

      context.setCommandState(taskResult.commandState);
      if (taskResult.commandState.isError()) {
        KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "DeployFailed",
            Constants.AI_K8S_MASTER, AppInsightsUtils.hash(taskResult.masterHost));
      } else {
        KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "Deployed",
            Constants.AI_K8S_MASTER, AppInsightsUtils.hash(taskResult.masterHost));
      }
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      context.logError(e);
      KubernetesCDPlugin.sendEvent(Constants.AI_KUBERNETES, "DeployFailed",
          Constants.AI_K8S_MASTER,
          AppInsightsUtils.hash(taskResult == null ? null : taskResult.masterHost));
    }
  }

  /**
   * 查询账户在应用托管平台对权限 .
   *
   * @param appManagerUrl          托管平台地址(http://xxx.com)
   * @param appManagerCredentialId
   * @param owner                  owner
   * @return Set
   * @throws IOException
   */
  private Set<String> getAllowNamespaces(String appManagerUrl, String appManagerCredentialId,
                                         Item owner)
      throws IOException {
    StandardUsernamePasswordCredentials creds = CredentialsMatchers.firstOrNull(
        CredentialsProvider.lookupCredentials(
            StandardUsernamePasswordCredentials.class,
            owner,
            ACL.SYSTEM,
            Collections.<DomainRequirement>emptyList()),
        CredentialsMatchers.withId(appManagerCredentialId));
    Set<String> allowNamespaceSet = null;
    if (appManagerUrl.endsWith("/")) {
      appManagerUrl = appManagerUrl.substring(0, appManagerUrl.length() - 1);
    }
    if (StringUtils.isNotBlank(appManagerUrl)) {
      //校验托管平台密钥是否正确
      String url = String
          .format("%s/login?username=%s&password=%s", appManagerUrl, creds.getUsername(),
              creds.getPassword());
      log("login url:" + url);

      OkHttpClient client = new OkHttpClient().newBuilder()
          .build();
      MediaType mediaType = MediaType.parse("application/json");
      RequestBody body = RequestBody.create(mediaType, "");
      Request request = new Request.Builder()
          .url(url)
          .method("POST", body)
          .build();

      Response response = client.newCall(request).execute();
      String resultStr = response.body().string();
      log("login response:" + resultStr);
      Gson gson = new Gson();
      BaseResponse baseResponse = gson.fromJson(resultStr, BaseResponse.class);
      if (baseResponse.getCode() == SC_OK) {
        List<String> namespaceList = (List<String>) baseResponse.getData().get("namespaces");
        allowNamespaceSet = new HashSet<>(namespaceList);
      } else {
        throw new IllegalArgumentException("login app-manager system error");
      }
    }
    return allowNamespaceSet;
  }

  @VisibleForTesting
  static String getMasterHost(KubernetesClientWrapper wrapper) {
    if (wrapper != null) {
      ApiClient client = wrapper.getClient();
      if (client != null) {
        String url = client.getBasePath();
        if (url != null) {
          return url;
        }
      }
    }
    return "Unknown";
  }

  private void log(String message) {
    if (logger != null) {
      logger.println(message);
    }
  }

  static class DeploymentTask extends MasterToSlaveCallable<TaskResult, ProxyException> {
    private FilePath workspace;
    private TaskListener taskListener;
    private ClientWrapperFactory clientFactory;
    private EnvVars envVars;

    private String configPaths;
    private String secretNamespace;
    private String secretNameCfg;
    private String defaultSecretNameSeed;
    private boolean enableSubstitution;
    private boolean deleteResource;

    //应用托管平台地址
    private String appManagerUrl;
    //应用托管平台密钥
    private String appManagerCredentialId;

    private Set<String> allowNamespaces;
    //mlog add start
    //项目名称
    private String projectName;
    //租户空间编码
    private String tenantCode;
    //应用编码
    private String appCode;
    //mlog add end

    private List<ResolvedDockerRegistryEndpoint> dockerRegistryEndpoints;

    @Override
    public TaskResult call() throws ProxyException {
      try {
        return doCall();
      } catch (Exception ex) {
        // JENKINS-50760
        // JEP-200 restricts the classes allowed to be serialized with XStream to a whitelist.
        // The task being executed in doCall may throw some exceptions from the third party libraries,
        // which will cause SecurityException when it's transferred from the slave back to the master.
        // We catch the exception and wrap the stack trace in a ProxyException which can
        // be serialized properly.
        throw new ProxyException(ex);
      }
    }

    private TaskResult doCall() throws Exception {
      TaskResult result = new TaskResult();

      checkState(StringUtils.isNotBlank(secretNamespace),
          Messages.DeploymentCommand_blankNamespace());
      checkState(StringUtils.isNotBlank(configPaths),
          Messages.DeploymentCommand_blankConfigFiles());

      KubernetesClientWrapper wrapper =
          clientFactory.buildClient(workspace).withLogger(taskListener.getLogger()).
              withDeleteResource(deleteResource);
      result.masterHost = getMasterHost(wrapper);

      FilePath[] configFiles = workspace.list(configPaths);
      if (configFiles.length == 0) {
        String message = Messages.DeploymentCommand_noMatchingConfigFiles(configPaths);
        taskListener.error(message);
        result.commandState = CommandState.HasError;
        throw new IllegalStateException(message);
      }

      if (!dockerRegistryEndpoints.isEmpty()) {
        String secretName =
            KubernetesClientWrapper
                .prepareSecretName(secretNameCfg, defaultSecretNameSeed, envVars);

        wrapper.createOrReplaceSecrets(secretNamespace, secretName, dockerRegistryEndpoints);

        taskListener.getLogger().println(Messages.DeploymentCommand_injectSecretName(
            Constants.KUBERNETES_SECRET_NAME_PROP, secretName));
        envVars.put(Constants.KUBERNETES_SECRET_NAME_PROP, secretName);
        result.extraEnvVars.put(Constants.KUBERNETES_SECRET_NAME_PROP, secretName);
      }


      if (enableSubstitution) {
        wrapper.withVariableResolver(new VariableResolver.ByMap<>(envVars));
      }

      wrapper.apply(configFiles, allowNamespaces, tenantCode, projectName, appCode);

      result.commandState = CommandState.Success;

      return result;
    }

    public void setWorkspace(FilePath workspace) {
      this.workspace = workspace;
    }

    public void setTaskListener(TaskListener taskListener) {
      this.taskListener = taskListener;
    }

    public void setClientFactory(ClientWrapperFactory clientFactory) {
      this.clientFactory = clientFactory;
    }

    public void setEnvVars(EnvVars envVars) {
      this.envVars = envVars;
    }

    public void setConfigPaths(String configPaths) {
      this.configPaths = configPaths;
    }

    public void setSecretNamespace(String secretNamespace) {
      this.secretNamespace = secretNamespace;
    }

    public void setSecretNameCfg(String secretNameCfg) {
      this.secretNameCfg = secretNameCfg;
    }

    public void setDefaultSecretNameSeed(String defaultSecretNameSeed) {
      this.defaultSecretNameSeed = defaultSecretNameSeed;
    }

    public void setEnableSubstitution(boolean enableSubstitution) {
      this.enableSubstitution = enableSubstitution;
    }

    public void setDockerRegistryEndpoints(
        List<ResolvedDockerRegistryEndpoint> dockerRegistryEndpoints) {
      this.dockerRegistryEndpoints = dockerRegistryEndpoints;
    }

    public void setDeleteResource(boolean isDeleteResource) {
      this.deleteResource = isDeleteResource;
    }

    public void setProjectName(String projectName) {
      this.projectName = projectName;
    }

    public void setTenantCode(String tenantCode) {
      this.tenantCode = tenantCode;
    }

    public void setAppCode(String appCode) {
      this.appCode = appCode;
    }

    public String getAppManagerUrl() {
      return appManagerUrl;
    }

    public void setAppManagerUrl(String appManagerUrl) {
      this.appManagerUrl = appManagerUrl;
    }

    public String getAppManagerCredentialId() {
      return appManagerCredentialId;
    }

    public void setAppManagerCredentialId(String appManagerCredentialId) {
      this.appManagerCredentialId = appManagerCredentialId;
    }

    public Set<String> getAllowNamespaces() {
      return allowNamespaces;
    }

    public void setAllowNamespaces(Set<String> allowNamespaces) {
      this.allowNamespaces = allowNamespaces;
    }
  }

  public static class TaskResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private CommandState commandState = CommandState.Unknown;
    private String masterHost;
    private final Map<String, String> extraEnvVars = new HashMap<>();
  }

  public interface IDeploymentCommand extends IBaseCommandData {
    ClientWrapperFactory clientFactory(Item owner);

    String getSecretNamespace();

    String getSecretName();

    List<ResolvedDockerRegistryEndpoint> resolveEndpoints(Item context) throws IOException;

    String getConfigs();

    //mlog add start
    String getAppManagerUrl();

    String getAppManagerCredentialId();

    String getProjectName();

    String getTenantCode();

    String getAppCode();
    //mlog add end

    boolean isEnableConfigSubstitution();

    boolean isDeleteResource();
  }
}
